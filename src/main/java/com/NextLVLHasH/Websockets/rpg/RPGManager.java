package com.NextLVLHasH.Websockets.rpg;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.core.RPGConfig;
import com.NextLVLHasH.Websockets.rpg.persistence.RPGDataRepository;
import com.NextLVLHasH.Websockets.rpg.character.CharacterManager;
import com.NextLVLHasH.Websockets.rpg.progression.ProgressionManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillManager;
import com.NextLVLHasH.Websockets.rpg.combat.CombatManager;
import com.NextLVLHasH.Websockets.rpg.ui.UIManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main facade for the RPG system.
 * Manages player data, configuration, and provides the primary API for RPG functionality.
 * Implements the Singleton pattern.
 */
public class RPGManager {

    private static final Logger LOGGER = Logger.getLogger(RPGManager.class.getName());
    private static volatile RPGManager instance;
    private static final Object LOCK = new Object();

    // Core components
    private final Map<UUID, PlayerRPGData> playerDataCache;
    private RPGConfig config;
    private RPGDataRepository repository;
    
    // Sub-managers
    private CharacterManager characterManager;
    private ProgressionManager progressionManager;
    private SkillManager skillManager;
    private CombatManager combatManager;
    private UIManager uiManager;
    
    // Death handling (not an ECS system, called by other systems)
    private com.NextLVLHasH.Websockets.rpg.systems.RPGDeathSystem deathSystem;

    // State management
    private volatile boolean initialized = false;
    private ScheduledExecutorService autoSaveExecutor;
    @SuppressWarnings("unused")
    private Path dataPath;

    /**
     * Private constructor for singleton pattern.
     */
    private RPGManager() {
        this.playerDataCache = new ConcurrentHashMap<>();
    }

    /**
     * Gets the singleton instance of RPGManager.
     *
     * @return the singleton RPGManager instance
     */
    public static RPGManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RPGManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (primarily for testing).
     */
    public static void resetInstance() {
        synchronized (LOCK) {
            if (instance != null) {
                instance.shutdown();
                instance = null;
            }
        }
    }

    /**
     * Initializes the RPG system with default settings.
     * Uses the current working directory for data storage.
     *
     * @throws IOException if initialization fails
     */
    public void initialize() throws IOException {
        initialize(Paths.get("."), null);
    }

    /**
     * Initializes the RPG system with a custom data path.
     *
     * @param dataPath the path for storing data files
     * @throws IOException if initialization fails
     */
    public void initialize(Path dataPath) throws IOException {
        initialize(dataPath, null);
    }

    /**
     * Initializes the RPG system with custom data path and config path.
     *
     * @param dataPath   the path for storing data files
     * @param configPath the path to the configuration file (null for default)
     * @throws IOException if initialization fails
     */
    public void initialize(Path dataPath, Path configPath) throws IOException {
        if (initialized) {
            LOGGER.warning("RPGManager is already initialized. Call shutdown() first to reinitialize.");
            return;
        }

        synchronized (LOCK) {
            if (initialized) return;

            this.dataPath = Objects.requireNonNull(dataPath, "Data path cannot be null");

            // Initialize repository
            this.repository = new RPGDataRepository(dataPath);

            // Load configuration
            if (configPath != null) {
                this.config = RPGConfig.loadFromFile(configPath);
            } else {
                Path defaultConfigPath = dataPath.resolve("config").resolve("rpg_config.json");
                this.config = RPGConfig.loadFromFile(defaultConfigPath);
            }

            // Start auto-save scheduler if enabled
            if (config.getAutoSaveInterval() > 0) {
                startAutoSave();
            }
            
            // Initialize sub-managers
            this.characterManager = CharacterManager.getInstance();
            this.characterManager.initialize(dataPath);
            
            this.progressionManager = ProgressionManager.getInstance();
            this.progressionManager.initialize(dataPath);
            
            this.skillManager = SkillManager.getInstance();
            this.skillManager.initialize(dataPath);
            
            this.combatManager = CombatManager.getInstance();
            this.combatManager.initialize();
            
            this.uiManager = UIManager.getInstance();
            this.uiManager.initialize();

            initialized = true;
            LOGGER.info("RPGManager initialized. Data path: " + dataPath);
            
            if (config.isDebug()) {
                LOGGER.info("Debug mode enabled. Configuration: " + config);
            }
        }
    }

