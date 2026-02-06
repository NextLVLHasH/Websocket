package com.NextLVLHasH.Websockets.rpg.skills;

import java.util.*;


/**
 * Calculates perk stat modifiers and gameplay bonuses for players.
 * 
 * This class serves as a bridge between the Perk system and the game's stat system.
 * It calculates total modifiers from all unlocked perks and provides utility methods
 * for applying these modifiers in game systems.
 * 
 * Actual stat application to Hytale's EntityStatMap should be done in the
 * system that has access to the player's entity components (e.g., on player join).
 * 
 * Example usage:
 * - In RPGMiningSystem: multiply XP by (1 + getGameplayModifier(uuid, "mining_xp_bonus"))
 * - In RPGDamageSystem: multiply damage by (1 + getGameplayModifier(uuid, "damage_bonus"))
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class PerkStatApplier {
    
    /** Stat name constants for Hytale's DefaultEntityStatTypes */
    public static final String STAT_HEALTH = "Health";
    public static final String STAT_MANA = "Mana";
    public static final String STAT_STAMINA = "Stamina";
    public static final String STAT_OXYGEN = "Oxygen";
    public static final String STAT_SIGNATURE_ENERGY = "SignatureEnergy";
    public static final String STAT_AMMO = "Ammo";
    
    // ==================== Gameplay Modifier Helpers ====================
    
    /**
     * Gets the total value for a gameplay modifier from all player perks.
     * Use this for custom modifiers like damage_bonus, mining_xp_bonus, etc.
     * 
     * Common modifier keys:
     * - "damage_bonus" - bonus damage dealt (0.25 = +25%)
     * - "damage_reduction" - reduced damage taken (0.10 = -10% damage taken)
     * - "critical_chance" - chance to crit (0.15 = 15% chance)
     * - "critical_damage" - bonus crit damage (0.50 = +50% crit multiplier)
     * - "mining_xp_bonus" - bonus mining XP (0.25 = +25%)
     * - "mining_speed_bonus" - faster mining (0.10 = +10%)
     * - "building_xp_bonus" - bonus building XP
     * - "health_regen_bonus" - faster health regen
     * - "mana_regen_bonus" - faster mana regen
     * - "mana_cost_reduction" - reduced spell costs
     * 
     * @param playerId the player's UUID
     * @param modifierKey the gameplay modifier key
     * @return total modifier value (additive sum of all perk bonuses)
     */
    public static double getGameplayModifier(UUID playerId, String modifierKey) {
        return PerkManager.getInstance().getTotalGameplayModifier(playerId, modifierKey);
    }
    
    /**
     * Calculates a modified value based on gameplay modifiers.
     * 
     * Example: calculateModifiedValue(100, playerId, "damage_bonus")
     * If player has +25% damage_bonus from perks, returns 125.0
     * 
     * @param baseValue the base value to modify
     * @param playerId the player's UUID
     * @param modifierKey the gameplay modifier key
     * @return the modified value
     */
    public static double calculateModifiedValue(double baseValue, UUID playerId, String modifierKey) {
        double modifier = getGameplayModifier(playerId, modifierKey);
        return baseValue * (1.0 + modifier);
    }
    
    /**
     * Calculates a reduced value based on a reduction modifier.
     * Use for damage_reduction type modifiers where higher = better protection.
     * 
     * Example: calculateReducedValue(100, playerId, "damage_reduction")
     * If player has 20% damage_reduction, returns 80.0
     * 
     * @param baseValue the base value to reduce
     * @param playerId the player's UUID
     * @param modifierKey the reduction modifier key
     * @return the reduced value
     */
    public static double calculateReducedValue(double baseValue, UUID playerId, String modifierKey) {
        double reduction = getGameplayModifier(playerId, modifierKey);
        return baseValue * (1.0 - Math.min(reduction, 0.9)); // Cap at 90% reduction
    }
    
    // ==================== Stat Modifier Calculations ====================
    
    /**
     * Get all stat modifiers for a player that should be applied to Hytale's EntityStatMap.
     * Returns a map of stat name -> total additive bonus.
     * 
     * This should be called when a player joins or when perks change, and the
     * resulting modifiers should be applied using:
     * 
     *   EntityStatMap statMap = store.getComponent(playerRef, EntityStatMap.getComponentType());
     *   for each (statName, amount) in getAdditiveStatBonuses(playerId):
     *       statMap.putModifier(statIndex, "perk_" + statName, new StaticModifier(MAX, ADDITIVE, amount));
     * 
     * @param playerId the player's UUID
     * @return map of stat name -> total additive bonus
     */
    public static Map<String, Float> getAdditiveStatBonuses(UUID playerId) {
        Map<String, Float> result = new HashMap<>();
        PerkManager perkManager = PerkManager.getInstance();
        
        for (Perk perk : perkManager.getUnlockedPerkObjects(playerId)) {
            for (Map.Entry<String, Perk.PerkModifier> entry : perk.getStatModifiers().entrySet()) {
                String statName = normalizeStatName(entry.getKey());
                Perk.PerkModifier mod = entry.getValue();
                
                if (mod.getCalculationType() == Perk.CalculationType.ADDITIVE) {
                    result.merge(statName, mod.getAmount(), Float::sum);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get all percentage stat modifiers for a player.
     * Returns a map of stat name -> total percentage bonus (0.25 = +25%).
     * 
     * These should be applied using MULTIPLICATIVE calculation type.
     * 
     * @param playerId the player's UUID
     * @return map of stat name -> total percentage bonus
     */
    public static Map<String, Float> getPercentageStatBonuses(UUID playerId) {
        Map<String, Float> result = new HashMap<>();
        PerkManager perkManager = PerkManager.getInstance();
        
        for (Perk perk : perkManager.getUnlockedPerkObjects(playerId)) {
            for (Map.Entry<String, Perk.PerkModifier> entry : perk.getStatModifiers().entrySet()) {
                String statName = normalizeStatName(entry.getKey());
                Perk.PerkModifier mod = entry.getValue();
                
                if (mod.getCalculationType() == Perk.CalculationType.MULTIPLICATIVE) {
                    result.merge(statName, mod.getAmount(), Float::sum);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Calculate the final stat value after applying all perk bonuses.
     * 
     * @param baseStat the base stat value
     * @param playerId the player's UUID  
     * @param statName the stat name (Health, Mana, etc.)
     * @return the modified stat value
     */
    public static float calculateFinalStat(float baseStat, UUID playerId, String statName) {
        String normalized = normalizeStatName(statName);
        
        // Get additive bonus
        float additive = getAdditiveStatBonuses(playerId).getOrDefault(normalized, 0f);
        
        // Get percentage bonus
        float percentage = getPercentageStatBonuses(playerId).getOrDefault(normalized, 0f);
        
        // Apply: (base + additive) * (1 + percentage)
        return (baseStat + additive) * (1f + percentage);
    }
    
    /**
     * Normalize stat name for consistent lookup.
     * Removes suffixes like "_percent", "_bonus", "_max"
     */
    private static String normalizeStatName(String statName) {
        if (statName == null) return "";
        return statName.toLowerCase()
                .replace("_percent", "")
                .replace("_bonus", "")
                .replace("_max", "");
    }
    
    // ==================== Debug Methods ====================
    
    /**
     * Debug method to print all modifiers from a player's perks.
     */
    public static void debugPrintPlayerModifiers(UUID playerId) {
        PerkManager perkManager = PerkManager.getInstance();
        
        System.out.println("=== Perk Modifiers for " + playerId + " ===");
        System.out.println("Unlocked perks: " + perkManager.getUnlockedPerks(playerId));
        
        for (Perk perk : perkManager.getUnlockedPerkObjects(playerId)) {
            System.out.println("\nPerk: " + perk.getDisplayName());
            
            if (!perk.getStatModifiers().isEmpty()) {
                System.out.println("  Stat Modifiers (Hytale EntityStatMap):");
                for (Map.Entry<String, Perk.PerkModifier> entry : perk.getStatModifiers().entrySet()) {
                    System.out.println("    " + entry.getKey() + ": " + entry.getValue());
                }
            }
            
            if (!perk.getGameplayModifiers().isEmpty()) {
                System.out.println("  Gameplay Modifiers:");
                for (Map.Entry<String, Double> entry : perk.getGameplayModifiers().entrySet()) {
                    double value = entry.getValue();
                    String formatted = (value >= 0 ? "+" : "") + ((int)(value * 100)) + "%";
                    System.out.println("    " + entry.getKey() + ": " + formatted);
                }
            }
        }
        
        System.out.println("\n=== Total Gameplay Modifiers ===");
        String[] commonModifiers = {
                "damage_bonus", "damage_reduction", "critical_chance", "critical_damage",
                "mining_xp_bonus", "mining_speed_bonus", "rare_ore_chance", "double_drop_chance",
                "health_regen_bonus", "mana_regen_bonus", "mana_cost_reduction", "spell_damage_bonus"
        };
        
        for (String mod : commonModifiers) {
            double value = perkManager.getTotalGameplayModifier(playerId, mod);
            if (value != 0) {
                String formatted = (value >= 0 ? "+" : "") + ((int)(value * 100)) + "%";
                System.out.println("  " + mod + ": " + formatted);
            }
        }
        
        System.out.println("\n=== Total Stat Bonuses (for EntityStatMap) ===");
        Map<String, Float> additive = getAdditiveStatBonuses(playerId);
        Map<String, Float> percentage = getPercentageStatBonuses(playerId);
        
        Set<String> allStats = new HashSet<>();
        allStats.addAll(additive.keySet());
        allStats.addAll(percentage.keySet());
        
        for (String stat : allStats) {
            Float add = additive.get(stat);
            Float pct = percentage.get(stat);
            StringBuilder sb = new StringBuilder("  " + stat + ":");
            if (add != null && add != 0) {
                sb.append(" +").append(add.intValue()).append(" flat");
            }
            if (pct != null && pct != 0) {
                sb.append(" +").append((int)(pct * 100)).append("%");
            }
            System.out.println(sb);
        }
    }
}
