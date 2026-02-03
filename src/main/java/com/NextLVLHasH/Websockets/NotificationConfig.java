package com.NextLVLHasH.Websockets;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Discord Bot notifications and chat bridge
 */
public class NotificationConfig {
    
    // Discord bot settings (replaces webhook)
    private boolean enableDiscordBot = false;
    private String discordBotToken = "";
    private String notificationChannelId = "";
    private String chatBridgeChannelId = "";
    private String linkedRoleId = "";
    
    // Reaction role settings
    private boolean enableReactionRoles = false;
    private String reactionRoleChannelId = "";
    private String reactionRoleMessageTitle = "Get your in-game roles below!";
    private String reactionRoleMessageDescription = "Click the reactions to get roles that will display in-game";
    private List<RoleReactionConfig> reactionRoles = new ArrayList<>();
    
    // Display settings
    private boolean showDiscordNameInGame = false;  // Show Discord username instead of Hytale username
    private boolean showRolePrefixInGame = true;    // Show role prefix like [Knight]
    
    // Chat bridge settings
    private boolean enableChatBridge = true;
    private boolean syncHytaleToDiscord = true;
    private boolean syncDiscordToHytale = true;
    
    // Notification toggles
    private boolean enablePlayerJoinNotifications = true;
    private boolean enablePlayerLeaveNotifications = true;
    
    // Server information (for notifications)
    private String serverName = "Hytale Server";
    private String serverAddress = "localhost:5520";
    private String serverDescription = "A Hytale Server";
    
    // WebSocket server URL for sending player count data
    private String WSServerURL = "ws://localhost:8080";
    
    public NotificationConfig() {
        // Default constructor
    }

    // Discord bot getters/setters
    public boolean isEnableDiscordBot() {
        return enableDiscordBot;
    }

    public void setEnableDiscordBot(boolean enableDiscordBot) {
        this.enableDiscordBot = enableDiscordBot;
    }

    public String getDiscordBotToken() {
        return discordBotToken;
    }

    public void setDiscordBotToken(String discordBotToken) {
        this.discordBotToken = discordBotToken;
    }

    public String getNotificationChannelId() {
        return notificationChannelId;
    }

    public void setNotificationChannelId(String notificationChannelId) {
        this.notificationChannelId = notificationChannelId;
    }

    public String getChatBridgeChannelId() {
        return chatBridgeChannelId;
    }

    public void setChatBridgeChannelId(String chatBridgeChannelId) {
        this.chatBridgeChannelId = chatBridgeChannelId;
    }

    public String getLinkedRoleId() {
        return linkedRoleId;
    }

    public void setLinkedRoleId(String linkedRoleId) {
        this.linkedRoleId = linkedRoleId;
    }

    // Chat bridge getters/setters
    public boolean isEnableChatBridge() {
        return enableChatBridge;
    }

    public void setEnableChatBridge(boolean enableChatBridge) {
        this.enableChatBridge = enableChatBridge;
    }

    public boolean isSyncHytaleToDiscord() {
        return syncHytaleToDiscord;
    }

    public void setSyncHytaleToDiscord(boolean syncHytaleToDiscord) {
        this.syncHytaleToDiscord = syncHytaleToDiscord;
    }

    public boolean isSyncDiscordToHytale() {
        return syncDiscordToHytale;
    }

    public void setSyncDiscordToHytale(boolean syncDiscordToHytale) {
        this.syncDiscordToHytale = syncDiscordToHytale;
    }

    // Notification toggles getters/setters
    public boolean isEnablePlayerJoinNotifications() {
        return enablePlayerJoinNotifications;
    }

    public void setEnablePlayerJoinNotifications(boolean enablePlayerJoinNotifications) {
        this.enablePlayerJoinNotifications = enablePlayerJoinNotifications;
    }

    public boolean isEnablePlayerLeaveNotifications() {
        return enablePlayerLeaveNotifications;
    }

    public void setEnablePlayerLeaveNotifications(boolean enablePlayerLeaveNotifications) {
        this.enablePlayerLeaveNotifications = enablePlayerLeaveNotifications;
    }

    // Server information getters/setters
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getServerDescription() {
        return serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    // Reaction role getters/setters
    public boolean isEnableReactionRoles() {
        return enableReactionRoles;
    }

    public void setEnableReactionRoles(boolean enableReactionRoles) {
        this.enableReactionRoles = enableReactionRoles;
    }

    public String getReactionRoleChannelId() {
        return reactionRoleChannelId;
    }

    public void setReactionRoleChannelId(String reactionRoleChannelId) {
        this.reactionRoleChannelId = reactionRoleChannelId;
    }

    public String getReactionRoleMessageTitle() {
        return reactionRoleMessageTitle;
    }

    public void setReactionRoleMessageTitle(String reactionRoleMessageTitle) {
        this.reactionRoleMessageTitle = reactionRoleMessageTitle;
    }

    public String getReactionRoleMessageDescription() {
        return reactionRoleMessageDescription;
    }

    public void setReactionRoleMessageDescription(String reactionRoleMessageDescription) {
        this.reactionRoleMessageDescription = reactionRoleMessageDescription;
    }

    public List<RoleReactionConfig> getReactionRoles() {
        return reactionRoles;
    }

    public void setReactionRoles(List<RoleReactionConfig> reactionRoles) {
        this.reactionRoles = reactionRoles;
    }

    // Display settings getters/setters
    public boolean isShowDiscordNameInGame() {
        return showDiscordNameInGame;
    }

    public void setShowDiscordNameInGame(boolean showDiscordNameInGame) {
        this.showDiscordNameInGame = showDiscordNameInGame;
    }

    public boolean isShowRolePrefixInGame() {
        return showRolePrefixInGame;
    }

    public void setShowRolePrefixInGame(boolean showRolePrefixInGame) {
        this.showRolePrefixInGame = showRolePrefixInGame;
    }
    
    // WebSocket server URL getters/setters
    public String getWSServerURL() {
        return WSServerURL;
    }
    
    public void setWSServerURL(String WSServerURL) {
        this.WSServerURL = WSServerURL;
    }
}
