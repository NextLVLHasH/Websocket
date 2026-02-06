package com.NextLVLHasH.Websockets.rpg.progression;

import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for handling player progression and leveling.
 * <p>
 * The ProgressionManager is responsible for:
 * <ul>
 *   <li>Loading and managing the active experience curve</li>
 *   <li>Adding experience to players and checking for level-ups</li>
 *   <li>Processing level-ups and applying stat increases</li>
 *   <li>Firing level-up events (placeholder for event system)</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class ProgressionManager {

    private static final Logger LOGGER = Logger.getLogger(ProgressionManager.class.getName());

    /**
     * Singleton instance.
     */
    private static ProgressionManager instance;

    /**
     * The active experience curve used for level calculations.
     */
    private ExperienceCurve activeCurve;

    /**
     * Factory for creating experience curves.
     */
    private final ExperienceCurveFactory curveFactory;

    /**
     * Path to the configuration directory.
     */
    @SuppressWarnings("unused")
    private Path configPath;

    /**
     * Cache of CharacterClass objects for modifier lookup.
     */
    private final Map<String, CharacterClass> classCache;

    /**
     * Callback for class lookups (to integrate with CharacterManager).
     */
    private java.util.function.Function<String, CharacterClass> classLookup;

    /**
     * Level-up event listeners (placeholder for event system).
     */
    private final java.util.List<BiConsumer<PlayerRPGData, Integer>> levelUpListeners;

    /**
     * Maximum level cap. 0 = no cap.
     */
    private int maxLevel = 100;

    /**
     * Whether the manager has been initialized.
     */
    private boolean initialized = false;

    /**
     * Private constructor for singleton pattern.
     */
    private ProgressionManager() {
        this.curveFactory = ExperienceCurveFactory.getInstance();
        this.classCache = new ConcurrentHashMap<>();
        this.levelUpListeners = new java.util.ArrayList<>();
        this.activeCurve = ExponentialCurve.createDefault();
    }

    /**
     * Gets the singleton instance of the ProgressionManager.
     *
     * @return the ProgressionManager instance
     */
    public static synchronized ProgressionManager getInstance() {
        if (instance == null) {
            instance = new ProgressionManager();
        }
        return instance;
    }

    /**
     * Initializes the ProgressionManager with default settings.
     * <p>
     * This loads the default experience curve from the factory.
     * Call this method during plugin initialization.
     * </p>
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warning("ProgressionManager already initialized");
            return;
        }

        try {
            activeCurve = curveFactory.loadDefaultCurve();
            LOGGER.info("ProgressionManager initialized with curve: " + activeCurve.getCurveType());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load default curve, using exponential fallback", e);
            activeCurve = ExponentialCurve.createDefault();
        }

        initialized = true;
    }

    /**
     * Initializes the ProgressionManager with a specified config path.
     *
     * @param configPath the path to the config directory
     */
    public void initialize(Path configPath) {
        if (initialized) {
            LOGGER.warning("ProgressionManager already initialized");
            return;
        }

        this.configPath = configPath;
        Path curvesFolder = configPath.resolve("experience_curves");

        // Create curves folder if it doesn't exist
        try {
            if (!Files.exists(curvesFolder)) {
                Files.createDirectories(curvesFolder);
                LOGGER.info("Created experience_curves config folder");
            }
            curveFactory.setConfigFolder(curvesFolder);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not create experience_curves folder", e);
        }

        initialize();
    }

    /**
     * Sets the active experience curve.
     *
     * @param curve the ExperienceCurve to use
     * @throws NullPointerException if curve is null
     */
    public void setActiveCurve(ExperienceCurve curve) {
        this.activeCurve = Objects.requireNonNull(curve, "Curve cannot be null");
        LOGGER.info("Active experience curve set to: " + curve.getCurveType());
    }

    /**
     * Gets the active experience curve.
     *
     * @return the active ExperienceCurve
     */
    public ExperienceCurve getActiveCurve() {
        return activeCurve;
    }

    /**
     * Sets the class lookup function for finding CharacterClass instances.
     *
     * @param classLookup function that takes a class ID and returns a CharacterClass
     */
    public void setClassLookup(java.util.function.Function<String, CharacterClass> classLookup) {
        this.classLookup = classLookup;
    }

    /**
     * Sets the maximum level cap.
     *
     * @param maxLevel the maximum level (0 = no cap)
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = Math.max(0, maxLevel);
    }

    /**
     * Gets the maximum level cap.
     *
     * @return the maximum level (0 = no cap)
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Adds experience to a player and processes any resulting level-ups.
     * <p>
     * This method handles:
     * <ul>
     *   <li>Adding the XP amount to the player's total</li>
     *   <li>Checking if the player has enough XP to level up</li>
     *   <li>Processing all level-ups and applying modifiers</li>
     *   <li>Updating the player's XP-to-next-level display value</li>
     *   <li>Firing level-up events</li>
     * </ul>
     * </p>
     *
     * @param data the PlayerRPGData to modify
     * @param amount the amount of XP to add (must be positive)
     * @return the number of levels gained
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if amount is negative
     */
    public int addExperience(PlayerRPGData data, long amount) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");
        if (amount < 0) {
            throw new IllegalArgumentException("XP amount cannot be negative");
        }
        if (amount == 0) {
            return 0;
        }

        // Check if at max level
        if (maxLevel > 0 && data.getLevel() >= maxLevel) {
            LOGGER.fine("Player " + data.getPlayerName() + " is at max level, XP not added");
            return 0;
        }

        // Add XP
        long newXP = data.getCurrentXP() + amount;
        data.setCurrentXP(newXP);

        LOGGER.fine("Added " + amount + " XP to " + data.getPlayerName() + " (total: " + newXP + ")");

        // Process any level-ups
        int levelsGained = processLevelUps(data);

        // Update XP to next level display
        data.setXpToNextLevel(getXPToNextLevel(data));

        return levelsGained;
    }

    /**
     * Checks if a player can level up with their current XP.
     *
     * @param data the PlayerRPGData to check
     * @return true if the player has enough XP to level up
     */
    public boolean canLevelUp(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        // Check max level
        if (maxLevel > 0 && data.getLevel() >= maxLevel) {
            return false;
        }

        long xpForNextLevel = activeCurve.calculateXPForLevel(data.getLevel() + 1);
        return data.getCurrentXP() >= xpForNextLevel;
    }

    /**
     * Processes all pending level-ups for a player.
     * <p>
     * This method continues to level up the player as long as they have
     * enough XP for the next level. Each level-up applies the appropriate
     * LevelUpModifier and fires a level-up event.
     * </p>
     *
     * @param data the PlayerRPGData to process
     * @return the number of levels gained
     */
    public int processLevelUps(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        int levelsGained = 0;
        int startLevel = data.getLevel();

        while (canLevelUp(data)) {
            int oldLevel = data.getLevel();
            int newLevel = oldLevel + 1;

            // Set new level
            data.setLevel(newLevel);
            levelsGained++;

            // Get and apply level-up modifier
            LevelUpModifier modifier = getModifierForLevel(data, newLevel);
            modifier.apply(data);

            // Fire level-up event (placeholder - just log for now)
            fireLevelUpEvent(data, newLevel);

            LOGGER.info("Player " + data.getPlayerName() + " leveled up! " + oldLevel + " → " + newLevel);

            // Safety check to prevent infinite loops
            if (levelsGained > 100) {
                LOGGER.warning("Level-up loop safety triggered for " + data.getPlayerName());
                break;
            }
        }

        if (levelsGained > 0) {
            LOGGER.info("Player " + data.getPlayerName() + " gained " + levelsGained + 
                       " level(s): " + startLevel + " → " + data.getLevel());
        }

        return levelsGained;
    }

    /**
     * Gets the appropriate LevelUpModifier for a player at a specific level.
     * <p>
     * This method checks the player's class to get class-specific modifiers.
     * If no class is set or found, returns a default modifier.
     * </p>
     *
     * @param data the PlayerRPGData
     * @param level the level being reached
     * @return the LevelUpModifier to apply
     */
    public LevelUpModifier getModifierForLevel(PlayerRPGData data, int level) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        // Try to get class-specific modifier
        String classId = data.getSelectedClass();
        if (classId != null && !classId.isEmpty()) {
            CharacterClass characterClass = getCharacterClass(classId);
            if (characterClass != null) {
                return LevelUpModifier.fromCharacterClass(characterClass);
            }
        }

        // Return default modifier
        return getDefaultModifier();
    }

    /**
     * Gets the level-up modifier for a character class at a specific level.
     *
     * @param characterClass the character class
     * @param level the new level being reached
     * @return the LevelUpModifier to apply
     */
    public LevelUpModifier getLevelUpModifier(CharacterClass characterClass, int level) {
        if (characterClass == null) {
            return LevelUpModifier.builder().build();
        }
        return LevelUpModifier.fromCharacterClass(characterClass, level);
    }

    /**
     * Gets the level-up modifier for a character class by ID.
     *
     * @param classId the character class ID
     * @return the LevelUpModifier to apply, or default modifier if class not found
     */
    public LevelUpModifier getLevelUpModifier(String classId) {
        if (classId == null || classId.isEmpty()) {
            return getDefaultModifier();
        }
        CharacterClass characterClass = getCharacterClass(classId);
        if (characterClass == null) {
            return getDefaultModifier();
        }
        return LevelUpModifier.fromCharacterClass(characterClass);
    }

    /**
     * Gets a CharacterClass by ID from cache or lookup function.
     *
     * @param classId the class ID
     * @return the CharacterClass, or null if not found
     */
    private CharacterClass getCharacterClass(String classId) {
        // Check cache first
        CharacterClass cached = classCache.get(classId.toLowerCase());
        if (cached != null) {
            return cached;
        }

        // Try lookup function
        if (classLookup != null) {
            CharacterClass found = classLookup.apply(classId);
            if (found != null) {
                classCache.put(classId.toLowerCase(), found);
                return found;
            }
        }

        return null;
    }

    /**
     * Creates a default LevelUpModifier for players without a class.
     *
     * @return a default LevelUpModifier
     */
    private LevelUpModifier getDefaultModifier() {
        return LevelUpModifier.builder()
                .healthIncrease(10.0)
                .manaIncrease(5.0)
                .staminaIncrease(5.0)
                .healthRegenIncrease(0.1)
                .manaRegenIncrease(0.1)
                .staminaRegenIncrease(0.2)
                .skillPointsAwarded(1)
                .build();
    }

    /**
     * Gets the total XP required to reach a specific level.
     *
     * @param level the target level
     * @return the total XP required
     */
    public long getXPForLevel(int level) {
        return activeCurve.calculateXPForLevel(level);
    }

    /**
     * Gets the XP needed for a player to reach the next level.
     *
     * @param data the PlayerRPGData
     * @return the XP remaining until next level
     */
    public long getXPToNextLevel(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");
        
        // If at max level, return 0
        if (maxLevel > 0 && data.getLevel() >= maxLevel) {
            return 0;
        }

        return activeCurve.getXPToNextLevel(data.getLevel(), data.getCurrentXP());
    }

    /**
     * Gets the XP progress percentage toward the next level.
     *
     * @param data the PlayerRPGData
     * @return progress as a percentage (0.0 to 100.0)
     */
    public double getProgressPercentage(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        if (maxLevel > 0 && data.getLevel() >= maxLevel) {
            return 100.0;
        }

        long currentLevelXP = activeCurve.calculateXPForLevel(data.getLevel());
        long nextLevelXP = activeCurve.calculateXPForLevel(data.getLevel() + 1);
        long xpInCurrentLevel = data.getCurrentXP() - currentLevelXP;
        long xpNeededForNextLevel = nextLevelXP - currentLevelXP;

        if (xpNeededForNextLevel <= 0) {
            return 100.0;
        }

        return Math.min(100.0, (double) xpInCurrentLevel / xpNeededForNextLevel * 100.0);
    }

    /**
     * Calculates what level a player would be with a given total XP.
     *
     * @param totalXP the total XP
     * @return the calculated level
     */
    public int calculateLevelFromXP(long totalXP) {
        int level = activeCurve.calculateLevelFromXP(totalXP);
        if (maxLevel > 0) {
            level = Math.min(level, maxLevel);
        }
        return level;
    }

    /**
     * Registers a CharacterClass in the cache.
     *
     * @param characterClass the CharacterClass to register
     */
    public void registerCharacterClass(CharacterClass characterClass) {
        Objects.requireNonNull(characterClass, "CharacterClass cannot be null");
        classCache.put(characterClass.getId().toLowerCase(), characterClass);
    }

    /**
     * Adds a level-up event listener.
     * <p>
     * Listeners receive the PlayerRPGData and the new level when a player levels up.
     * </p>
     *
     * @param listener the listener to add
     */
    public void addLevelUpListener(BiConsumer<PlayerRPGData, Integer> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        levelUpListeners.add(listener);
    }

    /**
     * Removes a level-up event listener.
     *
     * @param listener the listener to remove
     */
    public void removeLevelUpListener(BiConsumer<PlayerRPGData, Integer> listener) {
        levelUpListeners.remove(listener);
    }

    /**
     * Fires a level-up event to all registered listeners.
     *
     * @param data the PlayerRPGData
     * @param newLevel the new level reached
     */
    private void fireLevelUpEvent(PlayerRPGData data, int newLevel) {
        // Log the event (placeholder for actual event system)
        LOGGER.info("[LEVEL UP EVENT] Player: " + data.getPlayerName() + 
                   ", New Level: " + newLevel + 
                   ", Total XP: " + data.getCurrentXP());

        // Notify all listeners
        for (BiConsumer<PlayerRPGData, Integer> listener : levelUpListeners) {
            try {
                listener.accept(data, newLevel);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Level-up listener threw exception", e);
            }
        }
    }

    /**
     * Shuts down the ProgressionManager and clears caches.
     */
    public void shutdown() {
        classCache.clear();
        levelUpListeners.clear();
        curveFactory.clearCache();
        initialized = false;
        LOGGER.info("ProgressionManager shut down");
    }

    /**
     * Checks if the manager has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