    /**
     * Shuts down the RPG system, saving all data and releasing resources.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        synchronized (LOCK) {
            if (!initialized) return;

            LOGGER.info("Shutting down RPGManager...");

            // Stop auto-save
            stopAutoSave();

            // Save all cached player data
            saveAllPlayerData();

            // Clear cache
            playerDataCache.clear();

            initialized = false;
            LOGGER.info("RPGManager shutdown complete.");
        }
    }

    /**
     * Checks if the RPG system is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    // ==================== Player Data Management ====================

    /**
     * Gets player RPG data, loading from disk if necessary.
     * Creates new data if the player has no existing data.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name (used when creating new data)
     * @return the player's RPG data
     * @throws IllegalStateException if RPGManager is not initialized
     */
    public PlayerRPGData getPlayerData(UUID playerId, String playerName) {
        ensureInitialized();
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        // Check cache first
        PlayerRPGData data = playerDataCache.get(playerId);
        if (data != null) {
            return data;
        }

        // Try to load from disk
        data = loadPlayerData(playerId);
        if (data != null) {
            playerDataCache.put(playerId, data);
            return data;
        }

        // Create new data
        String name = playerName != null ? playerName : "Unknown";
        data = new PlayerRPGData(playerId, name);
        
        // Apply starting level from config
        if (config.getStartingLevel() > 1) {
            data.setLevel(config.getStartingLevel());
        }

        playerDataCache.put(playerId, data);
        config.logDebug("Created new RPG data for player: " + playerId);

        return data;
    }

    /**
     * Gets player RPG data from cache only.
     *
     * @param playerId the player's UUID
     * @return the player's RPG data, or null if not in cache
     */
    public PlayerRPGData getPlayerData(UUID playerId) {
        ensureInitialized();
        return playerId != null ? playerDataCache.get(playerId) : null;
    }

    /**
     * Gets player RPG data as an Optional.
     *
     * @param playerId the player's UUID
     * @return Optional containing the player data if available
     */
    public Optional<PlayerRPGData> getPlayerDataOptional(UUID playerId) {
        return Optional.ofNullable(getPlayerData(playerId));
    }

    /**
     * Saves player RPG data to disk.
     *
     * @param playerId the player's UUID
     * @return true if save was successful
     */
    public boolean savePlayerData(UUID playerId) {
        ensureInitialized();
        if (playerId == null) return false;

        PlayerRPGData data = playerDataCache.get(playerId);
        if (data == null) {
            config.logDebug("No data to save for player: " + playerId);
            return false;
        }

        try {
            repository.save(data);
            config.logDebug("Saved RPG data for player: " + playerId);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save RPG data for player: " + playerId, e);
            return false;
        }
    }

    /**
     * Loads player RPG data from disk.
     *
     * @param playerId the player's UUID
     * @return the loaded data, or null if not found
     */
    public PlayerRPGData loadPlayerData(UUID playerId) {
        ensureInitialized();
        if (playerId == null) return null;

        PlayerRPGData data = repository.load(playerId);
        if (data != null) {
            playerDataCache.put(playerId, data);
            config.logDebug("Loaded RPG data for player: " + playerId);
        }
        return data;
    }

    /**
     * Removes player data from cache (does not delete from disk).
     *
     * @param playerId the player's UUID
     * @return the removed data, or null if not in cache
     */
    public PlayerRPGData unloadPlayerData(UUID playerId) {
        if (playerId == null) return null;
        return playerDataCache.remove(playerId);
    }

