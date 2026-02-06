package com.NextLVLHasH.Websockets.rpg.combat;

import com.NextLVLHasH.Websockets.rpg.attributes.AttributeManager;
import com.NextLVLHasH.Websockets.rpg.attributes.DerivedStat;
import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Static utility class for calculating damage in the combat system.
 * Provides methods for applying various damage modifiers including
 * base damage scaling, critical hits, resistances, and armor mitigation.
 * 
 * <p>The damage calculation pipeline follows this order:
 * <ol>
 *     <li>Calculate base damage from raw damage and attacker stats</li>
 *     <li>Apply critical hit multiplier (if crit succeeds)</li>
 *     <li>Apply resistance based on damage type</li>
 *     <li>Apply armor mitigation (for physical damage)</li>
 * </ol>
 * 
 * <p>Formulas:
 * <ul>
 *     <li>Base Damage: rawDamage * (1 + strength * 0.02) * weaponModifier</li>
 *     <li>Critical: damage * critMultiplier (if random &lt; critChance)</li>
 *     <li>Resistance: damage * (1 - resistance/100), capped at 75%</li>
 *     <li>Armor Mitigation: damage * (100 / (100 + armor))</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public final class DamageCalculator {

    private static final Logger LOGGER = Logger.getLogger(DamageCalculator.class.getName());
    
    /** Maximum resistance percentage allowed (75%) */
    public static final double MAX_RESISTANCE_PERCENT = 0.75;
    
    /** Strength scaling factor per point */
    public static final double STRENGTH_SCALING = 0.02;
    
    /** Intelligence scaling factor per point for magical damage */
    public static final double INTELLIGENCE_SCALING = 0.025;
    
    /** Armor constant used in mitigation formula */
    public static final double ARMOR_CONSTANT = 100.0;
    
    /** Default critical hit multiplier */
    public static final double DEFAULT_CRIT_MULTIPLIER = 2.0;
    
    /** Random number generator for critical hit rolls */
    private static final Random RANDOM = new Random();
    
    /** Thread-local storage for critical hit result from last calculation */
    private static final ThreadLocal<CritResult> LAST_CRIT_RESULT = ThreadLocal.withInitial(() -> new CritResult(false, 1.0));

    /**
     * Private constructor to prevent instantiation.
     */
    private DamageCalculator() {
        throw new UnsupportedOperationException("DamageCalculator is a static utility class");
    }

    /**
     * Calculates base damage from raw damage and attacker stats.
     * Formula: rawDamage * (1 + strength * 0.02) * weaponModifier
     *
     * @param rawDamage the base raw damage value
     * @param attackerStrength the attacker's strength stat
     * @param weaponModifier the weapon damage modifier (1.0 = no bonus)
     * @return the calculated base damage
     */
    public static double calculateBaseDamage(double rawDamage, double attackerStrength, double weaponModifier) {
        if (rawDamage <= 0) return 0;
        
        double strengthBonus = 1.0 + (attackerStrength * STRENGTH_SCALING);
        double baseDamage = rawDamage * strengthBonus * Math.max(0, weaponModifier);
        
        LOGGER.fine(() -> String.format("Base damage: %.2f (raw=%.2f, str=%.2f, wpn=%.2f)", 
            baseDamage, rawDamage, attackerStrength, weaponModifier));
        
        return baseDamage;
    }

    /**
     * Calculates base magical damage from raw damage and attacker stats.
     * Formula: rawDamage * (1 + intelligence * 0.025) * spellModifier
     *
     * @param rawDamage the base raw damage value
     * @param attackerIntelligence the attacker's intelligence stat
     * @param spellModifier the spell damage modifier (1.0 = no bonus)
     * @return the calculated base magical damage
     */
    public static double calculateBaseMagicalDamage(double rawDamage, double attackerIntelligence, double spellModifier) {
        if (rawDamage <= 0) return 0;
        
        double intBonus = 1.0 + (attackerIntelligence * INTELLIGENCE_SCALING);
        double baseDamage = rawDamage * intBonus * Math.max(0, spellModifier);
        
        LOGGER.fine(() -> String.format("Base magical damage: %.2f (raw=%.2f, int=%.2f, spell=%.2f)", 
            baseDamage, rawDamage, attackerIntelligence, spellModifier));
        
        return baseDamage;
    }

    /**
     * Applies critical hit calculation to damage.
     * The critical result can be retrieved via {@link #wasLastHitCritical()}.
     *
     * @param damage the input damage
     * @param critChance the chance to crit (0.0 to 1.0)
     * @param critMultiplier the damage multiplier on crit
     * @return the damage after potential critical application
     */
    public static double applyCritical(double damage, double critChance, double critMultiplier) {
        if (damage <= 0) {
            LAST_CRIT_RESULT.set(new CritResult(false, 1.0));
            return 0;
        }
        
        double normalizedChance = Math.max(0, Math.min(1.0, critChance));
        double normalizedMultiplier = Math.max(1.0, critMultiplier);
        
        boolean isCrit = RANDOM.nextDouble() < normalizedChance;
        
        if (isCrit) {
            double critDamage = damage * normalizedMultiplier;
            LAST_CRIT_RESULT.set(new CritResult(true, normalizedMultiplier));
            
            LOGGER.fine(() -> String.format("Critical hit! Damage: %.2f -> %.2f (x%.2f)", 
                damage, critDamage, normalizedMultiplier));
            
            return critDamage;
        }
        
        LAST_CRIT_RESULT.set(new CritResult(false, 1.0));
        return damage;
    }

    /**
     * Checks if the last critical calculation resulted in a critical hit.
     * This is thread-safe and returns the result for the current thread's
     * last call to {@link #applyCritical}.
     *
     * @return true if the last hit was critical
     */
    public static boolean wasLastHitCritical() {
        return LAST_CRIT_RESULT.get().isCritical;
    }

    /**
     * Gets the critical multiplier from the last calculation.
     *
     * @return the crit multiplier (1.0 if not critical)
     */
    public static double getLastCritMultiplier() {
        return LAST_CRIT_RESULT.get().multiplier;
    }

    /**
     * Applies elemental or magical resistance to damage.
     * Formula: damage * (1 - resistance/100), capped at 75% reduction.
     *
     * @param damage the input damage
     * @param type the damage type (affects resistance application)
     * @param resistance the target's resistance value
     * @return the damage after resistance application
     */
    public static double applyResistance(double damage, DamageType type, double resistance) {
        if (damage <= 0) return 0;
        
        // True damage ignores all resistances
        if (type.ignoresDefenses()) {
            LOGGER.fine(() -> "True damage bypasses resistance");
            return damage;
        }
        
        // Calculate resistance percentage, capped at MAX_RESISTANCE_PERCENT
        final double resistancePercent = Math.max(0, Math.min(resistance / 100.0, MAX_RESISTANCE_PERCENT));
        
        double reducedDamage = damage * (1.0 - resistancePercent);
        
        final double finalDamage = damage;
        final double finalReducedDamage = reducedDamage;
        LOGGER.fine(() -> String.format("Resistance applied (%s): %.2f -> %.2f (%.1f%% reduction)", 
            type, finalDamage, finalReducedDamage, resistancePercent * 100));
        
        return reducedDamage;
    }

    /**
     * Applies armor mitigation to physical damage.
     * Formula: damage * (100 / (100 + armor))
     * This provides diminishing returns as armor increases.
     *
     * @param damage the input damage
     * @param armor the target's armor value
     * @return the damage after armor mitigation
     */
    public static double applyArmorMitigation(double damage, double armor) {
        if (damage <= 0) return 0;
        if (armor <= 0) return damage;
        
        double mitigation = ARMOR_CONSTANT / (ARMOR_CONSTANT + armor);
        double mitigatedDamage = damage * mitigation;
        
        LOGGER.fine(() -> String.format("Armor mitigation: %.2f -> %.2f (armor=%.2f, %.1f%% through)", 
            damage, mitigatedDamage, armor, mitigation * 100));
        
        return mitigatedDamage;
    }

    /**
     * Calculates the full damage pipeline from attacker to target.
     * Uses AttributeManager to retrieve stats for both entities.
     *
     * @param attacker the attacker's UUID (null for environmental damage)
     * @param target the target's UUID
     * @param rawDamage the base raw damage
     * @param type the damage type
     * @param skillId the skill ID that caused the damage (null for basic attacks)
     * @return the fully calculated Damage instance
     */
    public static Damage calculate(UUID attacker, UUID target, double rawDamage, DamageType type, String skillId) {
        Objects.requireNonNull(target, "Target UUID cannot be null");
        Objects.requireNonNull(type, "Damage type cannot be null");
        
        AttributeManager attrManager = AttributeManager.getInstance();
        
        // Get attacker stats (defaults if environmental damage)
        double attackerStrength = 0;
        double attackerIntelligence = 0;
        double critChance = 0.05; // 5% base crit chance
        double critMultiplier = DEFAULT_CRIT_MULTIPLIER;
        
        if (attacker != null && attrManager.hasPlayer(attacker)) {
            attackerStrength = attrManager.getAttribute(attacker, AttributeType.STRENGTH);
            attackerIntelligence = attrManager.getAttribute(attacker, AttributeType.INTELLIGENCE);
            
            // Get derived crit stats if available
            int attackerLevel = 1; // Default level, could be retrieved from PlayerRPGData
            critChance = attrManager.getDerivedStat(attacker, DerivedStat.CRITICAL_CHANCE, attackerLevel);
            critMultiplier = attrManager.getDerivedStat(attacker, DerivedStat.CRITICAL_DAMAGE, attackerLevel);
        }
        
        // Get target defensive stats
        double targetArmor = 0;
        double targetResistance = 0;
        
        if (attrManager.hasPlayer(target)) {
            int targetLevel = 1;
            targetArmor = attrManager.getDerivedStat(target, DerivedStat.ARMOR, targetLevel);
            
            // Get appropriate resistance based on damage type
            if (type.isMagical()) {
                targetResistance = attrManager.getDerivedStat(target, DerivedStat.MAGIC_RESIST, targetLevel);
            }
        }
        
        // Calculate damage through the pipeline
        double baseDamage;
        if (type.isMagical()) {
            baseDamage = calculateBaseMagicalDamage(rawDamage, attackerIntelligence, 1.0);
        } else {
            baseDamage = calculateBaseDamage(rawDamage, attackerStrength, 1.0);
        }
        
        // Apply critical hit
        double afterCrit = applyCritical(baseDamage, critChance, critMultiplier);
        boolean wasCrit = wasLastHitCritical();
        double appliedCritMultiplier = getLastCritMultiplier();
        
        // Apply resistance (for magical/elemental damage)
        double afterResist = applyResistance(afterCrit, type, targetResistance);
        
        // Apply armor (for physical damage only)
        double finalDamage;
        if (type.isPhysical()) {
            finalDamage = applyArmorMitigation(afterResist, targetArmor);
        } else if (type.ignoresDefenses()) {
            finalDamage = afterCrit; // True damage bypasses everything
        } else {
            finalDamage = afterResist;
        }
        
        double mitigated = afterCrit - finalDamage;
        
        LOGGER.fine(() -> String.format(
            "Damage calculation complete: raw=%.2f, base=%.2f, crit=%.2f, resist=%.2f, final=%.2f",
            rawDamage, baseDamage, afterCrit, afterResist, finalDamage));
        
        return Damage.builder()
            .attackerId(attacker)
            .targetId(target)
            .type(type)
            .rawAmount(afterCrit)
            .mitigatedAmount(mitigated)
            .finalAmount(finalDamage)
            .critical(wasCrit, appliedCritMultiplier)
            .sourceSkill(skillId)
            .build();
    }

    /**
     * Calculates damage without applying critical (deterministic).
     *
     * @param attacker the attacker's UUID
     * @param target the target's UUID
     * @param rawDamage the base raw damage
     * @param type the damage type
     * @param isCritical whether to force a critical hit
     * @param critMultiplier the critical multiplier to use
     * @param skillId the skill ID
     * @return the calculated Damage instance
     */
    public static Damage calculateDeterministic(UUID attacker, UUID target, double rawDamage, 
            DamageType type, boolean isCritical, double critMultiplier, String skillId) {
        
        Objects.requireNonNull(target, "Target UUID cannot be null");
        Objects.requireNonNull(type, "Damage type cannot be null");
        
        AttributeManager attrManager = AttributeManager.getInstance();
        
        // Get attacker stats
        double attackerStrength = 0;
        double attackerIntelligence = 0;
        
        if (attacker != null && attrManager.hasPlayer(attacker)) {
            attackerStrength = attrManager.getAttribute(attacker, AttributeType.STRENGTH);
            attackerIntelligence = attrManager.getAttribute(attacker, AttributeType.INTELLIGENCE);
        }
        
        // Get target defensive stats
        double targetArmor = 0;
        double targetResistance = 0;
        
        if (attrManager.hasPlayer(target)) {
            int targetLevel = 1;
            targetArmor = attrManager.getDerivedStat(target, DerivedStat.ARMOR, targetLevel);
            if (type.isMagical()) {
                targetResistance = attrManager.getDerivedStat(target, DerivedStat.MAGIC_RESIST, targetLevel);
            }
        }
        
        // Calculate base damage
        double baseDamage;
        if (type.isMagical()) {
            baseDamage = calculateBaseMagicalDamage(rawDamage, attackerIntelligence, 1.0);
        } else {
            baseDamage = calculateBaseDamage(rawDamage, attackerStrength, 1.0);
        }
        
        // Apply critical (deterministic)
        double afterCrit = isCritical ? baseDamage * Math.max(1.0, critMultiplier) : baseDamage;
        
        // Apply resistance
        double afterResist = applyResistance(afterCrit, type, targetResistance);
        
        // Apply armor
        double finalDamage;
        if (type.isPhysical()) {
            finalDamage = applyArmorMitigation(afterResist, targetArmor);
        } else if (type.ignoresDefenses()) {
            finalDamage = afterCrit;
        } else {
            finalDamage = afterResist;
        }
        
        double mitigated = afterCrit - finalDamage;
        
        return Damage.builder()
            .attackerId(attacker)
            .targetId(target)
            .type(type)
            .rawAmount(afterCrit)
            .mitigatedAmount(mitigated)
            .finalAmount(finalDamage)
            .critical(isCritical, isCritical ? critMultiplier : 1.0)
            .sourceSkill(skillId)
            .build();
    }

    /**
     * Calculates the effective health value considering armor.
     * Formula: health * (1 + armor/100)
     *
     * @param health the base health value
     * @param armor the armor value
     * @return the effective health
     */
    public static double calculateEffectiveHealth(double health, double armor) {
        return health * (1.0 + armor / ARMOR_CONSTANT);
    }

    /**
     * Calculates the armor required to achieve a specific damage reduction percentage.
     *
     * @param reductionPercent the desired reduction (0.0 to less than 1.0)
     * @return the required armor value
     */
    public static double calculateRequiredArmor(double reductionPercent) {
        if (reductionPercent <= 0) return 0;
        if (reductionPercent >= 1.0) return Double.MAX_VALUE;
        return ARMOR_CONSTANT * reductionPercent / (1.0 - reductionPercent);
    }

    /**
     * Internal class to store critical hit results.
     */
    private static record CritResult(boolean isCritical, double multiplier) {}
}
