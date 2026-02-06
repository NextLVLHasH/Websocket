package com.NextLVLHasH.Websockets;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.command.NotificationReloadCommand;
import com.NextLVLHasH.Websockets.command.NotificationStatusCommand;
import com.NextLVLHasH.Websockets.command.LinkDiscordCommand;
import com.NextLVLHasH.Websockets.command.UnlinkDiscordCommand;
import com.NextLVLHasH.Websockets.command.ChatSettingsCommand;
import com.NextLVLHasH.Websockets.command.PrivateMessageCommand;
import com.NextLVLHasH.Websockets.command.StatsCommand;
import com.NextLVLHasH.Websockets.command.RPGCommand;
import com.NextLVLHasH.Websockets.systems.BlockBreakTrackingSystem;
import com.NextLVLHasH.Websockets.systems.KillFeedTrackingSystem;
import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.ui.UIManager;
import com.NextLVLHasH.Websockets.rpg.bridge.RPGDiscordBridge;
import com.NextLVLHasH.Websockets.rpg.systems.RPGDamageSystem;
import com.NextLVLHasH.Websockets.rpg.systems.RPGDeathSystem;
import com.NextLVLHasH.Websockets.rpg.systems.RPGKillSystem;
import com.NextLVLHasH.Websockets.rpg.systems.RPGMiningSystem;
import com.NextLVLHasH.Websockets.rpg.systems.RPGBuildSystem;
import com.NextLVLHasH.Websockets.rpg.skills.PerkManager;
import com.NextLVLHasH.Websockets.party.PartyManager;
import com.NextLVLHasH.Websockets.ui.PartyHUD;
import java.io.File;

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
    public WebsocketNotificationMod(JavaPluginInit init) {
        super(init);
    }
    
    private DiscordBot discordBot;
    private NotificationConfig config;
    private ConfigManager configManager;
    private PlayerLinkManager linkManager;
    private LinkVerificationManager verificationManager;
    private PlayerCountWebSocketClient wsClient;
    private ChatSettingsManager chatSettingsManager;
    private PrivateMessageLogger privateMessageLogger;
    private PlayerStatisticsManager statisticsManager;
    private MessageFormatter messageFormatter; // Cached formatter for performance
    
    // RPG System
    private RPGManager rpgManager;
    private RPGDiscordBridge rpgBridge;
    
    // Track online players for Discord notifications and chat broadcast
    // Maps UUID -> PlayerRef for sending messages to players
    private final java.util.Map<String, PlayerRef> onlinePlayers = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track recently sent notifications to prevent duplicates
    private final Set<String> recentNotifications = ConcurrentHashMap.newKeySet();
    private static final long DUPLICATE_WINDOW_MS = 2000; // 2 second window
    
    // Cache for verified player status to avoid repeated lookups
    private final java.util.Map<String, Boolean> verifiedCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long VERIFY_CACHE_TTL_MS = 30000; // 30 second cache
    private final java.util.Map<String, Long> verifyCacheTime = new java.util.concurrent.ConcurrentHashMap<>();


    @Override
    protected void setup() {
        getLogger().atInfo().log("Discord Bot Notification Mod - Initializing...");
        
        // Initialize config manager and load configuration
        configManager = new ConfigManager(getDataDirectory());
        config = configManager.loadConfig();
        linkManager = new PlayerLinkManager();
        verificationManager = new LinkVerificationManager(getDataDirectory());
        chatSettingsManager = new ChatSettingsManager();
        statisticsManager = new PlayerStatisticsManager(getDataDirectory());
        messageFormatter = new MessageFormatter(config); // Cache the formatter
        
        getLogger().atInfo().log("Configuration loaded from: " + configManager.getConfigPath());
        
        // Initialize WebSocket client for player count data
        if (config.getWSServerURL() != null && !config.getWSServerURL().isEmpty()) {
            wsClient = new PlayerCountWebSocketClient(config.getWSServerURL());
            wsClient.connect().thenRun(() -> getLogger().atInfo().log("WebSocket client connected to: " + config.getWSServerURL()));
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
                        config.getReactionRoles(),
                        config.isUseChatWebhook(),
                        config.getChatWebhookUrl()
                );
                
                // Set verification manager for link verification
                discordBot.setVerificationManager(verificationManager);
                
                // Set statistics manager for /stats command
                discordBot.setStatisticsManager(statisticsManager);
                
                // Set message formatter for customizable formatting
                MessageFormatter messageFormatter = new MessageFormatter(config);
                discordBot.setMessageFormatter(messageFormatter);
                
                // Set up extended chat callback for Discord -> Hytale messages (with colored display)
                if (config.isEnableChatBridge() && config.isSyncDiscordToHytale()) {
                    discordBot.setExtendedChatCallback((discordUser, message, roleName, roleColorHex) -> {
                        // Filter Discord message before displaying in Hytale
                        String filteredMessage = messageFormatter.filterDiscordMessage(message);
                        
                        getLogger().atInfo().log("[Discord -> Hytale] Broadcasting: " + discordUser + ": " + filteredMessage + " (role: " + roleName + ", color: " + roleColorHex + ")");
                        
                        // Build colored Message using Hytale's Message API
                        Message formattedMessage = messageFormatter.buildDiscordToHytaleMessage(
                            "[Discord]",
                            null, // Default Discord blurple for label
                            roleName,
                            roleColorHex,
                            discordUser,
                            roleColorHex, // Username same color as role
                            filteredMessage,
                            null // Default white for content
                        );
                        
                        // Send to all online players
                        int sentCount = 0;
                        for (PlayerRef playerRef : onlinePlayers.values()) {
                            try {
                                playerRef.sendMessage(formattedMessage);
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
        
        // Register chat display modifier (shows Discord names/prefixes for verified players)
        // This runs when Discord bot is enabled, regardless of chat bridge settings
        if (config.isEnableDiscordBot()) {
            registerChatDisplayModifier();
        }
        
        // Register commands
        getCommandRegistry().registerCommand(new NotificationReloadCommand(this));
        getCommandRegistry().registerCommand(new NotificationStatusCommand(this));
        getCommandRegistry().registerCommand(new LinkDiscordCommand(this));
        getCommandRegistry().registerCommand(new UnlinkDiscordCommand(this));
        // Register chat settings command (/chat)
        getCommandRegistry().registerCommand(new ChatSettingsCommand(this));
        // Register private message command (/msg)
        getCommandRegistry().registerCommand(new PrivateMessageCommand(this));
        // Register stats command (/stats)
        getCommandRegistry().registerCommand(new StatsCommand(this));
        // Register RPG command with subcommands (/rpg sheet, /rpg skills, /rpg create, /rpg log, /rpg info)
        getCommandRegistry().registerCommand(new RPGCommand(this));
        // Register party command
        getCommandRegistry().registerCommand(new com.NextLVLHasH.Websockets.command.PartyCommand(this));
        
        // Register ECS systems for statistics tracking
        // Tracks blocks broken and mobs killed
        if (statisticsManager != null) {
            getEntityStoreRegistry().registerSystem(new BlockBreakTrackingSystem(statisticsManager));
            getEntityStoreRegistry().registerSystem(new KillFeedTrackingSystem(statisticsManager));
            getLogger().atInfo().log("Registered block break and kill feed tracking systems");
        }
        
        // Initialize RPG System
        initializeRPGSystem();
        
        // Initialize private message logger
        privateMessageLogger = new PrivateMessageLogger(getDataDirectory());

        // Initialize PartyManager and wire HUD supplier
        try {
            PartyManager.init(new File("parties.json"));
            PartyHUD.setOnlinePlayersSupplier(() -> onlinePlayers);
            getLogger().atInfo().log("PartyManager initialized and PartyHUD supplier set");
        } catch (Exception e) {
            getLogger().atWarning().log("Failed to initialize PartyManager: " + e.getMessage());
        }

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
     * Initialize the RPG System with Hytale integration and Discord bridge
     */
    private void initializeRPGSystem() {
        try {
            getLogger().atInfo().log("Initializing RPG System...");
            
            // Initialize RPG Manager with data directory for persistence
            rpgManager = RPGManager.getInstance();
            rpgManager.initialize(getDataDirectory());
            
            // Create Discord bridge for RPG events
            rpgBridge = new RPGDiscordBridge();
            
            // Set up Discord bridge callback to broadcast to Hytale
            rpgBridge.setHytaleMessageCallback(message -> {
                // Broadcast RPG messages to all online players
                Message hytaleMessage = Message.raw(message);
                for (PlayerRef playerRef : onlinePlayers.values()) {
                    try {
                        playerRef.sendMessage(hytaleMessage);
                    } catch (Exception e) {
                        getLogger().atWarning().log("Failed to send RPG message to player: " + e.getMessage());
                    }
                }
            });
            
            // Connect RPG manager to Discord bot
            if (discordBot != null) {
                discordBot.setRPGManager(rpgManager);
                rpgBridge = discordBot.getRPGBridge();
                getLogger().atInfo().log("RPG System connected to Discord bot");
            }
            
            // Register RPG ECS Systems for Hytale combat integration
            // These hook into Hytale's DamageModule events
            RPGDamageSystem damageSystem = new RPGDamageSystem(rpgManager, rpgBridge);
            damageSystem.register();
            getEntityStoreRegistry().registerSystem(new RPGKillSystem(rpgManager, rpgBridge));
            
            // Register XP systems for mining and building (Utilities category)
            getEntityStoreRegistry().registerSystem(new RPGMiningSystem(rpgManager, rpgBridge));
            getEntityStoreRegistry().registerSystem(new RPGBuildSystem(rpgManager, rpgBridge));
            
            // RPGDeathSystem is a handler class (not an ECS system)
            // It can be called by other systems when a player death is detected
            RPGDeathSystem deathSystem = new RPGDeathSystem(rpgManager, rpgBridge);
            rpgManager.setDeathSystem(deathSystem);
            
            // Initialize Perk System with JSON storage
            PerkManager perkManager = PerkManager.getInstance();
            perkManager.initialize(getDataDirectory());
            getLogger().atInfo().log("Perk System initialized with " + perkManager.getAllPerks().size() + " perks");
            
            getLogger().atInfo().log("RPG Systems registered (Kill, Mining, Build ECS) + Death handler initialized");
            getLogger().atInfo().log("RPG System initialized successfully!");
            
        } catch (Exception e) {
            getLogger().atSevere().log("Failed to initialize RPG System: " + e.getMessage());
            e.printStackTrace();
        }
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
                        return; // Silently skip duplicates
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
                    
                    // Track player statistics (playtime)
                    if (statisticsManager != null) {
                        statisticsManager.onPlayerJoin(playerUuid, playerName);
                    }
                    
                    // Initialize or load RPG data for player
                    if (rpgManager != null) {
                        java.util.UUID uuid = java.util.UUID.fromString(playerUuid);
                        PlayerRPGData rpgData = rpgManager.loadOrInitializePlayer(uuid, playerName);
                        
                        // Auto-prompt character creation if player hasn't created a character yet
                        if (rpgData != null && (!rpgData.isCharacterCreated() || 
                            rpgData.getSelectedClass() == null || rpgData.getSelectedRace() == null)) {
                            // Small delay to ensure player is fully loaded before showing UI
                            java.util.concurrent.CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                                try {
                                    UIManager.getInstance().openCharacterCreation(uuid);
                                    getLogger().atInfo().log("Opened character creation for new player: " + playerName);
                                } catch (Exception e) {
                                    getLogger().atWarning().log("Failed to open character creation UI: " + e.getMessage());
                                }
                            });
                        }
                    }
                    
                    // Send Discord notification (only once)
                    if (discordBot != null && discordBot.isReady()) {
                        discordBot.sendPlayerJoinNotification(playerName, onlinePlayers.size());
                        
                        // Assign linked role if player is linked
                        if (linkManager.isLinked(playerUuid)) {
                            String discordId = linkManager.getDiscordId(playerUuid);
                            discordBot.assignLinkedRole(discordId);
                        }
                    }
                    
                    // Send playerJoin event to WebSocket server immediately
                    if (wsClient != null) {
                        wsClient.updatePlayerNames(getOnlinePlayerNames());
                        wsClient.sendPlayerJoin(playerName, onlinePlayers.size(), onlinePlayers.size());
                    }
                    
                    // Apply saved chat settings (hide chat if configured)
                    try {
                        ChatSettingsManager.Settings chatSettings = chatSettingsManager.getSettings(playerUuid);
                        if (chatSettings.hidden) {
                            player.getHudManager().hideHudComponents(
                                playerRef, 
                                com.hypixel.hytale.protocol.packets.interface_.HudComponent.Chat
                            );
                        }
                    } catch (Exception chatEx) {
                        // Silently ignore chat settings errors
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
                        return; // Silently skip duplicates
                    }
                    
                    // Remove player from tracking
                    onlinePlayers.remove(playerUuid);
                    
                    // Track player statistics (end session, save playtime)
                    if (statisticsManager != null) {
                        statisticsManager.onPlayerLeave(playerUuid);
                    }
                    
                    // Save RPG data for player
                    if (rpgManager != null) {
                        java.util.UUID uuid = java.util.UUID.fromString(playerUuid);
                        rpgManager.savePlayerData(uuid);
                    }
                    
                    // Clean up old entries (keep last 100)
                    if (recentNotifications.size() > 100) {
                        recentNotifications.clear();
                    }
                    
                    // Send Discord notification
                    if (discordBot != null && discordBot.isReady()) {
                        discordBot.sendPlayerLeaveNotification(playerName, onlinePlayers.size());
                    }
                    
                    // Send playerLeave event to WebSocket server immediately
                    if (wsClient != null) {
                        wsClient.updatePlayerNames(getOnlinePlayerNames());
                        wsClient.sendPlayerLeave(playerName, onlinePlayers.size(), onlinePlayers.size());
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
    /**
     * Register chat display modifier to show Discord names and role prefixes
     * This is separate from chat bridge sync - it modifies in-game chat display
     */

    private void registerChatDisplayModifier() {
        // Register at EARLY priority so we can cancel the default broadcast and replace sender name
        getEventRegistry().registerGlobal(
            EventPriority.EARLY,
            PlayerChatEvent.class,
            (PlayerChatEvent event) -> {
                try {
                    // Get sender info
                    var sender = event.getSender();
                    if (sender == null) return;
                    
                    String playerName = sender.getUsername();
                    String playerUuid = sender.getUuid().toString();
                    String message = event.getContent();

                    // If message is a 6-digit verification code, handle and suppress broadcast
                    if (message != null && message.length() == 6 && message.matches("\\d{6}") && verificationManager != null) {
                        event.setCancelled(true);
                        handleVerificationCode(playerUuid, playerName, message);
                        return;
                    }

                    // Use cached verification check for performance
                    boolean isVerified = isVerifiedCached(playerUuid);
                    
                    // Determine display name: use Discord name + role prefix for verified players
                    String displayName = playerName;
                    String rolePrefix = "";
                    String roleColorHex = null;
                    
                    if (isVerified && discordBot != null) {
                        String discordName = discordBot.getVerifiedPlayerDiscordNickname(playerUuid);
                        rolePrefix = discordBot.getVerifiedPlayerRolePrefix(playerUuid);
                        
                        if (discordName != null && !discordName.isEmpty()) {
                            displayName = discordName;
                        }
                        if (config.isShowRoleColorInGame()) {
                            roleColorHex = discordBot.getVerifiedPlayerRoleColor(playerUuid);
                        }
                    }

                    // Cancel the original chat event so default sender name isn't used
                    event.setCancelled(true);

                    // Build colored message using cached MessageFormatter
                    Message formattedMessage;
                    
                    if (!rolePrefix.isEmpty() && config.isShowRolePrefixInGame()) {
                        formattedMessage = messageFormatter.buildInGameChatMessage(
                            rolePrefix, roleColorHex, displayName, roleColorHex, message, null
                        );
                    } else {
                        formattedMessage = messageFormatter.buildInGameChatMessage(
                            null, null, displayName, roleColorHex, message, null
                        );
                    }
                    
                    // Broadcast to all online players
                    for (PlayerRef playerRef : onlinePlayers.values()) {
                        try {
                            playerRef.sendMessage(formattedMessage);
                        } catch (Exception e) {
                            // Silently ignore individual send failures
                        }
                    }

                    // Track message sent in statistics
                    if (statisticsManager != null) {
                        statisticsManager.onPlayerChat(playerUuid);
                    }

                    // Forward to Discord only if chat bridge is enabled and player is verified
                    if (config.isEnableChatBridge() && config.isSyncHytaleToDiscord() && isVerified) {
                        if (discordBot != null && discordBot.isReady()) {
                            discordBot.sendHytaleChatToDiscord(displayName, playerUuid, message);
                        }
                    }

                } catch (Exception e) {
                    getLogger().atSevere().log("Error handling chat event: " + e.getMessage());
                }
            }
        );

        getLogger().atInfo().log("Chat display modifier registered - Discord names/prefixes will show for verified players");
    }
    
    /**
     * Handle verification code typed in game chat
     */

    private void handleVerificationCode(String playerUuid, String playerName, String code) {
        if (verificationManager == null) {
            return;
        }
        
        // Check if there's a pending verification for this player
        LinkVerificationManager.VerificationResult result = 
            verificationManager.verifyCodeByPlayer(playerUuid, code);
        
        if (result.success) {
            // Find the player and send success message
            PlayerRef playerRef = onlinePlayers.get(playerUuid);
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw(result.message));
            }
            
            // Invalidate verification cache since status changed
            invalidateVerificationCache(playerUuid);
            
            // Assign linked role
            if (discordBot != null && discordBot.isReady()) {
                String discordId = verificationManager.getDiscordIdByHytaleUuid(playerUuid);
                if (discordId != null) {
                    discordBot.assignLinkedRole(discordId);
                }
            }
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
        
        // End all player sessions and save statistics
        if (statisticsManager != null) {
            statisticsManager.endAllSessions();
        }
        
        // Send server offline status and disconnect WebSocket client
        if (wsClient != null) {
            wsClient.sendServerOffline();
            // Give time for message to send
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            wsClient.disconnect();
        }
        
        // Shutdown verification manager
        if (verificationManager != null) {
            verificationManager.shutdown();
        }
        
        // Shutdown private message logger
        if (privateMessageLogger != null) {
            privateMessageLogger.shutdown();
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
     * Get chat settings manager
     * @return ChatSettingsManager instance
     */
    public ChatSettingsManager getChatSettingsManager() {
        return chatSettingsManager;
    }
    
    /**
     * Get player statistics manager
     * @return PlayerStatisticsManager instance
     */
    public PlayerStatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
    
    /**
     * Check if player is verified with caching to reduce lookup overhead.
     * @param playerUuid the player UUID string
     * @return true if verified, false otherwise
     */
    private boolean isVerifiedCached(String playerUuid) {
        if (verificationManager == null) return false;
        
        Long cachedTime = verifyCacheTime.get(playerUuid);
        if (cachedTime != null && System.currentTimeMillis() - cachedTime < VERIFY_CACHE_TTL_MS) {
            Boolean cached = verifiedCache.get(playerUuid);
            if (cached != null) return cached;
        }
        
        boolean verified = verificationManager.isVerified(playerUuid);
        verifiedCache.put(playerUuid, verified);
        verifyCacheTime.put(playerUuid, System.currentTimeMillis());
        return verified;
    }
    
    /**
     * Invalidate verification cache for a player (call after linking/unlinking).
     */
    public void invalidateVerificationCache(String playerUuid) {
        verifiedCache.remove(playerUuid);
        verifyCacheTime.remove(playerUuid);
    }
    
    /**
     * Reload configuration from disk
     */
    public void reloadConfiguration() {
        getLogger().atInfo().log("Reloading configuration...");
        config = configManager.reloadConfig();
        messageFormatter = new MessageFormatter(config); // Recreate with new config
        getLogger().atInfo().log("Configuration reloaded successfully");
    }
    
    /**
     * Get list of online player names for WebSocket status updates
     * @return List of player usernames currently online
     */
    private java.util.List<String> getOnlinePlayerNames() {
        return onlinePlayers.values().stream()
            .map(PlayerRef::getUsername)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get the online players map (UUID -> PlayerRef)
     * @return Map of online players
     */
    public java.util.Map<String, PlayerRef> getOnlinePlayers() {
        return new java.util.concurrent.ConcurrentHashMap<>(onlinePlayers);
    }
    
    /**
     * Get the private message logger
     * @return PrivateMessageLogger instance
     */
    public PrivateMessageLogger getPrivateMessageLogger() {
        return privateMessageLogger;
    }
    
    /**
     * Send a message to a specific player by UUID
     * @param playerUuid The player's UUID
     * @param message The message to send
     */

    public void sendMessageToPlayer(String playerUuid, String message) {
        PlayerRef playerRef = onlinePlayers.get(playerUuid);
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw(message));
        }
    }
    
}
