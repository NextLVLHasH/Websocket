package com.NextLVLHasH.Websockets.rpg.progression;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class for creating and managing ExperienceCurve instances.
 * <p>
 * This factory supports loading experience curves from JSON configuration files
 * and provides methods to create curves programmatically. It handles all curve
 * types: linear, exponential, polynomial, and custom.
 * </p>
 * <p>
 * Configuration files are loaded from the config/experience_curves/ folder
 * and should be named with the curve identifier (e.g., "default.json").
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class ExperienceCurveFactory {

    private static final Logger LOGGER = Logger.getLogger(ExperienceCurveFactory.class.getName());

    /**
     * Gson instance for JSON parsing.
     */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Cache of loaded curves by name.
     */
    private final Map<String, ExperienceCurve> curveCache;

    /**
     * Path to the experience curves configuration folder.
     */
    private Path configFolder;

    /**
     * Singleton instance.
     */
    private static ExperienceCurveFactory instance;

    /**
     * Private constructor for singleton pattern.
     */
    private ExperienceCurveFactory() {
        this.curveCache = new HashMap<>();
    }

    /**
     * Gets the singleton instance of the factory.
     *
     * @return the ExperienceCurveFactory instance
     */
    public static synchronized ExperienceCurveFactory getInstance() {
        if (instance == null) {
            instance = new ExperienceCurveFactory();
        }
        return instance;
    }

    /**
     * Sets the configuration folder path for loading curve files.
     *
     * @param configFolder the path to the config/experience_curves folder
     */
    public void setConfigFolder(Path configFolder) {
        this.configFolder = configFolder;
    }

    /**
     * Creates an ExperienceCurve from a curve type and configuration.
     * <p>
     * Supported types:
     * <ul>
     *   <li>"linear" - requires "baseXP" and "increment" in config</li>
     *   <li>"exponential" - requires "baseXP" and "multiplier" in config</li>
     *   <li>"polynomial" - requires "a", "power", "b", "c" in config</li>
     *   <li>"custom" - requires "breakpoints" array in config</li>
     * </ul>
     * </p>
     *
     * @param curveType the type of curve ("linear", "exponential", "polynomial", "custom")
     * @param config the JSON configuration object
     * @return the created ExperienceCurve
     * @throws IllegalArgumentException if curveType is unknown or config is invalid
     */
    public ExperienceCurve create(String curveType, JsonObject config) {
        Objects.requireNonNull(curveType, "Curve type cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        return switch (curveType.toLowerCase()) {
            case "linear" -> createLinear(config);
            case "exponential" -> createExponential(config);
            case "polynomial" -> createPolynomial(config);
            case "custom" -> createCustom(config);
            default -> throw new IllegalArgumentException("Unknown curve type: " + curveType);
        };
    }

    /**
     * Creates a LinearCurve from JSON configuration.
     *
     * @param config JSON with "baseXP" and "increment" fields
     * @return the LinearCurve
     */
    private ExperienceCurve createLinear(JsonObject config) {
        long baseXP = config.has("baseXP") ? config.get("baseXP").getAsLong() : 100;
        long increment = config.has("increment") ? config.get("increment").getAsLong() : 50;

        // Support alternative field names
        if (config.has("base_xp")) {
            baseXP = config.get("base_xp").getAsLong();
        }

        return new LinearCurve(baseXP, increment);
    }

    /**
     * Creates an ExponentialCurve from JSON configuration.
     *
     * @param config JSON with "baseXP" and "multiplier" fields
     * @return the ExponentialCurve
     */
    private ExperienceCurve createExponential(JsonObject config) {
        long baseXP = config.has("baseXP") ? config.get("baseXP").getAsLong() : 100;
        double multiplier = config.has("multiplier") ? config.get("multiplier").getAsDouble() : 1.5;

        // Support alternative field names
        if (config.has("base_xp")) {
            baseXP = config.get("base_xp").getAsLong();
        }

        return new ExponentialCurve(baseXP, multiplier);
    }

    /**
     * Creates a PolynomialCurve from JSON configuration.
     *
     * @param config JSON with "a", "power", "b", "c" fields
     * @return the PolynomialCurve
     */
    private ExperienceCurve createPolynomial(JsonObject config) {
        double a = config.has("a") ? config.get("a").getAsDouble() : 10;
        double power = config.has("power") ? config.get("power").getAsDouble() : 2;
        double b = config.has("b") ? config.get("b").getAsDouble() : 50;
        double c = config.has("c") ? config.get("c").getAsDouble() : 40;

        // Support coefficient naming
        if (config.has("coefficient_a")) {
            a = config.get("coefficient_a").getAsDouble();
        }
        if (config.has("coefficient_b")) {
            b = config.get("coefficient_b").getAsDouble();
        }
        if (config.has("constant")) {
            c = config.get("constant").getAsDouble();
        }
        if (config.has("exponent")) {
            power = config.get("exponent").getAsDouble();
        }

        return new PolynomialCurve(a, power, b, c);
    }

    /**
     * Creates a CustomCurve from JSON configuration.
     *
     * @param config JSON with "breakpoints" array or object
     * @return the CustomCurve
     */
    private ExperienceCurve createCustom(JsonObject config) {
        if (config.has("breakpoints")) {
            if (config.get("breakpoints").isJsonArray()) {
                JsonArray breakpoints = config.getAsJsonArray("breakpoints");
                return CustomCurve.fromJsonArray(breakpoints);
            } else if (config.get("breakpoints").isJsonObject()) {
                JsonObject breakpoints = config.getAsJsonObject("breakpoints");
                return CustomCurve.fromJsonObject(breakpoints);
            }
        }

        // Try to parse config itself as breakpoints
        if (config.has("1") || config.has("level")) {
            return CustomCurve.fromJsonObject(config);
        }

        throw new IllegalArgumentException("Custom curve requires 'breakpoints' array or object");
    }

    /**
     * Loads an ExperienceCurve from a JSON file.
     *
     * @param filePath the path to the JSON file
     * @return the loaded ExperienceCurve
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if file format is invalid
     */
    public ExperienceCurve loadFromFile(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            if (json == null) {
                throw new IllegalArgumentException("Empty or invalid JSON file: " + filePath);
            }

            String curveType = json.has("type") ? json.get("type").getAsString() : "linear";
            
            // If type field not present, try to infer from contents
            if (!json.has("type")) {
                if (json.has("breakpoints")) {
                    curveType = "custom";
                } else if (json.has("multiplier")) {
                    curveType = "exponential";
                } else if (json.has("power") || json.has("a")) {
                    curveType = "polynomial";
                }
            }

            return create(curveType, json);
        }
    }

    /**
     * Loads a named curve from the config folder.
     *
     * @param curveName the name of the curve (without .json extension)
     * @return the loaded ExperienceCurve
     * @throws IOException if file reading fails
     * @throws IllegalStateException if config folder is not set
     */
    public ExperienceCurve loadCurve(String curveName) throws IOException {
        if (configFolder == null) {
            throw new IllegalStateException("Config folder not set. Call setConfigFolder() first.");
        }

        // Check cache first
        if (curveCache.containsKey(curveName)) {
            return curveCache.get(curveName);
        }

        Path curveFile = configFolder.resolve(curveName + ".json");
        
        if (!Files.exists(curveFile)) {
            throw new IOException("Curve file not found: " + curveFile);
        }

        ExperienceCurve curve = loadFromFile(curveFile);
        curveCache.put(curveName, curve);
        
        LOGGER.info("Loaded experience curve: " + curveName);
        return curve;
    }

    /**
     * Loads the default experience curve.
     * <p>
     * Tries to load "default.json" from config folder, falls back to
     * a built-in exponential curve if not found.
     * </p>
     *
     * @return the default ExperienceCurve
     */
    public ExperienceCurve loadDefaultCurve() {
        try {
            if (configFolder != null) {
                return loadCurve("default");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load default curve from config, using built-in default", e);
        }

        // Return built-in default
        return ExponentialCurve.createDefault();
    }

    /**
     * Registers a curve in the cache with a specific name.
     *
     * @param name the name to register the curve under
     * @param curve the ExperienceCurve to register
     */
    public void registerCurve(String name, ExperienceCurve curve) {
        Objects.requireNonNull(name, "Curve name cannot be null");
        Objects.requireNonNull(curve, "Curve cannot be null");
        curveCache.put(name, curve);
    }

    /**
     * Gets a registered curve by name.
     *
     * @param name the curve name
     * @return the ExperienceCurve, or null if not registered
     */
    public ExperienceCurve getCurve(String name) {
        return curveCache.get(name);
    }

    /**
     * Clears the curve cache.
     */
    public void clearCache() {
        curveCache.clear();
    }

    /**
     * Gets all registered curve names.
     *
     * @return set of curve names
     */
    public java.util.Set<String> getRegisteredCurveNames() {
        return java.util.Collections.unmodifiableSet(curveCache.keySet());
    }

    /**
     * Creates a sample configuration JSON for a curve type.
     *
     * @param curveType the curve type
     * @return a sample JsonObject
     */
    public JsonObject createSampleConfig(String curveType) {
        JsonObject config = new JsonObject();
        config.addProperty("type", curveType);

        switch (curveType.toLowerCase()) {
            case "linear" -> {
                config.addProperty("baseXP", 100);
                config.addProperty("increment", 50);
            }
            case "exponential" -> {
                config.addProperty("baseXP", 100);
                config.addProperty("multiplier", 1.5);
            }
            case "polynomial" -> {
                config.addProperty("a", 10);
                config.addProperty("power", 2);
                config.addProperty("b", 50);
                config.addProperty("c", 40);
            }
            case "custom" -> {
                JsonObject breakpoints = new JsonObject();
                breakpoints.addProperty("1", 100);
                breakpoints.addProperty("5", 500);
                breakpoints.addProperty("10", 1500);
                breakpoints.addProperty("20", 5000);
                breakpoints.addProperty("50", 25000);
                breakpoints.addProperty("100", 100000);
                config.add("breakpoints", breakpoints);
            }
        }

        return config;
    }
}
