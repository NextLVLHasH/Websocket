package com.NextLVLHasH.Websockets.rpg.systems;

import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
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
 * ECS System that grants XP for mining/breaking blocks.
 * 
 * XP rewards are based on block type and rarity:
 * - Common blocks (dirt, stone): minimal XP
 * - Ores (iron, gold, diamond): higher XP
 * - Rare materials: significant XP
 * 
 * This falls under the Utilities skill category.
 */
public class RPGMiningSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    
    private static final Logger LOGGER = Logger.getLogger("RPGMiningSystem");
    private final RPGManager rpgManager;
    private final RPGDiscordBridge discordBridge;
    
    // XP rewards per block type
    private final Map<String, Long> blockXPRewards = new ConcurrentHashMap<>();
    private long defaultBlockXP = 1L;
    
    // XP multipliers
    private double globalMiningXPMultiplier = 1.0;
    
    // Cooldown to prevent XP spam (block ID -> last mine time per player)
    private final Map<UUID, Long> lastMineTime = new ConcurrentHashMap<>();
    private static final long MINE_COOLDOWN_MS = 100; // 100ms between XP grants
    
    public RPGMiningSystem(RPGManager rpgManager, RPGDiscordBridge discordBridge) {
        super(BreakBlockEvent.class);
        this.rpgManager = rpgManager;
        this.discordBridge = discordBridge;
        initializeBlockXPRewards();
        LOGGER.fine("RPGMiningSystem initialized");
    }
    
    /**
     * Initialize XP rewards for different block types
     */
    private void initializeBlockXPRewards() {
        // Common blocks - minimal XP
        blockXPRewards.put("dirt", 1L);
        blockXPRewards.put("grass", 1L);
        blockXPRewards.put("sand", 1L);
        blockXPRewards.put("gravel", 1L);
        blockXPRewards.put("clay", 2L);
        
        // Stone variants - low XP
        blockXPRewards.put("stone", 2L);
        blockXPRewards.put("cobblestone", 1L);
        blockXPRewards.put("granite", 2L);
        blockXPRewards.put("diorite", 2L);
        blockXPRewards.put("andesite", 2L);
        blockXPRewards.put("deepslate", 3L);
        
        // Wood - low XP
        blockXPRewards.put("oak_log", 3L);
        blockXPRewards.put("birch_log", 3L);
        blockXPRewards.put("spruce_log", 3L);
        blockXPRewards.put("jungle_log", 4L);
        blockXPRewards.put("acacia_log", 4L);
        blockXPRewards.put("dark_oak_log", 4L);
        
        // Common ores - medium XP
        blockXPRewards.put("coal_ore", 5L);
        blockXPRewards.put("copper_ore", 8L);
        blockXPRewards.put("iron_ore", 10L);
        blockXPRewards.put("deepslate_coal_ore", 7L);
        blockXPRewards.put("deepslate_copper_ore", 10L);
        blockXPRewards.put("deepslate_iron_ore", 12L);
        
        // Valuable ores - high XP
        blockXPRewards.put("gold_ore", 15L);
        blockXPRewards.put("redstone_ore", 12L);
        blockXPRewards.put("lapis_ore", 15L);
        blockXPRewards.put("deepslate_gold_ore", 18L);
        blockXPRewards.put("deepslate_redstone_ore", 15L);
        blockXPRewards.put("deepslate_lapis_ore", 18L);
        
        // Rare ores - very high XP
        blockXPRewards.put("diamond_ore", 50L);
        blockXPRewards.put("emerald_ore", 75L);
        blockXPRewards.put("deepslate_diamond_ore", 60L);
        blockXPRewards.put("deepslate_emerald_ore", 85L);
        blockXPRewards.put("ancient_debris", 100L);
        
        // Hytale specific blocks (adjust as needed)
        blockXPRewards.put("kweebec_wood", 5L);
        blockXPRewards.put("verdant_ore", 20L);
        blockXPRewards.put("mithril_ore", 40L);
        blockXPRewards.put("adamantine_ore", 80L);
    }
    
    public void setBlockXP(String blockType, long xp) {
        blockXPRewards.put(blockType.toLowerCase(), xp);
    }
    
    public void setDefaultBlockXP(long xp) {
        this.defaultBlockXP = xp;
    }
    
    public void setGlobalMiningXPMultiplier(double multiplier) {
        this.globalMiningXPMultiplier = Math.max(0.0, multiplier);
    }
    
    @Override
    public void handle(int i,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        try {
            // Skip if cancelled
            if (event.isCancelled()) return;
            
            // Get the player who broke the block
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
            Player player = store.getComponent(ref, Player.getComponentType());
            
            if (player == null) return;
            
            @SuppressWarnings("removal")
            PlayerRef playerRef = player.getPlayerRef();
            UUID playerUuid = playerRef.getUuid();
            String playerName = playerRef.getUsername();
            
            // Check cooldown to prevent XP spam
            long now = System.currentTimeMillis();
            Long lastTime = lastMineTime.get(playerUuid);
            if (lastTime != null && (now - lastTime) < MINE_COOLDOWN_MS) {
                return;
            }
            lastMineTime.put(playerUuid, now);
            
            // Get player RPG data
            PlayerRPGData rpgData = rpgManager.getPlayerData(playerUuid);
            if (rpgData == null) {
                rpgData = rpgManager.initializePlayer(playerUuid, playerName);
            }
            
            // Get block type
            String blockType = getBlockType(event);
            
            // Calculate XP reward
            long baseXP = blockXPRewards.getOrDefault(blockType.toLowerCase(), defaultBlockXP);
            
            // Apply profession/class mining bonus
            double professionMultiplier = getMiningProfessionMultiplier(rpgData);
            
            // Apply perk bonuses (mining_xp_bonus from perks like Mining Apprentice, Efficient Miner, etc.)
            double perkMultiplier = 1.0 + PerkStatApplier.getGameplayModifier(playerUuid, "mining_xp_bonus");
            
            // Calculate final XP
            long finalXP = Math.round(baseXP * professionMultiplier * perkMultiplier * globalMiningXPMultiplier);
            
            if (finalXP <= 0) return;
            
            // Track level before XP gain
            int levelBefore = rpgData.getLevel();
            
            // Award XP
            rpgData.addXP(finalXP);
            
            // Increment mining stat
            rpgData.incrementStat("blocks_mined");
            rpgData.incrementStat("mining_xp_earned", finalXP);
            
            // Check for level up
            int levelAfter = rpgData.getLevel();
            if (levelAfter > levelBefore) {
                handleLevelUp(playerUuid, playerName, rpgData, levelBefore, levelAfter);
            }
            
            LOGGER.finest(String.format("Player %s mined %s: +%d XP", playerName, blockType, finalXP));
            
        } catch (Exception e) {
            LOGGER.warning("Error processing mining XP: " + e.getMessage());
        }
    }
    
    /**
     * Get the block type from the event
     */
    private String getBlockType(BreakBlockEvent event) {
        try {
            if (event.getBlockType() != null) {
                return event.getBlockType().getId();
            }
        } catch (Exception e) {
            // Fallback
        }
        return "unknown";
    }
    
    /**
     * Get mining XP multiplier based on profession/class
     */
    private double getMiningProfessionMultiplier(PlayerRPGData rpgData) {
        String profession = rpgData.getSelectedProfession();
        if (profession == null) return 1.0;
        
        return switch (profession.toLowerCase()) {
            case "miner" -> 1.5;      // Mining specialist
            case "blacksmith" -> 1.3; // Uses ores
            case "engineer" -> 1.2;   // Utility focus
            case "crafter" -> 1.1;    // General bonus
            default -> 1.0;
        };
    }
    
    /**
     * Handle player level up
     */
    private void handleLevelUp(UUID playerUuid, String playerName, PlayerRPGData rpgData,
                               int oldLevel, int newLevel) {
        LOGGER.info(String.format("Player %s leveled up from mining! %d -> %d", playerName, oldLevel, newLevel));
        
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
