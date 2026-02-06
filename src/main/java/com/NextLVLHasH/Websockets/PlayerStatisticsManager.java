package com.NextLVLHasH.Websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages player statistics persistence and tracking
 * Saves data to player_statistics.json in the plugin data directory
 */
public class PlayerStatisticsManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayerStatisticsManager");
    private static final String STATS_FILE = "player_statistics.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path dataDirectory;
    private final Path statsFilePath;
    
    // UUID -> PlayerStatistics mapping
    private final Map<String, PlayerStatistics> playerStats = new ConcurrentHashMap<>();
    
    // Auto-save interval tracking
    private long lastSaveTime = 0;
    private static final long AUTO_SAVE_INTERVAL_MS = 300000; // 5 minutes
    
    public PlayerStatisticsManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.statsFilePath = dataDirectory.resolve(STATS_FILE);
        loadStats();
    }
    
    /**
     * Load statistics from JSON file
     */
    public void loadStats() {
        try {
            if (Files.exists(statsFilePath)) {
                String json = Files.readString(statsFilePath);
                Type type = new TypeToken<Map<String, PlayerStatistics>>(){}.getType();
                Map<String, PlayerStatistics> loaded = GSON.fromJson(json, type);
                
                if (loaded != null) {
                    playerStats.clear();
                    playerStats.putAll(loaded);
                    LOGGER.info("Loaded statistics for " + playerStats.size() + " players");
                    
                    // Reset any sessions that were left open (server crash recovery)
                    for (PlayerStatistics stats : playerStats.values()) {
                        if (stats.isInSession()) {
                            // End the orphaned session
                            stats.endSession();
                            LOGGER.info("Recovered orphaned session for: " + stats.getPlayerName());
                        }
                    }
                }
            } else {
                LOGGER.info("No existing player statistics file found, starting fresh");
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to load player statistics: " + e.getMessage());
        }
    }
    
    /**
     * Save statistics to JSON file
     */
    public void saveStats() {
        try {
            // Ensure directory exists
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            String json = GSON.toJson(playerStats);
            Files.writeString(statsFilePath, json);
            lastSaveTime = System.currentTimeMillis();
            LOGGER.fine("Saved statistics for " + playerStats.size() + " players");
        } catch (IOException e) {
            LOGGER.warning("Failed to save player statistics: " + e.getMessage());
        }
    }
    
    /**
     * Auto-save if enough time has passed
     */
    public void autoSaveIfNeeded() {
        if (System.currentTimeMillis() - lastSaveTime > AUTO_SAVE_INTERVAL_MS) {
            saveStats();
        }
    }
    
    /**
     * Get or create statistics for a player
     */
    public PlayerStatistics getOrCreateStats(String playerUuid, String playerName) {
        return playerStats.computeIfAbsent(playerUuid, uuid -> {
            LOGGER.info("Creating new statistics entry for: " + playerName);
            return new PlayerStatistics(uuid, playerName);
        });
    }
    
    /**
     * Get statistics for a player (returns null if not found)
     */
    public PlayerStatistics getStats(String playerUuid) {
        return playerStats.get(playerUuid);
    }
    
    /**
     * Called when a player joins
     */
    public void onPlayerJoin(String playerUuid, String playerName) {
        PlayerStatistics stats = getOrCreateStats(playerUuid, playerName);
        
        // Update name in case it changed
        stats.setPlayerName(playerName);
        
        // Start session
        stats.startSession();
        
        LOGGER.info("Started session for " + playerName + 
                   " (Total playtime: " + stats.getFormattedPlaytime() + 
                   ", Session #" + stats.getTotalSessions() + ")");
        
        autoSaveIfNeeded();
    }
    
    /**
     * Called when a player leaves
     */
    public void onPlayerLeave(String playerUuid) {
        PlayerStatistics stats = playerStats.get(playerUuid);
        if (stats != null) {
            long sessionDuration = stats.endSession();
            LOGGER.info("Ended session for " + stats.getPlayerName() + 
                       " (Session: " + PlayerStatistics.formatPlaytime(sessionDuration) + 
                       ", Total: " + stats.getFormattedPlaytime() + ")");
            
            // Save immediately on player leave to preserve data
            saveStats();
        }
    }
    
    /**
     * Called when a player sends a chat message
     */
    public void onPlayerChat(String playerUuid) {
        PlayerStatistics stats = playerStats.get(playerUuid);
        if (stats != null) {
            stats.incrementMessagesSent();
            autoSaveIfNeeded();
        }
    }
    
    /**
     * Called when a player breaks a block
     */
    public void onBlockBroken(String playerUuid) {
        PlayerStatistics stats = playerStats.get(playerUuid);
        if (stats != null) {
            stats.incrementBlocksBroken();
            autoSaveIfNeeded();
        }
    }
    
    /**
     * Called when a player kills a mob
     */
    public void onMobKilled(String playerUuid) {
        PlayerStatistics stats = playerStats.get(playerUuid);
        if (stats != null) {
            stats.incrementMobsKilled();
            autoSaveIfNeeded();
        }
    }
    
    /**
     * End all active sessions (called on server shutdown)
     */
    public void endAllSessions() {
        for (PlayerStatistics stats : playerStats.values()) {
            if (stats.isInSession()) {
                stats.endSession();
            }
        }
        saveStats();
        LOGGER.info("Ended all active sessions and saved statistics");
    }
    
    /**
     * Get top players by playtime
     * @param limit Maximum number of players to return
     * @return List of top players sorted by playtime (descending)
     */
    public List<PlayerStatistics> getTopByPlaytime(int limit) {
        return playerStats.values().stream()
            .sorted(Comparator.comparingLong(PlayerStatistics::getTotalPlaytimeWithCurrentSession).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get top players by messages sent
     * @param limit Maximum number of players to return
     * @return List of top players sorted by messages (descending)
     */
    public List<PlayerStatistics> getTopByMessages(int limit) {
        return playerStats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStatistics::getMessagesSent).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get top players by blocks broken
     * @param limit Maximum number of players to return
     * @return List of top players sorted by blocks broken (descending)
     */
    public List<PlayerStatistics> getTopByBlocksBroken(int limit) {
        return playerStats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStatistics::getBlocksBroken).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get top players by mobs killed
     * @param limit Maximum number of players to return
     * @return List of top players sorted by mobs killed (descending)
     */
    public List<PlayerStatistics> getTopByMobsKilled(int limit) {
        return playerStats.values().stream()
            .sorted(Comparator.comparingInt(PlayerStatistics::getMobsKilled).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get total server playtime across all players
     */
    public long getTotalServerPlaytime() {
        return playerStats.values().stream()
            .mapToLong(PlayerStatistics::getTotalPlaytimeWithCurrentSession)
            .sum();
    }
    
    /**
     * Get count of unique players
     */
    public int getUniquePlayerCount() {
        return playerStats.size();
    }
    
    /**
     * Get all player statistics
     */
    public Map<String, PlayerStatistics> getAllStats() {
        return new ConcurrentHashMap<>(playerStats);
    }
    
    /**
     * Get player rank by playtime (1-indexed)
     */
    public int getPlaytimeRank(String playerUuid) {
        List<PlayerStatistics> sorted = new ArrayList<>(playerStats.values());
        sorted.sort(Comparator.comparingLong(PlayerStatistics::getTotalPlaytimeWithCurrentSession).reversed());
        
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPlayerUuid().equals(playerUuid)) {
                return i + 1;
            }
        }
        return -1;
    }
}
