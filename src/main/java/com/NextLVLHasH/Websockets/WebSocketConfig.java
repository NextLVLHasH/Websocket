package com.NextLVLHasH.Websockets;

/**
 * Configuration class for WebSocket settings
 * This should be loaded from a config file in production
 */
public class WebSocketConfig {
    private static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:8080/notifications";
    
    private String websocketUrl;
    private boolean enablePeriodicPolling;
    private int pollingIntervalMinutes;
    private boolean enablePlayerJoinNotifications;
    private boolean enablePlayerLeaveNotifications;

    public WebSocketConfig() {
        // Default values
        this.websocketUrl = DEFAULT_WEBSOCKET_URL;
        this.enablePeriodicPolling = true;
        this.pollingIntervalMinutes = 5;
        this.enablePlayerJoinNotifications = true;
        this.enablePlayerLeaveNotifications = true;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public boolean isEnablePeriodicPolling() {
        return enablePeriodicPolling;
    }

    public void setEnablePeriodicPolling(boolean enablePeriodicPolling) {
        this.enablePeriodicPolling = enablePeriodicPolling;
    }

    public int getPollingIntervalMinutes() {
        return pollingIntervalMinutes;
    }

    public void setPollingIntervalMinutes(int pollingIntervalMinutes) {
        this.pollingIntervalMinutes = pollingIntervalMinutes;
    }

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
}
