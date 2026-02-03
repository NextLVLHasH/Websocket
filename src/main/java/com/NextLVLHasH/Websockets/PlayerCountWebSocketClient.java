package com.NextLVLHasH.Websockets;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * WebSocket client for sending game server events to the WebSocket server.
 * Sends player join/leave events immediately and periodic status updates.
 * 
 * Message formats:
 * - playerJoin: {"type":"playerJoin","serverId":"hytale","playerName":"...","playerCount":X,"totalCount":X,"timestamp":...}
 * - playerLeave: {"type":"playerLeave","serverId":"hytale","playerName":"...","playerCount":X,"totalCount":X,"timestamp":...}
 * - serverStatus: {"type":"serverStatus","serverId":"hytale","online":true,"playerCount":X,"maxPlayers":X,"playerNames":[...],"version":"...","motd":"...","timestamp":...}
 */
public class PlayerCountWebSocketClient implements WebSocket.Listener {
    private static final Logger LOGGER = Logger.getLogger(PlayerCountWebSocketClient.class.getName());
    
    private final String serverUrl;
    private final HttpClient httpClient;
    private WebSocket webSocket;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    
    // Server identification
    private static final String SERVER_ID = "hytale";
    
    // Track player counts
    private final AtomicInteger playerCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    
    // Server info for status updates
    private String serverVersion = "1.0.0";
    private String serverMotd = "Hytale Server";
    private int maxPlayers = 100;
    private volatile List<String> currentPlayerNames = List.of();
    
    // Pulse interval (5 minutes)
    private static final long PULSE_INTERVAL_MINUTES = 5;
    private ScheduledFuture<?> pulseTask;
    
    // Reconnection settings
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_BASE_DELAY_SECONDS = 30;
    private int reconnectAttempts = 0;
    
    // Connection timeout
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    
    /**
     * Create a new optimized WebSocket client for player count data
     * @param serverUrl The WebSocket server URL (e.g., ws://localhost:8080)
     */
    public PlayerCountWebSocketClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PlayerCountWS");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Connect to the WebSocket server and start the 5-minute pulse timer
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Void> connect() {
        if (connected.get() || connecting.get()) {
            return CompletableFuture.completedFuture(null);
        }
        
        connecting.set(true);
        LOGGER.info("Connecting to WebSocket server: " + serverUrl);
        
        return httpClient.newWebSocketBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .buildAsync(URI.create(serverUrl), this)
            .thenAccept(ws -> {
                this.webSocket = ws;
                connected.set(true);
                connecting.set(false);
                reconnectAttempts = 0;
                LOGGER.info("Connected to WebSocket server: " + serverUrl);
                
                // Start the 5-minute pulse timer
                startPulseTimer();
                
                // Send initial pulse immediately
                sendPulse();
            })
            .exceptionally(ex -> {
                connecting.set(false);
                LOGGER.warning("Failed to connect to WebSocket: " + ex.getMessage());
                scheduleReconnect();
                return null;
            });
    }
    
    /**
     * Start the 5-minute heartbeat pulse timer
     */
    private void startPulseTimer() {
        // Cancel existing timer if any
        if (pulseTask != null && !pulseTask.isDone()) {
            pulseTask.cancel(false);
        }
        
        // Schedule pulse every 5 minutes
        pulseTask = scheduler.scheduleAtFixedRate(
            this::sendPulse,
            PULSE_INTERVAL_MINUTES,
            PULSE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        LOGGER.info("Pulse timer started (every " + PULSE_INTERVAL_MINUTES + " minutes)");
    }
    
    /**
     * Stop the pulse timer
     */
    private void stopPulseTimer() {
        if (pulseTask != null && !pulseTask.isDone()) {
            pulseTask.cancel(false);
            pulseTask = null;
        }
    }
    
    /**
     * Send a full server status update (heartbeat pulse)
     */
    private void sendPulse() {
        sendServerStatus(true);
    }
    
    /**
     * Send a server status message
     * @param online Whether the server is online
     */
    public void sendServerStatus(boolean online) {
        if (!connected.get() || webSocket == null) {
            return;
        }
        
        String playerNamesJson = currentPlayerNames.stream()
            .map(name -> "\"" + escapeJson(name) + "\"")
            .collect(Collectors.joining(",", "[", "]"));
        
        String json = String.format(
            "{\"type\":\"serverStatus\",\"serverId\":\"%s\",\"online\":%s,\"playerCount\":%d,\"maxPlayers\":%d,\"playerNames\":%s,\"version\":\"%s\",\"motd\":\"%s\",\"timestamp\":%d}",
            SERVER_ID,
            online,
            playerCount.get(),
            maxPlayers,
            playerNamesJson,
            escapeJson(serverVersion),
            escapeJson(serverMotd),
            System.currentTimeMillis()
        );
        
        LOGGER.info("Sending serverStatus: " + json);
        
        webSocket.sendText(json, true)
            .exceptionally(ex -> {
                LOGGER.warning("Failed to send serverStatus: " + ex.getMessage());
                handleDisconnect();
                return null;
            });
    }
    
    /**
     * Send a player join event
     * @param playerName The name of the player who joined
     * @param currentPlayerCount Current number of online players
     * @param totalPlayerCount Total player count
     */
    public void sendPlayerJoin(String playerName, int currentPlayerCount, int totalPlayerCount) {
        this.playerCount.set(currentPlayerCount);
        this.totalCount.set(totalPlayerCount);
        
        if (!connected.get() || webSocket == null) {
            LOGGER.warning("Cannot send playerJoin - not connected");
            return;
        }
        
        String json = String.format(
            "{\"type\":\"playerJoin\",\"serverId\":\"%s\",\"playerName\":\"%s\",\"playerCount\":%d,\"totalCount\":%d,\"timestamp\":%d}",
            SERVER_ID,
            escapeJson(playerName),
            currentPlayerCount,
            totalPlayerCount,
            System.currentTimeMillis()
        );
        
        LOGGER.info("Sending playerJoin: " + json);
        
        webSocket.sendText(json, true)
            .exceptionally(ex -> {
                LOGGER.warning("Failed to send playerJoin: " + ex.getMessage());
                return null;
            });
    }
    
    /**
     * Send a player leave event
     * @param playerName The name of the player who left
     * @param currentPlayerCount Current number of online players
     * @param totalPlayerCount Total player count
     */
    public void sendPlayerLeave(String playerName, int currentPlayerCount, int totalPlayerCount) {
        this.playerCount.set(currentPlayerCount);
        this.totalCount.set(totalPlayerCount);
        
        if (!connected.get() || webSocket == null) {
            LOGGER.warning("Cannot send playerLeave - not connected");
            return;
        }
        
        String json = String.format(
            "{\"type\":\"playerLeave\",\"serverId\":\"%s\",\"playerName\":\"%s\",\"playerCount\":%d,\"totalCount\":%d,\"timestamp\":%d}",
            SERVER_ID,
            escapeJson(playerName),
            currentPlayerCount,
            totalPlayerCount,
            System.currentTimeMillis()
        );
        
        LOGGER.info("Sending playerLeave: " + json);
        
        webSocket.sendText(json, true)
            .exceptionally(ex -> {
                LOGGER.warning("Failed to send playerLeave: " + ex.getMessage());
                return null;
            });
    }
    
    /**
     * Escape special characters for JSON string
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    /**
     * Update server information for status messages
     * @param version Server version
     * @param motd Server message of the day
     * @param maxPlayers Maximum players allowed
     */
    public void setServerInfo(String version, String motd, int maxPlayers) {
        this.serverVersion = version != null ? version : "1.0.0";
        this.serverMotd = motd != null ? motd : "Hytale Server";
        this.maxPlayers = maxPlayers > 0 ? maxPlayers : 100;
    }
    
    /**
     * Update the list of current player names
     * @param playerNames List of online player names
     */
    public void updatePlayerNames(List<String> playerNames) {
        this.currentPlayerNames = playerNames != null ? List.copyOf(playerNames) : List.of();
        this.playerCount.set(this.currentPlayerNames.size());
    }
    
    /**
     * Schedule a reconnection attempt with exponential backoff
     */
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            LOGGER.severe("Max reconnection attempts reached. Will retry in 5 minutes.");
            // Schedule a retry after 5 minutes instead of giving up completely
            scheduler.schedule(this::resetAndReconnect, PULSE_INTERVAL_MINUTES, TimeUnit.MINUTES);
            return;
        }
        
