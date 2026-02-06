package com.NextLVLHasH.Websockets.rpg.systems;

import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.bridge.RPGDiscordBridge;
import com.NextLVLHasH.Websockets.rpg.progression.ProgressionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ECS System that hooks into Hytale's KillFeedEvent for RPG XP rewards.
 * 
 * This system:
 * - Grants XP when players kill mobs
 * - Applies XP multipliers based on class/race/profession
 * - Tracks kill statistics
 * - Sends kill events to Discord
 * - Handles level-up notifications
 * 
 * Requires: DamageModule dependency in manifest.json
 */
public class RPGKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    
    private static final Logger LOGGER = Logger.getLogger("RPGKillSystem");
    private final RPGManager rpgManager;
    private final RPGDiscordBridge discordBridge;
    
    // Base XP rewards per mob type (can be loaded from config)
    private final Map<String, Long> mobXPRewards = new ConcurrentHashMap<>();
    private long defaultMobXP = 25L;
    
    // XP multipliers
    private double globalXPMultiplier = 1.0;
    
    public RPGKillSystem(RPGManager rpgManager, RPGDiscordBridge discordBridge) {
        super(KillFeedEvent.KillerMessage.class);
        this.rpgManager = rpgManager;
        this.discordBridge = discordBridge;
        initializeDefaultXPRewards();
        LOGGER.info("RPGKillSystem initialized");
    }
    
    /**
     * Initialize default XP rewards for different mob types
     */
    private void initializeDefaultXPRewards() {
        // Common mobs
        mobXPRewards.put("zombie", 20L);
        mobXPRewards.put("skeleton", 25L);
        mobXPRewards.put("spider", 15L);
        mobXPRewards.put("trork", 30L);
        mobXPRewards.put("trork_warrior", 50L);
        mobXPRewards.put("kweebec", 10L);
        
        // Medium difficulty
        mobXPRewards.put("scarak", 75L);
        mobXPRewards.put("outlander", 60L);
        mobXPRewards.put("feran", 100L);
        
        // Mini-bosses
        mobXPRewards.put("trork_chieftain", 500L);
        mobXPRewards.put("scarak_queen", 750L);
        
        // Bosses
        mobXPRewards.put("void_dragon", 5000L);
        mobXPRewards.put("boss", 2500L);
        
        // Players (PvP)
        mobXPRewards.put("player", 100L);
    }
    
    public void setMobXP(String mobType, long xp) {
        mobXPRewards.put(mobType.toLowerCase(), xp);
    }
    
    public void setDefaultMobXP(long xp) {
        this.defaultMobXP = xp;
    }
    
    public void setGlobalXPMultiplier(double multiplier) {
        this.globalXPMultiplier = Math.max(0.0, multiplier);
    }
    
    @Override
    public void handle(int i,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull KillFeedEvent.KillerMessage event) {
        try {
            // Get the killer entity
            Ref<EntityStore> killerRef = archetypeChunk.getReferenceTo(i);
            Player player = store.getComponent(killerRef, Player.getComponentType());
            
            if (player == null) {
                return; // Not a player kill
            }
            
            @SuppressWarnings("removal")
            PlayerRef playerRef = player.getPlayerRef();
            UUID playerUuid = playerRef.getUuid();
            String playerName = playerRef.getUsername();
            
            PlayerRPGData rpgData = rpgManager.getPlayerData(playerUuid);
            if (rpgData == null) {
                // Auto-initialize RPG data for new players
                rpgData = rpgManager.initializePlayer(playerUuid, playerName);
            }
            
            // Determine killed entity type
            String killedType = getKilledEntityType(event);
            
            // Calculate XP reward
            long baseXP = mobXPRewards.getOrDefault(killedType.toLowerCase(), defaultMobXP);
            
            // Apply class XP multiplier
            double classMultiplier = getClassXPMultiplier(rpgData.getSelectedClass());
            
            // Apply level scaling (higher level mobs compared to player = more XP)
            double levelScaling = 1.0; // Can be extended later
            
            // Calculate final XP
            long finalXP = Math.round(baseXP * classMultiplier * globalXPMultiplier * levelScaling);
            
            // Track level before XP gain
            int levelBefore = rpgData.getLevel();
            @SuppressWarnings("unused")
            long xpBefore = rpgData.getCurrentXP();
            
            // Award XP
            rpgData.addXP(finalXP);
            
            // Check for level up
            int levelAfter = rpgData.getLevel();
            boolean leveledUp = levelAfter > levelBefore;
            
            LOGGER.fine(String.format("Player %s killed %s: +%d XP (base: %d, mult: %.2f)", 
                    playerName, killedType, finalXP, baseXP, classMultiplier));
            
            // Handle level up
            if (leveledUp) {
                handleLevelUp(playerUuid, playerName, rpgData, levelBefore, levelAfter);
            }
            
            // Send to Discord
            if (discordBridge != null) {
                discordBridge.onPlayerKill(playerUuid, playerName, killedType, finalXP, leveledUp, rpgData);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error processing RPG kill: " + e.getMessage());
        }
    }
    
    /**
     * Get the type of entity that was killed
     */
    private String getKilledEntityType(KillFeedEvent.KillerMessage event) {
        // Try to get entity type from the event
        // The exact method depends on Hytale's API
        try {
            // This is a placeholder - actual implementation depends on event structure
            return event.getClass().getSimpleName().replace("Message", "");
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get XP multiplier for a character class
     */
    private double getClassXPMultiplier(String className) {
        if (className == null) return 1.0;
        
        return switch (className.toLowerCase()) {
            case "warrior" -> 1.0;    // Balanced
            case "mage" -> 1.1;       // Slight bonus (harder to survive early)
            case "rogue" -> 1.15;     // Higher bonus (glass cannon)
            case "ranger" -> 1.05;    // Small bonus
            case "cleric" -> 0.95;    // Slight penalty (support class)
            case "paladin" -> 1.0;    // Balanced
            default -> 1.0;
        };
    }
    
    /**
     * Handle player level up
     */
    private void handleLevelUp(UUID playerUuid, String playerName, PlayerRPGData rpgData, 
                               int oldLevel, int newLevel) {
        LOGGER.info(String.format("Player %s leveled up! %d -> %d", playerName, oldLevel, newLevel));
        
        // Apply level-up stat bonuses via ProgressionManager
        ProgressionManager progressionManager = rpgManager.getProgressionManager();
        if (progressionManager != null) {
            // Get level up modifiers for the player's class
            var modifier = progressionManager.getLevelUpModifier(rpgData.getSelectedClass());
            if (modifier != null) {
                // Apply stat increases per level gained
                int levelsGained = newLevel - oldLevel;
                for (int i = 0; i < levelsGained; i++) {
                    rpgData.setMaxHealth(rpgData.getMaxHealth() + modifier.healthPerLevel());
                    rpgData.setMaxMana(rpgData.getMaxMana() + modifier.manaPerLevel());
                    rpgData.setMaxStamina(rpgData.getMaxStamina() + modifier.staminaPerLevel());
                    rpgData.addSkillPoints(modifier.skillPointsPerLevel());
                }
            }
        }
        
        // Restore stats on level up
        rpgData.setCurrentHealth(rpgData.getMaxHealth());
        rpgData.setCurrentMana(rpgData.getMaxMana());
        rpgData.setCurrentStamina(rpgData.getMaxStamina());
        
        // Show level-up notification via UI manager
        rpgManager.getUIManager().showLevelUpNotification(playerUuid, newLevel);
        
        // Notify Discord
        if (discordBridge != null) {
            discordBridge.onPlayerLevelUp(playerUuid, playerName, newLevel, rpgData);
        }
    }
    
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
