package com.NextLVLHasH.Websockets.rpg.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration class for the RPG system.
 * Uses the Singleton pattern to ensure only one configuration instance exists.
 * Supports loading from and saving to JSON files.
 */
public class RPGConfig {

    private static final Logger LOGGER = Logger.getLogger(RPGConfig.class.getName());
    private static volatile RPGConfig instance;
    private static final Object LOCK = new Object();

    // Transient fields (not serialized)
    private transient Path configFilePath;
    private transient Gson gson;

    // Configuration fields
    @SerializedName("enabled")
    private boolean enabled = true;

    @SerializedName("starting_level")
    private int startingLevel = 1;

    @SerializedName("max_level")
    private int maxLevel = 100;

    @SerializedName("skill_points_per_level")
    private int skillPointsPerLevel = 1;

    @SerializedName("allow_respec")
    private boolean allowRespec = true;

    @SerializedName("respec_cost")
    private int respecCost = 100;

    @SerializedName("auto_save_interval")
    private int autoSaveInterval = 300; // seconds

    @SerializedName("experience_curve")
    private String experienceCurve = "exponential";

    @SerializedName("enable_custom_race_models")
    private boolean enableCustomRaceModels = true;

    @SerializedName("combat_log_enabled")
    private boolean combatLogEnabled = true;

    @SerializedName("permission_integration")
    private boolean permissionIntegration = true;

    @SerializedName("debug")
    private boolean debug = false;

