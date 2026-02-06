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
    private boolean showRoleColorInGame = true;     // Apply Discord role color to in-game chat
    
    // Chat bridge settings
    private boolean enableChatBridge = true;
    private boolean syncHytaleToDiscord = true;
    private boolean syncDiscordToHytale = true;
    
    // Webhook settings for chat messages (with player avatars)
    private boolean useChatWebhook = false;         // Use webhook instead of bot for chat (enables avatars)
    private String chatWebhookUrl = "";             // Webhook URL for chat messages
    private String playerAvatarUrlTemplate = "https://mc-heads.net/avatar/{uuid}/64"; // Player avatar URL template
    
    // Message formatting settings
    private String hytaleToDiscordFormat = "**[Hytale]** `{player}`: {message}"; // Format for Hytale->Discord
    private String discordToHytaleFormat = "[Discord] {role}{name}: {message}"; // Format for Discord->Hytale
    private String inGameChatFormat = "{rolePrefix} {name}: {message}"; // Format for in-game chat
    
    // Message filtering settings
    private boolean filterDiscordMentions = true;   // Convert @mentions to plain text
    private boolean filterDiscordFormatting = false; // Remove **bold**, *italic*, etc.
    private boolean filterUrls = false;             // Remove URLs from messages
    private boolean filterCustomEmojis = true;      // Convert custom Discord emojis to :name:
    
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
    
    public boolean isShowRoleColorInGame() {
        return showRoleColorInGame;
    }
    
    public void setShowRoleColorInGame(boolean showRoleColorInGame) {
        this.showRoleColorInGame = showRoleColorInGame;
    }
    
    // Webhook settings getters/setters
    public boolean isUseChatWebhook() {
        return useChatWebhook;
    }
    
    public void setUseChatWebhook(boolean useChatWebhook) {
        this.useChatWebhook = useChatWebhook;
    }
    
    public String getChatWebhookUrl() {
        return chatWebhookUrl;
    }
    
    public void setChatWebhookUrl(String chatWebhookUrl) {
        this.chatWebhookUrl = chatWebhookUrl;
    }
    
    public String getPlayerAvatarUrlTemplate() {
        return playerAvatarUrlTemplate;
    }
    
    public void setPlayerAvatarUrlTemplate(String playerAvatarUrlTemplate) {
        this.playerAvatarUrlTemplate = playerAvatarUrlTemplate;
    }
    
    // Message format getters/setters
    public String getHytaleToDiscordFormat() {
        return hytaleToDiscordFormat;
    }
    
    public void setHytaleToDiscordFormat(String hytaleToDiscordFormat) {
        this.hytaleToDiscordFormat = hytaleToDiscordFormat;
    }
    
    public String getDiscordToHytaleFormat() {
        return discordToHytaleFormat;
    }
    
    public void setDiscordToHytaleFormat(String discordToHytaleFormat) {
        this.discordToHytaleFormat = discordToHytaleFormat;
    }
    
    public String getInGameChatFormat() {
        return inGameChatFormat;
    }
    
    public void setInGameChatFormat(String inGameChatFormat) {
        this.inGameChatFormat = inGameChatFormat;
    }
    
    // Message filtering getters/setters
    public boolean isFilterDiscordMentions() {
        return filterDiscordMentions;
    }
    
    public void setFilterDiscordMentions(boolean filterDiscordMentions) {
        this.filterDiscordMentions = filterDiscordMentions;
    }
    
    public boolean isFilterDiscordFormatting() {
        return filterDiscordFormatting;
    }
    
    public void setFilterDiscordFormatting(boolean filterDiscordFormatting) {
        this.filterDiscordFormatting = filterDiscordFormatting;
    }
    
    public boolean isFilterUrls() {
        return filterUrls;
    }
    
    public void setFilterUrls(boolean filterUrls) {
        this.filterUrls = filterUrls;
    }
    
    public boolean isFilterCustomEmojis() {
        return filterCustomEmojis;
    }
    
    public void setFilterCustomEmojis(boolean filterCustomEmojis) {
        this.filterCustomEmojis = filterCustomEmojis;
    }
    
    // WebSocket server URL getters/setters
    public String getWSServerURL() {
        return WSServerURL;
    }
    
    public void setWSServerURL(String WSServerURL) {
        this.WSServerURL = WSServerURL;
    }
}
