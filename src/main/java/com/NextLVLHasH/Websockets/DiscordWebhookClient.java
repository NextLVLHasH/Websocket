package com.NextLVLHasH.Websockets;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Discord Webhook client for sending rich embeds to Discord channels
 */
public class DiscordWebhookClient {
    private static final Logger LOGGER = Logger.getLogger(DiscordWebhookClient.class.getName());
    private final String webhookUrl;
    private final HttpClient httpClient;
    private final String botName;
    private final String avatarUrl;
    private final String serverName;
    private final String serverAddress;
    private final String serverDescription;

    public DiscordWebhookClient(String webhookUrl, String botName, String avatarUrl,
                               String serverName, String serverAddress, String serverDescription) {
        this.webhookUrl = webhookUrl;
        this.botName = botName;
        this.avatarUrl = avatarUrl;
        this.serverName = serverName;
        this.serverAddress = serverAddress;
        this.serverDescription = serverDescription;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send player join notification to Discord
     */
    public void sendPlayerJoinNotification(String playerName, String playerUuid, String currentServerName, int totalPlayers) {
        String title = "✅ Player Joined";
        String description = String.format("**%s** has joined the server!", playerName);
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[");
        fields.append("{\"name\":\"👤 Player\",\"value\":\"").append(escapeJson(playerName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"👥 Players Online\",\"value\":\"**").append(totalPlayers).append("**\",\"inline\":true},");
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":false},");
        fields.append("{\"name\":\"📍 Server Address\",\"value\":\"```").append(escapeJson(serverAddress)).append("```\",\"inline\":false}");
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            fields.append(",{\"name\":\"ℹ️ About\",\"value\":\"").append(escapeJson(serverDescription)).append("\",\"inline\":false}");
        }
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0x00FF00, getCurrentTimestamp(), fields.toString());
    }

    /**
     * Send player leave notification to Discord
     */
    public void sendPlayerLeaveNotification(String playerName, String playerUuid, String currentServerName, int totalPlayers) {
        String title = "❌ Player Left";
        String description = String.format("**%s** has left the server.", playerName);
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[");
        fields.append("{\"name\":\"👤 Player\",\"value\":\"").append(escapeJson(playerName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"👥 Players Online\",\"value\":\"**").append(totalPlayers).append("**\",\"inline\":true},");
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":false},");
        fields.append("{\"name\":\"📍 Server Address\",\"value\":\"```").append(escapeJson(serverAddress)).append("```\",\"inline\":false}");
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            fields.append(",{\"name\":\"ℹ️ About\",\"value\":\"").append(escapeJson(serverDescription)).append("\",\"inline\":false}");
        }
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0xFF0000, getCurrentTimestamp(), fields.toString());
    }

    /**
     * Send server start notification to Discord
     */
    public void sendServerStartNotification(int totalPlayers) {
        String title = "🟢 Server Started";
        String description = String.format("**%s** is now online!", serverName);
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[" );
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"👥 Players Online\",\"value\":\"**").append(totalPlayers).append("**\",\"inline\":true},");
        fields.append("{\"name\":\"📍 Server Address\",\"value\":\"```").append(escapeJson(serverAddress)).append("```\",\"inline\":false}");
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            fields.append(",{\"name\":\"ℹ️ About\",\"value\":\"").append(escapeJson(serverDescription)).append("\",\"inline\":false}");
        }
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0x00FF00, getCurrentTimestamp(), fields.toString());
    }

    /**
     * Send server stop notification to Discord
     */
    public void sendServerStopNotification(int totalPlayers) {
        String title = "🔴 Server Stopped";
        String description = String.format("**%s** is now offline.", serverName);
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[" );
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"👥 Final Player Count\",\"value\":\"**").append(totalPlayers).append("**\",\"inline\":true}");
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0xFF0000, getCurrentTimestamp(), fields.toString());
    }

    /**
     * Send periodic poll notification to Discord
     */
    public void sendPeriodicPoll(String currentServerName, int playerCount) {
        String title = "🔄 Server Status Update";
        String description = "Current server activity report";
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[");
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"👥 Players Online\",\"value\":\"**").append(playerCount).append("**\",\"inline\":true},");
        fields.append("{\"name\":\"📍 Server Address\",\"value\":\"```").append(escapeJson(serverAddress)).append("```\",\"inline\":false}");
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            fields.append(",{\"name\":\"ℹ️ About\",\"value\":\"").append(escapeJson(serverDescription)).append("\",\"inline\":false}");
        }
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0x0099FF, getCurrentTimestamp(), fields.toString());
    }

    /**
     * Build Discord embed JSON with custom fields
     */
    private String buildEmbedJsonWithFields(String title, String description, int color, String timestamp, String fields) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Add username if provided
        if (botName != null && !botName.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(botName)).append("\",");
        }
        
        // Add avatar URL if provided
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        
        // Add embed
        json.append("\"embeds\":[{");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        json.append("\"color\":").append(color).append(",");
        
        if (timestamp != null && !timestamp.isEmpty()) {
            json.append("\"timestamp\":\"").append(timestamp).append("\",");
        }
        
        // Add fields
        json.append(fields);
        
        json.append("}]}");
        
        return json.toString();
    }

    /**
     * Send embed with custom fields
     */
    private void sendEmbedWithFields(String title, String description, int color, String timestamp, String fields) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOGGER.warning("Discord webhook URL not configured");
            return;
        }

        String json = buildEmbedJsonWithFields(title, description, color, timestamp, fields);
        sendWebhook(json);
    }

    /**
     * Send webhook POST request
     */
    private void sendWebhook(String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 204) {
                            LOGGER.info("Discord webhook sent successfully");
                        } else {
                            LOGGER.warning("Discord webhook failed with status: " + response.statusCode());
                        }
                    })
                    .exceptionally(error -> {
                        LOGGER.severe("Failed to send Discord webhook: " + error.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            LOGGER.severe("Error sending Discord webhook: " + e.getMessage());
        }
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Test webhook connectivity
     */
    public void testConnection() {
        String title = "🔔 Webhook Connected";
        String description = "Discord webhook is configured and working correctly!";
        
        StringBuilder fields = new StringBuilder();
        fields.append("\"fields\":[");
        fields.append("{\"name\":\"🎮 Server\",\"value\":\"").append(escapeJson(serverName)).append("\",\"inline\":true},");
        fields.append("{\"name\":\"✅ Status\",\"value\":\"Online\",\"inline\":true},");
        fields.append("{\"name\":\"📍 Server Address\",\"value\":\"```").append(escapeJson(serverAddress)).append("```\",\"inline\":false}");
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            fields.append(",{\"name\":\"ℹ️ About\",\"value\":\"").append(escapeJson(serverDescription)).append("\",\"inline\":false}");
        }
        fields.append("]");
        
        sendEmbedWithFields(title, description, 0x00FF00, getCurrentTimestamp(), fields.toString());
    }
}
