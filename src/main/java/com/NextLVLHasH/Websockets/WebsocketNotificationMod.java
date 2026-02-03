package com.NextLVLHasH.Websockets;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.command.NotificationReloadCommand;
import com.NextLVLHasH.Websockets.command.NotificationStatusCommand;
import com.NextLVLHasH.Websockets.command.LinkDiscordCommand;
import com.NextLVLHasH.Websockets.command.UnlinkDiscordCommand;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Discord Bot Notification Plugin
 * 
 * Features:
 * - Discord bot with rich embeds
 * - Role management
 * - Player linking system
 * - Chat bridge (coming soon - needs PlayerChatEvent)
 * 
 * Configurable via config.json in the plugin data directory
 */
public class WebsocketNotificationMod extends JavaPlugin {
    
    @SuppressWarnings("null")
    public WebsocketNotificationMod(JavaPluginInit init) {
        super(init);
    }
    
    private DiscordBot discordBot;
    private NotificationConfig config;
    private ConfigManager configManager;
    private PlayerLinkManager linkManager;
    private LinkVerificationManager verificationManager;
    private PlayerCountWebSocketClient wsClient;
    
    // Track online players for Discord notifications and chat broadcast
    // Maps UUID -> PlayerRef for sending messages to players
    private final java.util.Map<String, PlayerRef> onlinePlayers = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track recently sent notifications to prevent duplicates
    private final Set<String> recentNotifications = ConcurrentHashMap.newKeySet();
    private static final long DUPLICATE_WINDOW_MS = 2000; // 2 second window

    @Override
    protected void setup() {
        getLogger().atInfo().log("Discord Bot Notification Mod - Initializing...");
        
        // Initialize config manager and load configuration
        configManager = new ConfigManager(getDataDirectory());
        config = configManager.loadConfig();
        linkManager = new PlayerLinkManager();
        verificationManager = new LinkVerificationManager();
        
        getLogger().atInfo().log("Configuration loaded from: " + configManager.getConfigPath());
        
        // Initialize WebSocket client for player count data
        if (config.getWsServerURL() != null && !config.getWsServerURL().isEmpty()) {
            wsClient = new PlayerCountWebSocketClient(config.getWsServerURL());
            wsClient.connect().thenRun(() -> getLogger().atInfo().log("WebSocket client connected to: " + config.getWsServerURL()));
        } else {
            getLogger().atWarning().log("WebSocket server URL not configured, player count data will not be sent");
        }
        
        // Initialize Discord bot if enabled
        if (config.isEnableDiscordBot()) {
            if (config.getDiscordBotToken() != null && !config.getDiscordBotToken().isEmpty()) {
                discordBot = new DiscordBot(
                        config.getDiscordBotToken(),
                        config.getServerName(),
                        config.getServerAddress(),
                        config.getServerDescription(),
                        config.getNotificationChannelId(),
                        config.getChatBridgeChannelId(),
                        config.getLinkedRoleId(),
                        config.isEnableReactionRoles(),
                        config.getReactionRoleChannelId(),
                        config.getReactionRoleMessageTitle(),
                        config.getReactionRoleMessageDescription(),
                        config.getReactionRoles()
                );
                
                // Set verification manager for link verification
                discordBot.setVerificationManager(verificationManager);
                
                // Set up a chat callback for Discord -> Hytale messages
                if (config.isEnableChatBridge() && config.isSyncDiscordToHytale()) {
                    discordBot.setChatCallback((discordUser, message) -> {
                        // Broadcast Discord message to all online Hytale players
                        // Using plain text format since Hytale doesn't use Minecraft color codes
                        String formattedMessage = "[Discord] " + discordUser + ": " + message;
                        getLogger().atInfo().log("[Discord -> Hytale] Broadcasting: " + discordUser + ": " + message);
                        
                        // Send to all online players
                        int sentCount = 0;
                        for (PlayerRef playerRef : onlinePlayers.values()) {
                            try {
                                playerRef.sendMessage(Message.raw(formattedMessage));
                                sentCount++;
                            } catch (Exception e) {
                                getLogger().atWarning().log("Failed to send Discord message to player: " + e.getMessage());
                            }
                        }
                        getLogger().atInfo().log("[Discord -> Hytale] Message sent to " + sentCount + " players");
                    });
                }
                
                // Start bot async
                discordBot.start().thenRun(() -> getLogger().atInfo().log("Discord bot connected successfully!"));
                
            } else {
                getLogger().atWarning().log("Discord bot enabled but token not configured!");
            }
        }

        // Register player join event
        if (config.isEnablePlayerJoinNotifications()) {
            registerPlayerJoinEvent();
        }

        // Register player leave event
        if (config.isEnablePlayerLeaveNotifications()) {
            registerPlayerLeaveEvent();
        }
        
        // Register chat bridge (Hytale -> Discord)
        if (config.isEnableChatBridge() && config.isSyncHytaleToDiscord()) {
            registerChatBridge();
        }
        
        // Register commands
        getCommandRegistry().registerCommand(new NotificationReloadCommand(this));
        getCommandRegistry().registerCommand(new NotificationStatusCommand(this));
        getCommandRegistry().registerCommand(new LinkDiscordCommand(this));
        getCommandRegistry().registerCommand(new UnlinkDiscordCommand(this));

        getLogger().atInfo().log("Discord Bot Notification Mod - Setup complete!");
        getLogger().atInfo().log("Discord Bot: " + (config.isEnableDiscordBot() ? "ENABLED" : "DISABLED"));
    }
    
