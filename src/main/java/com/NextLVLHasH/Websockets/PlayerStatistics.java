package com.NextLVLHasH.Websockets;

import java.time.Instant;

/**
 * Data class representing a player's statistics
 * Tracks playtime, sessions, and other stats
 */
public class PlayerStatistics {
    
    private String playerUuid;
    private String playerName;
    
    // Playtime tracking
    private long totalPlaytimeSeconds;
    private long currentSessionStartTime; // Unix timestamp when session started (0 if not online)
    private int totalSessions;
    
    // First/last seen
    private long firstSeenTimestamp;
    private long lastSeenTimestamp;
    
    // Additional stats
    private int messagesSent;
    
    // Block and entity tracking
    private int blocksBroken;
    private int mobsKilled;
    
    public PlayerStatistics() {
        // Default constructor for JSON deserialization
    }
    
    public PlayerStatistics(String playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.totalPlaytimeSeconds = 0;
        this.currentSessionStartTime = 0;
        this.totalSessions = 0;
        this.firstSeenTimestamp = Instant.now().getEpochSecond();
        this.lastSeenTimestamp = this.firstSeenTimestamp;
        this.messagesSent = 0;
        this.blocksBroken = 0;
        this.mobsKilled = 0;
    }
    
    // --- Session Management ---
    
    /**
     * Start a new session (player joined)
     */
    public void startSession() {
        this.currentSessionStartTime = Instant.now().getEpochSecond();
        this.totalSessions++;
        this.lastSeenTimestamp = this.currentSessionStartTime;
    }
    
    /**
     * End the current session (player left)
     * @return The session duration in seconds
     */
    public long endSession() {
        if (currentSessionStartTime == 0) {
            return 0;
        }
        
        long now = Instant.now().getEpochSecond();
        long sessionDuration = now - currentSessionStartTime;
        this.totalPlaytimeSeconds += sessionDuration;
        this.lastSeenTimestamp = now;
        this.currentSessionStartTime = 0;
        
        return sessionDuration;
    }
    
    /**
     * Check if player is currently in a session
     */
    public boolean isInSession() {
        return currentSessionStartTime > 0;
    }
    
    /**
     * Get current session duration in seconds
     */
    public long getCurrentSessionDuration() {
        if (currentSessionStartTime == 0) {
            return 0;
        }
        return Instant.now().getEpochSecond() - currentSessionStartTime;
    }
    
    /**
     * Get total playtime including current session
     */
    public long getTotalPlaytimeWithCurrentSession() {
        return totalPlaytimeSeconds + getCurrentSessionDuration();
    }
    
    // --- Stats Increment ---
    
    public void incrementMessagesSent() {
        this.messagesSent++;
    }
    
    public void incrementBlocksBroken() {
        this.blocksBroken++;
    }
    
    public void incrementMobsKilled() {
        this.mobsKilled++;
    }
    
    // --- Formatting ---
    
    /**
     * Format playtime as human-readable string
     * @param seconds Total seconds
     * @return Formatted string like "2d 5h 30m" or "45m 12s"
     */
    public static String formatPlaytime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m");
        }
        if (days == 0 && hours == 0) {
            sb.append(" ").append(secs).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Get formatted total playtime string
     */
    public String getFormattedPlaytime() {
        return formatPlaytime(getTotalPlaytimeWithCurrentSession());
    }
    
    /**
     * Get formatted current session time
     */
    public String getFormattedSessionTime() {
        return formatPlaytime(getCurrentSessionDuration());
    }
    
    // --- Getters and Setters ---
    
    public String getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public long getTotalPlaytimeSeconds() {
        return totalPlaytimeSeconds;
    }
    
    public void setTotalPlaytimeSeconds(long totalPlaytimeSeconds) {
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
    }
    
    public long getCurrentSessionStartTime() {
        return currentSessionStartTime;
    }
    
    public void setCurrentSessionStartTime(long currentSessionStartTime) {
        this.currentSessionStartTime = currentSessionStartTime;
    }
    
    public int getTotalSessions() {
        return totalSessions;
    }
    
    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }
    
    public long getFirstSeenTimestamp() {
        return firstSeenTimestamp;
    }
    
    public void setFirstSeenTimestamp(long firstSeenTimestamp) {
        this.firstSeenTimestamp = firstSeenTimestamp;
    }
    
    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }
    
    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp = lastSeenTimestamp;
    }
    
    public int getMessagesSent() {
        return messagesSent;
    }
    
    public void setMessagesSent(int messagesSent) {
        this.messagesSent = messagesSent;
    }
    
    public int getBlocksBroken() {
        return blocksBroken;
    }
    
    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }
    
    public int getMobsKilled() {
        return mobsKilled;
    }
    
    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }
}
