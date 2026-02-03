package com.NextLVLHasH.Websockets;

/**
 * Utility class for formatting player display names with Discord info
 * Handles showing Discord username and role prefixes in-game
 */
public class DiscordDisplayHelper {
    
    private final DiscordBot discordBot;
    private final PlayerLinkManager linkManager;
    private final NotificationConfig config;
    
    public DiscordDisplayHelper(DiscordBot discordBot, PlayerLinkManager linkManager, NotificationConfig config) {
        this.discordBot = discordBot;
        this.linkManager = linkManager;
        this.config = config;
    }
    
    /**
     * Get formatted display name for player
     * Format: [RolePrefix] DiscordName or [RolePrefix] HytaleName
     * 
     * @param hytaleUuid Player's Hytale UUID
     * @param hytaleUsername Player's Hytale username (fallback)
     * @return Formatted display name
     */
    public String getFormattedPlayerName(String hytaleUuid, String hytaleUsername) {
        // Check if player is linked
        if (!linkManager.isLinked(hytaleUuid)) {
            return hytaleUsername;
        }
        
        String discordUserId = linkManager.getDiscordId(hytaleUuid);
        if (discordUserId == null) {
            return hytaleUsername;
        }
        
        StringBuilder displayName = new StringBuilder();
        
        // Add role prefix if enabled
        if (config.isShowRolePrefixInGame() && discordBot != null) {
            String rolePrefix = discordBot.getPlayerRolePrefix(discordUserId);
            if (rolePrefix != null && !rolePrefix.isEmpty()) {
                displayName.append(rolePrefix).append(" ");
            }
        }
        
        // Add username (Discord or Hytale based on config)
        if (config.isShowDiscordNameInGame() && discordBot != null) {
            String discordUsername = linkManager.getDiscordUsername(hytaleUuid);
            
            // Fetch from Discord if not cached
            if (discordUsername == null) {
                discordUsername = discordBot.getDiscordUsername(discordUserId);
                if (discordUsername != null) {
                    linkManager.setDiscordUsername(hytaleUuid, discordUsername);
                }
            }
            
            displayName.append(discordUsername != null ? discordUsername : hytaleUsername);
        } else {
            displayName.append(hytaleUsername);
        }
        
        return displayName.toString();
    }
    
    /**
     * Get just the role prefix for a player
     */
    public String getRolePrefix(String hytaleUuid) {
        if (!linkManager.isLinked(hytaleUuid) || discordBot == null) {
            return "";
        }
        
        String discordUserId = linkManager.getDiscordId(hytaleUuid);
        if (discordUserId == null) {
            return "";
        }
        
        return discordBot.getPlayerRolePrefix(discordUserId);
    }
    
    /**
     * Get Discord username for a player
     */
    public String getDiscordUsername(String hytaleUuid) {
        if (!linkManager.isLinked(hytaleUuid)) {
            return null;
        }
        
        String discordUserId = linkManager.getDiscordId(hytaleUuid);
        if (discordUserId == null) {
            return null;
        }
        
        String cached = linkManager.getDiscordUsername(hytaleUuid);
        if (cached != null) {
            return cached;
        }
        
        if (discordBot != null) {
            String fetched = discordBot.getDiscordUsername(discordUserId);
            if (fetched != null) {
                linkManager.setDiscordUsername(hytaleUuid, fetched);
            }
            return fetched;
        }
        
        return null;
    }
}
