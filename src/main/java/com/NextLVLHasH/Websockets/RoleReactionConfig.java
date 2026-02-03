package com.NextLVLHasH.Websockets;

/**
 * Configuration for individual reaction role mappings
 * Maps emoji -> Discord role -> in-game prefix
 */
public class RoleReactionConfig {
    private String emojiId;          // Discord emoji ID or unicode emoji
    private String discordRoleId;    // Discord role ID to assign
    private String roleName;         // Display name of the role
    private String inGamePrefix;     // Prefix to show in-game like "[Knight]"
    
    public RoleReactionConfig() {
        // Default constructor for JSON
    }
    
    public RoleReactionConfig(String emojiId, String discordRoleId, String roleName, String inGamePrefix) {
        this.emojiId = emojiId;
        this.discordRoleId = discordRoleId;
        this.roleName = roleName;
        this.inGamePrefix = inGamePrefix;
    }

    public String getEmojiId() {
        return emojiId;
    }

    public void setEmojiId(String emojiId) {
        this.emojiId = emojiId;
    }

    public String getDiscordRoleId() {
        return discordRoleId;
    }

    public void setDiscordRoleId(String discordRoleId) {
        this.discordRoleId = discordRoleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getInGamePrefix() {
        return inGamePrefix;
    }

    public void setInGamePrefix(String inGamePrefix) {
        this.inGamePrefix = inGamePrefix;
    }
}