    @Override
    protected void start() {
        // Send server start notification
        if (discordBot != null && discordBot.isReady()) {
            discordBot.sendServerStartNotification(onlinePlayers.size());
        }
        
        // Send initial player count pulse to WebSocket server
        if (wsClient != null) {
            wsClient.sendImmediatePulse(onlinePlayers.size(), onlinePlayers.size());
        }
        
        getLogger().atInfo().log("Server started - Discord notification sent");
    }

    /**
     * Register handler for player join events
     * Uses PlayerReadyEvent as recommended by Hytale docs
     */
    private void registerPlayerJoinEvent() {
        getEventRegistry().registerGlobal(
            PlayerReadyEvent.class, 
            event -> {
                try {
                    var player = event.getPlayer();
                    String playerName = player.getDisplayName();
                    @SuppressWarnings("removal")
                    String playerUuid = player.getPlayerRef().getUuid().toString();
                    
                    // Create unique key for this notification
                    String notificationKey = "JOIN:" + playerUuid + ":" + System.currentTimeMillis() / DUPLICATE_WINDOW_MS;
                    
                    // Check if we already sent this notification recently
                    if (!recentNotifications.add(notificationKey)) {
                        getLogger().atInfo().log("Duplicate join notification prevented for: " + playerName);
                        return;
                    }
                    
                    // Clean up old entries (keep last 100)
                    if (recentNotifications.size() > 100) {
                        recentNotifications.clear();
                    }
                    
                    getLogger().atInfo().log("Player joined: " + playerName + " (" + playerUuid + ")");
                    
                    // Track player with their PlayerRef for message broadcasting
                    @SuppressWarnings("removal")
                    PlayerRef playerRef = player.getPlayerRef();
                    onlinePlayers.put(playerUuid, playerRef);
                    
                    // Send Discord notification (only once)
                    if (discordBot != null && discordBot.isReady()) {
                        discordBot.sendPlayerJoinNotification(playerName, onlinePlayers.size());
                        
                        // Assign linked role if player is linked
                        if (linkManager.isLinked(playerUuid)) {
                            String discordId = linkManager.getDiscordId(playerUuid);
                            discordBot.assignLinkedRole(discordId);
                        }
                    }
                    
                    // Update player count for WebSocket (will be sent on next 5-min pulse)
                    if (wsClient != null) {
                        wsClient.updatePlayerCount(onlinePlayers.size(), onlinePlayers.size());
                    }
                    
                } catch (Exception e) {
                    getLogger().atSevere().log("Error handling player join event: " + e.getMessage());
                }
            }
        );
        
        getLogger().atInfo().log("Player join event handler registered");
    }

    /**
     * Register handler for player leave events
     * Uses PlayerDisconnectEvent as recommended by Hytale docs
     */
    private void registerPlayerLeaveEvent() {
        getEventRegistry().register(
            PlayerDisconnectEvent.class,
            event -> {
                try {
                    var playerRef = event.getPlayerRef();
                    String playerName = playerRef.getUsername();
                    String playerUuid = playerRef.getUuid().toString();
                    
                    // Create unique key for this notification
                    String notificationKey = "LEAVE:" + playerUuid + ":" + System.currentTimeMillis() / DUPLICATE_WINDOW_MS;
                    
                    // Check if we already sent this notification recently
                    if (!recentNotifications.add(notificationKey)) {
                        getLogger().atInfo().log("Duplicate leave notification prevented for: " + playerName);
                        return;
                    }
                    
                    // Remove player from tracking
                    onlinePlayers.remove(playerUuid);
                    
                    // Clean up old entries (keep last 100)
                    if (recentNotifications.size() > 100) {
                        recentNotifications.clear();
                    }
                    
                    getLogger().atInfo().log("Player left: " + playerName + " (" + playerUuid + ")");
                    
                    // Send Discord notification
                    if (discordBot != null && discordBot.isReady()) {
                        discordBot.sendPlayerLeaveNotification(playerName, onlinePlayers.size());
                    }
                    
                    // Update player count for WebSocket (will be sent on next 5-min pulse)
                    if (wsClient != null) {
                        wsClient.updatePlayerCount(onlinePlayers.size(), onlinePlayers.size());
                    }
                    
                } catch (Exception e) {
                    getLogger().atSevere().log("Error handling player leave event: " + e.getMessage());
                }
            }
        );
        
        getLogger().atInfo().log("Player leave event handler registered");
    }
    