        reconnectAttempts++;
        // Exponential backoff: 30s, 60s, 120s, 240s, 480s
        int delay = RECONNECT_BASE_DELAY_SECONDS * (1 << (reconnectAttempts - 1));
        delay = Math.min(delay, 300); // Cap at 5 minutes
        
        LOGGER.info("Scheduling reconnection attempt " + reconnectAttempts + " in " + delay + " seconds...");
        scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }
    
    /**
     * Reset reconnection counter and try again
     */
    private void resetAndReconnect() {
        reconnectAttempts = 0;
        connect();
    }
    
    /**
     * Update player count (called by main mod on join/leave events)
     * Does NOT immediately send - waits for next pulse
     * @param totalPlayers Total number of players registered
     * @param onlinePlayers Number of players currently online
     */
    public void updatePlayerCount(int totalPlayers, int onlinePlayers) {
        this.playerCount.set(onlinePlayers);
        this.totalCount.set(totalPlayers);
        // Just update the values - the pulse timer will send them
    }
    
    /**
     * Legacy method for backwards compatibility
     * @deprecated Use sendPlayerJoin/sendPlayerLeave instead
     */
    @Deprecated
    public void sendPlayerCount(int totalPlayers, int onlinePlayers) {
        updatePlayerCount(totalPlayers, onlinePlayers);
    }
    
    /**
     * Force an immediate status pulse (use sparingly - for server start/stop)
     */
    public void sendImmediatePulse(int totalPlayers, int onlinePlayers) {
        this.playerCount.set(onlinePlayers);
        this.totalCount.set(totalPlayers);
        
        if (connected.get()) {
            sendServerStatus(true);
        } else if (!connecting.get()) {
            // Try to connect first
            connect();
        }
    }
    
    /**
     * Send server offline status (for shutdown)
     */
    public void sendServerOffline() {
        if (connected.get()) {
            sendServerStatus(false);
        }
    }
    
    /**
     * Handle WebSocket disconnection
     */
    private void handleDisconnect() {
        connected.set(false);
        stopPulseTimer();
        webSocket = null;
        scheduleReconnect();
    }
    
    /**
     * Disconnect from the WebSocket server
     */
    public void disconnect() {
        stopPulseTimer();
        scheduler.shutdown();
        
        if (webSocket != null && connected.get()) {
            LOGGER.info("Disconnecting from WebSocket server...");
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Shutting down")
                .thenRun(() -> {
                    connected.set(false);
                    webSocket = null;
                    LOGGER.info("Disconnected from WebSocket server");
                });
        }
    }
    
    /**
     * Check if connected to the WebSocket server
     * @return true if connected
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    // WebSocket.Listener implementation
    
    @Override
    public void onOpen(WebSocket webSocket) {
        LOGGER.info("WebSocket connection opened");
        webSocket.request(1);
    }
    
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        // Silently handle server responses
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.info("WebSocket closed: " + statusCode + " - " + reason);
        handleDisconnect();
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.warning("WebSocket error: " + error.getMessage());
        handleDisconnect();
    }
}
