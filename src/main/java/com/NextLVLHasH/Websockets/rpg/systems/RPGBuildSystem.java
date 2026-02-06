package com.NextLVLHasH.Websockets.rpg.systems;

import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.bridge.RPGDiscordBridge;
import com.NextLVLHasH.Websockets.rpg.skills.PerkStatApplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ECS System that grants XP for building/placing blocks.
 * 
 * XP rewards are based on block complexity:
 * - Simple blocks (dirt, cobblestone): minimal XP
 * - Crafted blocks (planks, bricks): medium XP
 * - Complex structures: higher XP
 * 
 * This falls under the Utilities skill category.
 */
public class RPGBuildSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    
    private static final Logger LOGGER = Logger.getLogger("RPGBuildSystem");
    private final RPGManager rpgManager;
    private final RPGDiscordBridge discordBridge;
    
    // XP rewards per block type
    private final Map<String, Long> blockXPRewards = new ConcurrentHashMap<>();
    private long defaultBlockXP = 1L;
    
    // XP multipliers
    private double globalBuildXPMultiplier = 1.0;
    
    // Cooldown to prevent XP spam
    private final Map<UUID, Long> lastBuildTime = new ConcurrentHashMap<>();
    private static final long BUILD_COOLDOWN_MS = 100; // 100ms between XP grants
    
    // Track consecutive builds for combo bonus
    private final Map<UUID, Integer> buildStreak = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBuildStreakTime = new ConcurrentHashMap<>();
    private static final long STREAK_TIMEOUT_MS = 2000; // 2 seconds to maintain streak
    private static final int MAX_STREAK_BONUS = 10; // Max 10x streak multiplier
    
    public RPGBuildSystem(RPGManager rpgManager, RPGDiscordBridge discordBridge) {
        super(PlaceBlockEvent.class);
        this.rpgManager = rpgManager;
        this.discordBridge = discordBridge;
        initializeBlockXPRewards();
        LOGGER.fine("RPGBuildSystem initialized");
    }
    
    /**
     * Initialize XP rewards for different block types
     */
    private void initializeBlockXPRewards() {
        // Natural blocks - minimal XP (easy to get)
        blockXPRewards.put("dirt", 1L);
        blockXPRewards.put("sand", 1L);
        blockXPRewards.put("gravel", 1L);
        blockXPRewards.put("cobblestone", 1L);
        blockXPRewards.put("stone", 2L);
        
        // Processed basic blocks - low XP
        blockXPRewards.put("oak_planks", 2L);
        blockXPRewards.put("birch_planks", 2L);
        blockXPRewards.put("spruce_planks", 2L);
        blockXPRewards.put("jungle_planks", 2L);
        blockXPRewards.put("acacia_planks", 2L);
        blockXPRewards.put("dark_oak_planks", 2L);
        blockXPRewards.put("smooth_stone", 3L);
        blockXPRewards.put("polished_granite", 3L);
        blockXPRewards.put("polished_diorite", 3L);
        blockXPRewards.put("polished_andesite", 3L);
        
        // Bricks and structural blocks - medium XP
        blockXPRewards.put("bricks", 4L);
        blockXPRewards.put("stone_bricks", 4L);
        blockXPRewards.put("mossy_stone_bricks", 5L);
        blockXPRewards.put("cracked_stone_bricks", 5L);
        blockXPRewards.put("chiseled_stone_bricks", 6L);
        blockXPRewards.put("deepslate_bricks", 5L);
        blockXPRewards.put("deepslate_tiles", 5L);
        
        // Decorative blocks - higher XP
        blockXPRewards.put("glass", 3L);
        blockXPRewards.put("stained_glass", 4L);
        blockXPRewards.put("terracotta", 4L);
        blockXPRewards.put("glazed_terracotta", 6L);
        blockXPRewards.put("concrete", 4L);
        blockXPRewards.put("wool", 3L);
        
        // Functional blocks - medium-high XP
        blockXPRewards.put("crafting_table", 5L);
        blockXPRewards.put("furnace", 5L);
        blockXPRewards.put("chest", 5L);
        blockXPRewards.put("barrel", 5L);
        blockXPRewards.put("smoker", 6L);
        blockXPRewards.put("blast_furnace", 7L);
        blockXPRewards.put("anvil", 10L);
        blockXPRewards.put("enchanting_table", 15L);
        
        // Redstone/mechanical - high XP
        blockXPRewards.put("redstone_block", 8L);
        blockXPRewards.put("piston", 8L);
        blockXPRewards.put("sticky_piston", 10L);
        blockXPRewards.put("observer", 10L);
        blockXPRewards.put("hopper", 10L);
        blockXPRewards.put("dispenser", 8L);
        blockXPRewards.put("dropper", 8L);
        
        // Valuable blocks - very high XP
        blockXPRewards.put("iron_block", 15L);
        blockXPRewards.put("gold_block", 20L);
        blockXPRewards.put("diamond_block", 50L);
        blockXPRewards.put("emerald_block", 60L);
        blockXPRewards.put("netherite_block", 100L);
        
        // Lighting - medium XP
        blockXPRewards.put("torch", 1L);
        blockXPRewards.put("lantern", 4L);
        blockXPRewards.put("soul_lantern", 5L);
        blockXPRewards.put("glowstone", 6L);
        blockXPRewards.put("sea_lantern", 8L);
        
        // Hytale specific (adjust as needed)
        blockXPRewards.put("kweebec_planks", 3L);
        blockXPRewards.put("verdant_crystal", 15L);
        blockXPRewards.put("mithril_block", 30L);
    }
    
    public void setBlockXP(String blockType, long xp) {
        blockXPRewards.put(blockType.toLowerCase(), xp);
    }
    
    public void setDefaultBlockXP(long xp) {
        this.defaultBlockXP = xp;
    }
    
    public void setGlobalBuildXPMultiplier(double multiplier) {
        this.globalBuildXPMultiplier = Math.max(0.0, multiplier);
    }
    
    @Override
    public void handle(int i,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {
        try {
            // Skip if cancelled
            if (event.isCancelled()) return;
            
            // Get the player who placed the block
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
            Player player = store.getComponent(ref, Player.getComponentType());
            
            if (player == null) return;
            
            @SuppressWarnings("removal")
            PlayerRef playerRef = player.getPlayerRef();
            UUID playerUuid = playerRef.getUuid();
            String playerName = playerRef.getUsername();
            
            // Check cooldown to prevent XP spam
            long now = System.currentTimeMillis();
            Long lastTime = lastBuildTime.get(playerUuid);
            if (lastTime != null && (now - lastTime) < BUILD_COOLDOWN_MS) {
                return;
            }
            lastBuildTime.put(playerUuid, now);
            
            // Get player RPG data
            PlayerRPGData rpgData = rpgManager.getPlayerData(playerUuid);
            if (rpgData == null) {
                rpgData = rpgManager.initializePlayer(playerUuid, playerName);
            }
            
            // Get block type
            String blockType = getBlockType(event);
            
            // Calculate XP reward
            long baseXP = blockXPRewards.getOrDefault(blockType.toLowerCase(), defaultBlockXP);
            
            // Apply profession/class building bonus
            double professionMultiplier = getBuildProfessionMultiplier(rpgData);
            
            // Calculate build streak bonus
            double streakMultiplier = updateAndGetStreakMultiplier(playerUuid, now);
            
            // Apply perk bonuses (building_xp_bonus from perks)
            double perkMultiplier = 1.0 + PerkStatApplier.getGameplayModifier(playerUuid, "building_xp_bonus");
            
            // Calculate final XP
            long finalXP = Math.round(baseXP * professionMultiplier * streakMultiplier * perkMultiplier * globalBuildXPMultiplier);
            
            if (finalXP <= 0) return;
            
            // Track level before XP gain
            int levelBefore = rpgData.getLevel();
            
            // Award XP
            rpgData.addXP(finalXP);
            
            // Increment building stat
            rpgData.incrementStat("blocks_placed");
            rpgData.incrementStat("building_xp_earned", finalXP);
            
            // Check for level up
            int levelAfter = rpgData.getLevel();
            if (levelAfter > levelBefore) {
                handleLevelUp(playerUuid, playerName, rpgData, levelBefore, levelAfter);
            }
            
            LOGGER.finest(String.format("Player %s placed %s: +%d XP (streak: %.1fx)", 
                    playerName, blockType, finalXP, streakMultiplier));
            
        } catch (Exception e) {
            LOGGER.warning("Error processing building XP: " + e.getMessage());
        }
    }
    
    /**
     * Get the block type from the event
     */
    @SuppressWarnings("null")
    private String getBlockType(PlaceBlockEvent event) {
        try {
            if (event.getItemInHand() != null) {
                // ItemStack doesn't have getType(), use toString or getId if available
                return event.getItemInHand().toString();
            }
        } catch (Exception e) {
            // Fallback
        }
        return "unknown";
    }
    
    /**
     * Get building XP multiplier based on profession/class
     */
    private double getBuildProfessionMultiplier(PlayerRPGData rpgData) {
        String profession = rpgData.getSelectedProfession();
        if (profession == null) return 1.0;
        
        return switch (profession.toLowerCase()) {
            case "architect" -> 1.5;  // Building specialist
            case "engineer" -> 1.3;   // Technical building
            case "crafter" -> 1.2;    // General crafting
            case "artisan" -> 1.25;   // Decorative focus
            default -> 1.0;
        };
    }
    
    /**
     * Update and get the build streak multiplier
     * Building quickly in succession grants bonus XP
     */
    private double updateAndGetStreakMultiplier(UUID playerUuid, long now) {
        Long lastStreakTime = lastBuildStreakTime.get(playerUuid);
        int currentStreak = buildStreak.getOrDefault(playerUuid, 0);
        
        if (lastStreakTime == null || (now - lastStreakTime) > STREAK_TIMEOUT_MS) {
            // Streak expired, reset
            currentStreak = 1;
        } else {
            // Continue streak
            currentStreak = Math.min(currentStreak + 1, MAX_STREAK_BONUS * 10);
        }
        
        buildStreak.put(playerUuid, currentStreak);
        lastBuildStreakTime.put(playerUuid, now);
        
        // Calculate multiplier (max 2x at 100 blocks)
        return 1.0 + Math.min((currentStreak / 100.0), 1.0);
    }
    
    /**
     * Handle player level up
     */
    private void handleLevelUp(UUID playerUuid, String playerName, PlayerRPGData rpgData,
                               int oldLevel, int newLevel) {
        LOGGER.info(String.format("Player %s leveled up from building! %d -> %d", playerName, oldLevel, newLevel));
        
        // Show level-up notification
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
