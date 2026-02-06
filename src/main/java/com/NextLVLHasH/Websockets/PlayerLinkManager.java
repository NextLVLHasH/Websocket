package com.NextLVLHasH.Websockets;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Discord <-> Hytale player linking
 * Links Discord users to Hytale players for role assignment and name display
 */
public class PlayerLinkManager {
    
    // UUID -> Discord ID mapping
    private final ConcurrentHashMap<String, String> playerLinks = new ConcurrentHashMap<>();
    
    // UUID -> Discord Username mapping (for in-game display)
    private final ConcurrentHashMap<String, String> discordUsernames = new ConcurrentHashMap<>();
    
    /**
     * Link a Hytale player to a Discord user
     */
    public void linkPlayer(String hytaleUuid, String discordUserId) {
        playerLinks.put(hytaleUuid, discordUserId);
    }
    
    /**
     * Link player with Discord username
     */
    public void linkPlayer(String hytaleUuid, String discordUserId, String discordUsername) {
        playerLinks.put(hytaleUuid, discordUserId);
        discordUsernames.put(hytaleUuid, discordUsername);
    }
    
    /**
     * Unlink a player
     */
    public void unlinkPlayer(String hytaleUuid) {
        playerLinks.remove(hytaleUuid);
        discordUsernames.remove(hytaleUuid);
    }
    
    /**
     * Get Discord ID for Hytale player
     */
    public String getDiscordId(String hytaleUuid) {
        return playerLinks.get(hytaleUuid);
    }
    
    /**
     * Get Discord username for Hytale player
     */
    public String getDiscordUsername(UUID hytaleUuid) {
        return discordUsernames.get(hytaleUuid.toString());
    }
    
    /**
     * Get Discord username for Hytale player by UUID string
     */
    public String getDiscordUsername(String hytaleUuid) {
        return discordUsernames.get(hytaleUuid);
    }
    
    /**
     * Set Discord username for linked player
     */
    public void setDiscordUsername(String hytaleUuid, String discordUsername) {
        if (isLinked(hytaleUuid)) {
            discordUsernames.put(hytaleUuid, discordUsername);
        }
    }
    
    /**
     * Check if player is linked
     */
    public boolean isLinked(UUID hytaleUuid) {
        return playerLinks.containsKey(hytaleUuid.toString());
    }
    
    /**
     * Check if player is linked by UUID string
     */
    public boolean isLinked(String hytaleUuid) {
        return playerLinks.containsKey(hytaleUuid);
    }
    
    /**
     * Get all linked players
     */
    public ConcurrentHashMap<String, String> getAllLinks() {
        return new ConcurrentHashMap<>(playerLinks);
    }
}
