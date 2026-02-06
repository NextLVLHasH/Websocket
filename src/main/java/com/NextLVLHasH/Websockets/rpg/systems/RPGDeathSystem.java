package com.NextLVLHasH.Websockets.rpg.systems;

import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.bridge.RPGDiscordBridge;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handler class for RPG death processing.
 * 
 * This is NOT an ECS system - it's a simple handler that can be called by other systems
 * (like RPGKillSystem or KillFeedTrackingSystem) when a player death is detected.
 * 
 * This handler:
 * - Tracks player deaths for statistics
 * - Applies XP loss penalties (configurable)
 * - Resets combat effects on death
 * - Sends death events to Discord
 * - Handles respawn stat restoration
 * 
 * Usage:
 *   rpgDeathSystem.handlePlayerDeath(playerUuid, playerName, "PvP Combat");
 */
public class RPGDeathSystem {
    
    private static final Logger LOGGER = Logger.getLogger("RPGDeathSystem");
    private final RPGManager rpgManager;
    private final RPGDiscordBridge discordBridge;
    
    // XP loss on death (configurable)
    private double xpLossPercentage = 0.05; // 5% XP loss
    private boolean loseXpOnDeath = false; // Disabled by default
    
    public RPGDeathSystem(RPGManager rpgManager, RPGDiscordBridge discordBridge) {
        this.rpgManager = rpgManager;
        this.discordBridge = discordBridge;
        LOGGER.info("RPGDeathSystem initialized");
    }
    
    /**
     * Enable or disable XP loss on death.
     * @param enabled Whether XP loss is enabled
     * @param percentage The percentage of current XP to lose (0.0 to 1.0)
     */
    public void setXPLossEnabled(boolean enabled, double percentage) {
        this.loseXpOnDeath = enabled;
        this.xpLossPercentage = Math.max(0, Math.min(1.0, percentage));
    }
    
    /**
     * Check if XP loss on death is enabled.
     * @return true if XP loss is enabled
     */
    public boolean isXPLossEnabled() {
        return loseXpOnDeath;
    }
    
    /**
     * Get the current XP loss percentage.
     * @return XP loss percentage (0.0 to 1.0)
     */
    public double getXPLossPercentage() {
        return xpLossPercentage;
    }
    
    /**
     * Handle a player death event.
     * Call this method from other systems (like RPGKillSystem) when a player death is detected.
     * 
     * @param playerId The UUID of the player who died
     * @param playerName The name of the player who died
     * @param deathCause The cause of death (e.g., "PvP Combat", "Fall Damage", "Monster")
     */
    public void handlePlayerDeath(UUID playerId, String playerName, String deathCause) {
        if (playerId == null || playerName == null) {
            LOGGER.warning("handlePlayerDeath called with null playerId or playerName");
            return;
        }
        
        try {
            PlayerRPGData rpgData = rpgManager.getPlayerData(playerId);
            if (rpgData == null) {
                LOGGER.fine("No RPG data found for player: " + playerName);
                return;
            }
            
            // Track death statistics
            int currentLevel = rpgData.getLevel();
            long xpBefore = rpgData.getCurrentXP();
            
            LOGGER.info(String.format("Player %s died at level %d with %d XP (cause: %s)", 
                    playerName, currentLevel, xpBefore, deathCause != null ? deathCause : "Unknown"));
            
            // Apply XP penalty if enabled
            long xpLost = 0;
            if (loseXpOnDeath && xpBefore > 0) {
                xpLost = (long) (xpBefore * xpLossPercentage);
                rpgData.removeXP(xpLost);
                LOGGER.info(String.format("Player %s lost %d XP on death", playerName, xpLost));
            }
            
            // Clear all combat effects
            if (rpgManager.getCombatManager() != null) {
                rpgManager.getCombatManager().removeAllEffects(playerId);
            }
            
            // Reset current stats to max on respawn
            rpgData.setCurrentHealth(rpgData.getMaxHealth());
            rpgData.setCurrentMana(rpgData.getMaxMana());
            rpgData.setCurrentStamina(rpgData.getMaxStamina());
            
            // Send death notification to Discord
            if (discordBridge != null) {
                String cause = deathCause != null ? deathCause : "Unknown";
                discordBridge.onPlayerDeath(playerId, playerName, currentLevel, cause, xpLost);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error processing RPG death for " + playerName + ": " + e.getMessage());
        }
    }
    
    /**
     * Handle a player death with killer information.
     * Use this overload when the death was caused by another player (PvP).
     * 
     * @param playerId The UUID of the player who died
     * @param playerName The name of the player who died
     * @param killerId The UUID of the killer (can be null for non-PvP deaths)
     * @param killerName The name of the killer (can be null for non-PvP deaths)
     */
    public void handlePlayerDeath(UUID playerId, String playerName, UUID killerId, String killerName) {
        String deathCause;
        if (killerId != null && killerName != null) {
            deathCause = "Killed by " + killerName;
        } else {
            deathCause = "Unknown";
        }
        handlePlayerDeath(playerId, playerName, deathCause);
    }
    
    /**
     * Get the RPGManager instance.
     * @return the RPGManager
     */
    public RPGManager getRPGManager() {
        return rpgManager;
    }
    
    /**
     * Get the Discord bridge instance.
     * @return the RPGDiscordBridge (may be null)
     */
    public RPGDiscordBridge getDiscordBridge() {
        return discordBridge;
    }
}