    /**
     * Register handler for chat bridge (Hytale -> Discord)
     * Uses PlayerChatEvent to capture and forward messages to Discord
     * Note: PlayerChatEvent is an async event with String key type, requires registerGlobal()
     */
    private void registerChatBridge() {
        getEventRegistry().registerGlobal(
            PlayerChatEvent.class,
            event -> {
                try {
                    // Get sender info
                    var sender = event.getSender();
                    String playerName = sender != null ? sender.getUsername() : "Unknown";
                    String playerUuid = sender != null ? sender.getUuid().toString() : "";
                    String message = event.getContent();
                    
                    // Check if this is a verification code (6 digits) - handle separately
                    if (message.matches("\\d{6}") && verificationManager != null) {
                        handleVerificationCode(playerUuid, playerName, message);
                        return;
                    }
                    
                    // Check if player has linked role (is verified) before allowing chat to Discord
                    if (verificationManager == null || !verificationManager.isVerified(playerUuid)) {
                        // Player not verified - don't send to Discord
                        return;
                    }
                    
                    // Get role prefix for verified players
                    String rolePrefix = "";
                    if (discordBot != null) {
                        rolePrefix = discordBot.getVerifiedPlayerRolePrefix(playerUuid);
                    }
                    
                    // Format display name with role prefix
                    String displayName = rolePrefix.isEmpty() ? playerName : rolePrefix + " " + playerName;
                    
                    getLogger().atInfo().log("[Hytale -> Discord] " + displayName + ": " + message);
                    
                    // Send to Discord with role prefix
                    if (discordBot != null && discordBot.isReady()) {
                        discordBot.sendHytaleChatToDiscord(displayName, message);
                    }
                    
                } catch (Exception e) {
                    getLogger().atSevere().log("Error handling chat event: " + e.getMessage());
                }
            }
        );
        
        getLogger().atInfo().log("Chat bridge (Hytale -> Discord) registered successfully!");
    }
    
    /**
     * Handle verification code typed in game chat
     */
    @SuppressWarnings("null")
    private void handleVerificationCode(String playerUuid, String playerName, String code) {
        if (verificationManager == null) return;
        
        // Check if there's a pending verification for this player
        LinkVerificationManager.VerificationResult result = 
            verificationManager.verifyCodeByPlayer(playerUuid, code);
        
        if (result.success) {
            // Find the player and send success message
            PlayerRef playerRef = onlinePlayers.get(playerUuid);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw(result.message));
            }
            
            // Assign linked role
            if (discordBot != null && discordBot.isReady()) {
                String discordId = verificationManager.getDiscordIdByHytaleUuid(playerUuid);
                if (discordId != null) {
                    discordBot.assignLinkedRole(discordId);
                }
            }
            
            getLogger().atInfo().log("Player " + playerName + " verified via in-game code");
        } else {
            // Send error message to player
            PlayerRef playerRef = onlinePlayers.get(playerUuid);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw(result.message));
            }
        }
    }
    
    @Override
    protected void shutdown() {
        getLogger().atInfo().log("Discord Bot Notification Mod - Shutting down...");
        
        // Disconnect WebSocket client
        if (wsClient != null) {
            wsClient.disconnect();
        }
        
        // Shutdown verification manager
        if (verificationManager != null) {
            verificationManager.shutdown();
        }
        
        // Send server stop notification
        if (discordBot != null && discordBot.isReady()) {
            discordBot.sendServerStopNotification(onlinePlayers.size());
            
            // Give time for message to send
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            discordBot.shutdown();
        }
        
        getLogger().atInfo().log("Discord Bot Notification Mod - Shutdown complete");
    }

    /**
     * Get the Discord bot
     * @return DiscordBot instance or null if not enabled
     */
    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    /**
     * Get the configuration
     * @return NotificationConfig instance
     */
    public NotificationConfig getConfig() {
        return config;
    }
    
    /**
     * Get the player link manager
     * @return PlayerLinkManager instance
     */
    public PlayerLinkManager getLinkManager() {
        return linkManager;
    }
    
    /**
     * Get the verification manager
     * @return LinkVerificationManager instance
     */
    public LinkVerificationManager getVerificationManager() {
        return verificationManager;
    }
    
    /**
     * Reload configuration from disk
     */
    public void reloadConfiguration() {
        getLogger().atInfo().log("Reloading configuration...");
        config = configManager.reloadConfig();
        getLogger().atInfo().log("Configuration reloaded successfully");
    }
    
}