    /**
     * Private constructor for singleton pattern.
     * Initializes default values.
     */
    private RPGConfig() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }

    /**
     * Gets the singleton instance of RPGConfig.
     * Creates a new instance with default values if none exists.
     *
     * @return the singleton RPGConfig instance
     */
    public static RPGConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new RPGConfig();
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
            instance = null;
        }
    }

    /**
     * Loads configuration from a JSON file.
     * If the file doesn't exist, creates a new file with default values.
     *
     * @param filePath the path to the configuration file
     * @return the loaded RPGConfig instance
     * @throws IOException if there's an error reading the file
     */
    public static RPGConfig loadFromFile(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        synchronized (LOCK) {
            if (Files.exists(filePath)) {
                try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    RPGConfig loaded = gson.fromJson(reader, RPGConfig.class);
                    if (loaded != null) {
                        loaded.configFilePath = filePath;
                        loaded.gson = gson;
                        instance = loaded;
                        LOGGER.info("RPG configuration loaded from: " + filePath);
                    } else {
                        instance = new RPGConfig();
                        instance.configFilePath = filePath;
                        instance.saveToFile();
                        LOGGER.warning("Configuration file was empty, created defaults: " + filePath);
                    }
                }
            } else {
                // Create new config with defaults
                instance = new RPGConfig();
                instance.configFilePath = filePath;
                
                // Ensure parent directories exist
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                
                instance.saveToFile();
                LOGGER.info("Created default RPG configuration: " + filePath);
            }
            return instance;
        }
    }

    /**
     * Loads configuration from a JSON string.
     *
     * @param json the JSON string to parse
     * @return the loaded RPGConfig instance
     */
    public static RPGConfig loadFromJson(String json) {
        Objects.requireNonNull(json, "JSON string cannot be null");

        synchronized (LOCK) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            RPGConfig loaded = gson.fromJson(json, RPGConfig.class);
            if (loaded != null) {
                loaded.gson = gson;
                instance = loaded;
            } else {
                instance = new RPGConfig();
            }
            return instance;
        }
    }

    /**
     * Saves the current configuration to the configured file path.
     *
     * @throws IOException          if there's an error writing the file
     * @throws IllegalStateException if no file path has been configured
     */
    public void saveToFile() throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("No configuration file path set. Use saveToFile(Path) instead.");
        }
        saveToFile(configFilePath);
    }

    /**
     * Saves the current configuration to the specified file path.
     *
     * @param filePath the path to save to
     * @throws IOException if there's an error writing the file
     */
    public void saveToFile(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        // Ensure parent directories exist
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            if (gson == null) {
                gson = new GsonBuilder().setPrettyPrinting().create();
            }
            gson.toJson(this, writer);
        }

        this.configFilePath = filePath;
        LOGGER.fine("RPG configuration saved to: " + filePath);
    }

    /**
     * Converts the configuration to a JSON string.
     *
     * @return the JSON representation of this configuration
     */
    public String toJson() {
        if (gson == null) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        return gson.toJson(this);
    }

    /**
     * Reloads the configuration from the configured file path.
     *
     * @throws IOException           if there's an error reading the file
     * @throws IllegalStateException if no file path has been configured
     */
    public void reload() throws IOException {
        if (configFilePath == null) {
            throw new IllegalStateException("No configuration file path set.");
        }
        loadFromFile(configFilePath);
        LOGGER.info("RPG configuration reloaded.");
    }

    // ==================== Getters and Setters ====================

    /**
     * Checks if the RPG system is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the RPG system is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the starting level for new characters.
     *
     * @return the starting level
     */
    public int getStartingLevel() {
        return startingLevel;
    }

    /**
     * Sets the starting level for new characters.
     *
     * @param startingLevel the starting level (minimum 1)
     */
    public void setStartingLevel(int startingLevel) {
        this.startingLevel = Math.max(1, startingLevel);
    }

    /**
     * Gets the maximum achievable level.
     *
     * @return the max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Sets the maximum achievable level.
     *
     * @param maxLevel the max level
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = Math.max(1, maxLevel);
    }

    /**
     * Gets the number of skill points awarded per level.
     *
     * @return skill points per level
     */
    public int getSkillPointsPerLevel() {
        return skillPointsPerLevel;
    }

    /**
     * Sets the number of skill points awarded per level.
     *
     * @param skillPointsPerLevel skill points per level
     */
    public void setSkillPointsPerLevel(int skillPointsPerLevel) {
        this.skillPointsPerLevel = Math.max(0, skillPointsPerLevel);
    }

    /**
     * Checks if respec (skill/attribute reset) is allowed.
     *
     * @return true if respec is allowed
     */
    public boolean isAllowRespec() {
        return allowRespec;
    }

    /**
     * Sets whether respec is allowed.
     *
     * @param allowRespec true to allow
     */
    public void setAllowRespec(boolean allowRespec) {
        this.allowRespec = allowRespec;
    }

    /**
     * Gets the cost of respec (in currency or points).
     *
     * @return the respec cost
     */
    public int getRespecCost() {
        return respecCost;
    }

    /**
     * Sets the cost of respec.
     *
     * @param respecCost the respec cost
     */
    public void setRespecCost(int respecCost) {
        this.respecCost = Math.max(0, respecCost);
    }

    /**
     * Gets the auto-save interval in seconds.
     *
     * @return the auto-save interval
     */
    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    /**
     * Sets the auto-save interval in seconds.
     *
     * @param autoSaveInterval the interval (minimum 30 seconds)
     */
    public void setAutoSaveInterval(int autoSaveInterval) {
        this.autoSaveInterval = Math.max(30, autoSaveInterval);
    }

    /**
     * Gets the experience curve type.
     *
     * @return the experience curve (e.g., "linear", "exponential", "logarithmic")
     */
    public String getExperienceCurve() {
        return experienceCurve;
    }

    /**
     * Sets the experience curve type.
     *
     * @param experienceCurve the curve type
     */
    public void setExperienceCurve(String experienceCurve) {
        this.experienceCurve = experienceCurve != null ? experienceCurve : "exponential";
    }

    /**
     * Checks if custom race models are enabled.
     *
     * @return true if enabled
     */
    public boolean isEnableCustomRaceModels() {
        return enableCustomRaceModels;
    }

    /**
     * Sets whether custom race models are enabled.
     *
     * @param enableCustomRaceModels true to enable
     */
    public void setEnableCustomRaceModels(boolean enableCustomRaceModels) {
        this.enableCustomRaceModels = enableCustomRaceModels;
    }

    /**
     * Checks if combat logging is enabled.
     *
     * @return true if enabled
     */
    public boolean isCombatLogEnabled() {
        return combatLogEnabled;
    }

    /**
     * Sets whether combat logging is enabled.
     *
     * @param combatLogEnabled true to enable
     */
    public void setCombatLogEnabled(boolean combatLogEnabled) {
        this.combatLogEnabled = combatLogEnabled;
    }

    /**
     * Checks if permission system integration is enabled.
     *
     * @return true if enabled
     */
    public boolean isPermissionIntegration() {
        return permissionIntegration;
    }

    /**
     * Sets whether permission system integration is enabled.
     *
     * @param permissionIntegration true to enable
     */
    public void setPermissionIntegration(boolean permissionIntegration) {
        this.permissionIntegration = permissionIntegration;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is on
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Sets whether debug mode is enabled.
     *
     * @param debug true to enable debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Gets the configured file path, if any.
     *
     * @return the file path, or null if not set
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Logs a debug message if debug mode is enabled.
     *
     * @param message the message to log
     */
    public void logDebug(String message) {
        if (debug && message != null) {
            LOGGER.log(Level.INFO, "[RPG-DEBUG] " + message);
        }
    }

    @Override
    public String toString() {
        return "RPGConfig{" +
                "enabled=" + enabled +
                ", startingLevel=" + startingLevel +
                ", maxLevel=" + maxLevel +
                ", skillPointsPerLevel=" + skillPointsPerLevel +
                ", allowRespec=" + allowRespec +
                ", respecCost=" + respecCost +
                ", autoSaveInterval=" + autoSaveInterval +
                ", experienceCurve='" + experienceCurve + '\'' +
                ", debug=" + debug +
                '}';
    }
}