    /**
     * Saves and unloads player data (typically called when player disconnects).
     *
     * @param playerId the player's UUID
     */
    public void saveAndUnloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        unloadPlayerData(playerId);
    }

    /**
     * Saves all cached player data to disk.
     */
    public void saveAllPlayerData() {
        ensureInitialized();
        
        int saved = 0;
        int failed = 0;

        for (UUID playerId : playerDataCache.keySet()) {
            if (savePlayerData(playerId)) {
                saved++;
            } else {
                failed++;
            }
        }

        LOGGER.info("Saved all player data. Success: " + saved + ", Failed: " + failed);
    }

    /**
     * Checks if a player has existing data on disk.
     *
     * @param playerId the player's UUID
     * @return true if data exists
     */
    public boolean hasPlayerData(UUID playerId) {
        ensureInitialized();
        return playerId != null && (playerDataCache.containsKey(playerId) || repository.exists(playerId));
    }

    /**
     * Deletes player data from disk and cache.
     *
     * @param playerId the player's UUID
     * @return true if deletion was successful
     */
    public boolean deletePlayerData(UUID playerId) {
        ensureInitialized();
        if (playerId == null) return false;

        playerDataCache.remove(playerId);
        return repository.tryDelete(playerId);
    }

    // ==================== Character Creation ====================

    /**
     * Creates a character for a player with the specified class, race, and profession.
     *
     * @param playerId     the player's UUID
     * @param classId      the selected class ID
     * @param raceId       the selected race ID
     * @param professionId the selected profession ID (can be null)
     * @return the updated PlayerRPGData
     * @throws IllegalStateException    if RPGManager is not initialized
     * @throws IllegalArgumentException if playerId, classId, or raceId is null
     */
    public PlayerRPGData createCharacter(UUID playerId, String classId, String raceId, String professionId) {
        ensureInitialized();
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(classId, "Class ID cannot be null");
        Objects.requireNonNull(raceId, "Race ID cannot be null");

        PlayerRPGData data = getPlayerData(playerId, null);
        
        if (data.isCharacterCreated()) {
            LOGGER.warning("Character already created for player: " + playerId);
            return data;
        }

        // Set character choices
        data.setSelectedClass(classId);
        data.setSelectedRace(raceId);
        data.setSelectedProfession(professionId);
        data.setCharacterCreated(true);

        // Apply starting configuration
        data.setLevel(config.getStartingLevel());
        data.setCurrentXP(0);
        
        // Grant starting skill points
        int startingPoints = (config.getStartingLevel() - 1) * config.getSkillPointsPerLevel();
        if (startingPoints > 0) {
            data.addSkillPoints(startingPoints);
        }

        // Save the new character
        savePlayerData(playerId);

        LOGGER.info("Created character for player " + playerId + 
                ": Class=" + classId + ", Race=" + raceId + ", Profession=" + professionId);

        return data;
    }

    /**
     * Resets a player's character, allowing them to recreate.
     * Requires respec to be allowed in config and may have a cost.
     *
     * @param playerId the player's UUID
     * @return true if respec was successful
     */
    public boolean respecCharacter(UUID playerId) {
        ensureInitialized();
        if (playerId == null) return false;

        if (!config.isAllowRespec()) {
            LOGGER.warning("Respec is not allowed by configuration.");
            return false;
        }

        PlayerRPGData data = getPlayerData(playerId);
        if (data == null) {
            return false;
        }

        // Create backup before respec
        repository.backup(playerId);

        // Reset character-specific data
        data.setSelectedClass(null);
        data.setSelectedRace(null);
        data.setSelectedProfession(null);
        data.setCharacterCreated(false);

        // Reset progression (optionally preserve level)
        data.setLevel(config.getStartingLevel());
        data.setCurrentXP(0);
        data.setSkillPoints(0);

        // Reset attributes to default
        data.recalculateCurrentAttributes();

        // Clear skills
        data.getUnlockedSkills().stream().toList().forEach(skill -> {
            // Can't directly modify, need to recreate data or add a clearSkills method
        });

        // Clear effects
        data.clearActiveEffects();
        data.clearAllCooldowns();

        // Full restore
        data.fullRestore();

        savePlayerData(playerId);

        LOGGER.info("Respec completed for player: " + playerId);
        return true;
    }

    // ==================== Configuration ====================

    /**
     * Gets the RPG configuration.
     *
     * @return the RPGConfig instance
     */
    public RPGConfig getConfig() {
        return config;
    }

    /**
     * Reloads the configuration from disk.
     *
     * @return true if reload was successful
     */
    public boolean reloadConfig() {
        ensureInitialized();
        try {
            config.reload();
            
            // Restart auto-save with potentially new interval
            stopAutoSave();
            if (config.getAutoSaveInterval() > 0) {
                startAutoSave();
            }
            
            LOGGER.info("Configuration reloaded.");
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to reload configuration", e);
            return false;
        }
    }

    // ==================== Statistics ====================

    /**
     * Gets the number of players currently in cache.
     *
     * @return the number of cached players
     */
    public int getCachedPlayerCount() {
        return playerDataCache.size();
    }

    /**
     * Gets all cached player UUIDs.
     *
     * @return an unmodifiable set of cached player UUIDs
     */
    public Set<UUID> getCachedPlayerIds() {
        return Collections.unmodifiableSet(playerDataCache.keySet());
    }

    /**
     * Gets the total number of player data files on disk.
     *
     * @return the count of player data files
     */
    public long getTotalPlayerCount() {
        ensureInitialized();
        return repository.getPlayerDataCount();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Ensures the manager is initialized before operations.
     *
     * @throws IllegalStateException if not initialized
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("RPGManager is not initialized. Call initialize() first.");
        }
    }

    /**
     * Starts the auto-save scheduler.
     */
    private void startAutoSave() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            return;
        }

        int intervalSeconds = config.getAutoSaveInterval();
        if (intervalSeconds <= 0) {
            return;
        }

        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RPG-AutoSave");
            t.setDaemon(true);
            return t;
        });

        autoSaveExecutor.scheduleAtFixedRate(
                this::autoSaveTask,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        LOGGER.info("Auto-save started with interval: " + intervalSeconds + " seconds");
    }

    /**
     * Stops the auto-save scheduler.
     */
    private void stopAutoSave() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            autoSaveExecutor = null;
            LOGGER.info("Auto-save stopped.");
        }
    }

    /**
     * Task executed by the auto-save scheduler.
     */
    private void autoSaveTask() {
        try {
            if (!initialized) return;
            
            config.logDebug("Auto-save running...");
            saveAllPlayerData();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Auto-save task failed", e);
        }
    }

    /**
     * Gets the data repository (for advanced use cases).
     *
     * @return the RPGDataRepository instance
     */
    public RPGDataRepository getRepository() {
        ensureInitialized();
        return repository;
    }
    
    // ==================== Sub-Manager Getters ====================
    
    /**
     * Gets the character manager for class/race/profession management.
     *
     * @return the CharacterManager instance
     */
    public CharacterManager getCharacterManager() {
        return characterManager;
    }
    
    /**
     * Gets the progression manager for XP and leveling.
     *
     * @return the ProgressionManager instance
     */
    public ProgressionManager getProgressionManager() {
        return progressionManager;
    }
    
    /**
     * Gets the skill manager for skill trees and abilities.
     *
     * @return the SkillManager instance
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    /**
     * Gets the combat manager for damage and effects.
     *
     * @return the CombatManager instance
     */
    public CombatManager getCombatManager() {
        return combatManager;
    }
    
    /**
     * Gets the UI manager for RPG interfaces.
     *
     * @return the UIManager instance
     */
    public UIManager getUIManager() {
        return uiManager;
    }
    
    /**
     * Sets the death system handler.
     * Called during mod initialization.
     *
     * @param deathSystem the RPGDeathSystem instance
     */
    public void setDeathSystem(com.NextLVLHasH.Websockets.rpg.systems.RPGDeathSystem deathSystem) {
        this.deathSystem = deathSystem;
    }
    
    /**
     * Gets the death system handler.
     *
     * @return the RPGDeathSystem instance (may be null if not initialized)
     */
    public com.NextLVLHasH.Websockets.rpg.systems.RPGDeathSystem getDeathSystem() {
        return deathSystem;
    }
    
    // ==================== Additional Player Methods ====================
    
    /**
     * Loads existing player data or initializes new data for a player.
     * This is typically called when a player joins the server.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @return the player's RPG data
     */
    public PlayerRPGData loadOrInitializePlayer(UUID playerId, String playerName) {
        ensureInitialized();
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(playerName, "Player name cannot be null");
        
        // Check cache first
        PlayerRPGData data = playerDataCache.get(playerId);
        if (data != null) {
            // Update name in case it changed
            data.setPlayerName(playerName);
            // Sync unlocked skills into SkillManager so in-game UI sees them
            try {
                var skillMgr = com.NextLVLHasH.Websockets.rpg.skills.SkillManager.getInstance();
                for (String s : data.getUnlockedSkills()) {
                    if (s != null && !s.isEmpty()) {
                        skillMgr.forceUnlockSkill(playerId, s.toLowerCase());
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("Failed to sync unlocked skills to SkillManager: " + e.getMessage());
            }
            return data;
        }
        
        // Try to load from disk
        data = loadPlayerData(playerId);
        if (data != null) {
            data.setPlayerName(playerName);
            LOGGER.info("Loaded existing RPG data for player: " + playerName);
            // Sync unlocked skills into SkillManager so in-game UI sees them
            try {
                var skillMgr = com.NextLVLHasH.Websockets.rpg.skills.SkillManager.getInstance();
                for (String s : data.getUnlockedSkills()) {
                    if (s != null && !s.isEmpty()) {
                        skillMgr.forceUnlockSkill(playerId, s.toLowerCase());
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("Failed to sync unlocked skills to SkillManager: " + e.getMessage());
            }
            return data;
        }
        
        // Create new player data
        return initializePlayer(playerId, playerName);
    }
    
    /**
     * Initializes new RPG data for a player.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @return the newly created player RPG data
     */
    public PlayerRPGData initializePlayer(UUID playerId, String playerName) {
        ensureInitialized();
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        String name = playerName != null ? playerName : "Unknown";
        PlayerRPGData data = new PlayerRPGData(playerId, name);
        
        // Apply starting level from config
        if (config != null && config.getStartingLevel() > 1) {
            data.setLevel(config.getStartingLevel());
        }
        
        playerDataCache.put(playerId, data);
        LOGGER.info("Initialized new RPG data for player: " + name);
        
        return data;
    }
    
    /**
     * Gets all cached player data.
     * Note: This returns a copy to prevent modification.
     *
     * @return a map of player UUIDs to their RPG data
     */
    public Map<UUID, PlayerRPGData> getAllPlayerData() {
        return Collections.unmodifiableMap(new HashMap<>(playerDataCache));
    }
}
