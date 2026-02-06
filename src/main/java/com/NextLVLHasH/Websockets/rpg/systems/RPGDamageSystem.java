package com.NextLVLHasH.Websockets.rpg.systems;

import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.combat.DamageType;
import com.NextLVLHasH.Websockets.rpg.combat.Damage;
import com.NextLVLHasH.Websockets.rpg.combat.DamageCalculator;
import com.NextLVLHasH.Websockets.rpg.combat.DamageEvent;
import com.NextLVLHasH.Websockets.rpg.combat.CombatManager;
import com.NextLVLHasH.Websockets.rpg.bridge.RPGDiscordBridge;
import com.NextLVLHasH.Websockets.rpg.skills.PerkStatApplier;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * RPG Damage Integration System.
 * 
 * This system hooks into the RPG CombatManager's damage event listener
 * to apply RPG-specific damage modifications and track combat statistics.
 * 
 * Unlike EntityEventSystem-based approaches, this uses the internal RPG
 * damage event system which gives us full control over damage calculations.
 * 
 * Features:
 * - Applies RPG attribute modifiers (strength, defense, etc.)
 * - Tracks damage dealt/received statistics
 * - Sends significant combat events to Discord
 * - Works with our internal Damage/DamageEvent system
 */
public class RPGDamageSystem implements CombatManager.DamageEventListener {
    
    private static final Logger LOGGER = Logger.getLogger("RPGDamageSystem");
    private final RPGManager rpgManager;
    private final RPGDiscordBridge discordBridge;
    private boolean enabled = true;
    
    public RPGDamageSystem(RPGManager rpgManager, RPGDiscordBridge discordBridge) {
        this.rpgManager = rpgManager;
        this.discordBridge = discordBridge;
        LOGGER.info("RPGDamageSystem initialized");
    }
    
    /**
     * Register this system with the combat manager.
     */
    public void register() {
        CombatManager combatManager = rpgManager.getCombatManager();
        if (combatManager != null) {
            combatManager.addDamageListener(this);
            LOGGER.info("RPGDamageSystem registered with CombatManager");
        }
    }
    
    /**
     * Unregister this system from the combat manager.
     */
    public void unregister() {
        CombatManager combatManager = rpgManager.getCombatManager();
        if (combatManager != null) {
            combatManager.removeDamageListener(this);
            LOGGER.info("RPGDamageSystem unregistered from CombatManager");
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void onDamage(DamageEvent event) {
        if (!enabled || event.isCancelled()) {
            return;
        }
        
        try {
            Damage damage = event.getDamage();
            UUID targetId = damage.getTargetId();
            UUID attackerId = damage.getAttackerId();
            
            if (targetId == null) {
                return; // No target
            }
            
            // Get target RPG data
            PlayerRPGData targetData = rpgManager.getPlayerData(targetId);
            if (targetData == null) {
                return; // Target has no RPG data
            }
            
            // Calculate damage with RPG modifiers
            double rawDamage = damage.getRawAmount();
            DamageType damageType = damage.getType();
            
            // Apply attacker's damage bonus from perks (damage_bonus, critical_chance, etc.)
            if (attackerId != null) {
                double damageBonus = PerkStatApplier.getGameplayModifier(attackerId, "damage_bonus");
                rawDamage *= (1.0 + damageBonus);
                
                // Check for critical hit
                double critChance = PerkStatApplier.getGameplayModifier(attackerId, "critical_chance");
                if (Math.random() < critChance) {
                    double critDamage = 1.5 + PerkStatApplier.getGameplayModifier(attackerId, "critical_damage");
                    rawDamage *= critDamage;
                    LOGGER.fine("Critical hit! Damage multiplied by " + critDamage);
                }
            }
            
            // Apply defense calculations using static utility methods
            double defense = targetData.getCurrentAttribute(
                com.NextLVLHasH.Websockets.rpg.core.AttributeType.CONSTITUTION);
            double mitigated = DamageCalculator.applyArmorMitigation(rawDamage, defense);
            
            // Apply resistance based on damage type
            double resistance = getResistance(targetData, damageType);
            double finalDamage = DamageCalculator.applyResistance(mitigated, damageType, resistance);
            
            // Apply target's damage reduction from perks
            double damageReduction = PerkStatApplier.getGameplayModifier(targetId, "damage_reduction");
            finalDamage *= (1.0 - damageReduction);
            
            // Update the event with modified damage
            event.setModifiedDamage(finalDamage);
            
            // Log the damage
            String targetName = targetData.getPlayerName();
            LOGGER.fine(String.format("Player %s took %.1f damage (raw: %.1f, type: %s)",
                    targetName, finalDamage, rawDamage, damageType.name()));
            
            // Send to Discord if significant damage
            if (discordBridge != null && finalDamage >= 10.0) {
                discordBridge.onPlayerDamaged(targetId, targetName, finalDamage, damageType, targetData);
            }
            
            // Track attacker stats if it's a player
            if (attackerId != null) {
                PlayerRPGData attackerData = rpgManager.getPlayerData(attackerId);
                if (attackerData != null) {
                    // Could track damage dealt statistics here
                    LOGGER.fine(String.format("Player %s dealt %.1f damage to %s",
                            attackerData.getPlayerName(), finalDamage, targetName));
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error processing RPG damage: " + e.getMessage());
        }
    }
    
    /**
     * Get the player's resistance to a specific damage type.
     */
    private double getResistance(PlayerRPGData playerData, DamageType damageType) {
        // Get resistance from race bonuses if applicable
        String raceId = playerData.getSelectedRace();
        if (raceId != null && rpgManager.getCharacterManager() != null) {
            com.NextLVLHasH.Websockets.rpg.character.Race race = 
                rpgManager.getCharacterManager().getRace(raceId);
            if (race != null) {
                return race.getResistance(damageType.name().toLowerCase());
            }
        }
        return 0.0;
    }
}
