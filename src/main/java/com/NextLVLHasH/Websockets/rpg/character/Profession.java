package com.NextLVLHasH.Websockets.rpg.character;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a profession (trade/crafting specialization) in the RPG system.
 * <p>
 * A profession defines a secondary progression path for characters, focusing on
 * non-combat activities like crafting, gathering, or trading. Professions can
 * have class restrictions and provide bonuses to resource gathering/crafting.
 * </p>
 *
 * <p>Example professions: Blacksmith, Herbalist, Enchanter, Miner, Alchemist</p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class Profession {

    // ==================== Identity ====================

    /**
     * Unique identifier for this profession (e.g., "blacksmith", "herbalist").
     */
    @SerializedName("id")
    private final String id;

    /**
     * Human-readable display name (e.g., "Blacksmith", "Master Herbalist").
     */
    @SerializedName("display_name")
    private final String displayName;

    /**
     * Description of the profession and what it provides.
     */
    @SerializedName("description")
    private final String description;

    /**
     * Path to the profession icon for UI display.
     */
    @SerializedName("icon_path")
    private final String iconPath;

    // ==================== Restrictions ====================

    /**
     * List of class IDs that can choose this profession.
     * Empty list means all classes are allowed.
     */
    @SerializedName("allowed_classes")
    private final List<String> allowedClasses;

    // ==================== Bonuses ====================

    /**
     * Resource gathering/crafting bonuses.
     * Maps resource type to bonus percentage (e.g., "ore" -> 10 means +10% ore yield).
     */
    @SerializedName("resource_bonuses")
    private final Map<String, Integer> resourceBonuses;

    // ==================== Progression ====================

    /**
     * Maximum level this profession can reach.
     */
    @SerializedName("max_level")
    private final int maxLevel;

    // ==================== Constants ====================

    private static final int DEFAULT_MAX_LEVEL = 100;

    // Gson instance for JSON operations
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ==================== Constructor ====================

    /**
     * Creates a new Profession with all specified parameters.
     * Use {@link Builder} for easier construction.
     */
    private Profession(
            String id,
            String displayName,
            String description,
            String iconPath,
            List<String> allowedClasses,
            Map<String, Integer> resourceBonuses,
            int maxLevel) {

        this.id = Objects.requireNonNull(id, "Profession ID cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";

        this.allowedClasses = allowedClasses != null 
                ? new ArrayList<>(allowedClasses) 
                : new ArrayList<>();
        this.resourceBonuses = resourceBonuses != null 
                ? new HashMap<>(resourceBonuses) 
                : new HashMap<>();

        this.maxLevel = maxLevel > 0 ? maxLevel : DEFAULT_MAX_LEVEL;
    }

    // ==================== Identity Getters ====================

    /**
     * Gets the unique identifier for this profession.
     *
     * @return the profession ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the profession description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the path to the profession icon.
     *
     * @return the icon path
     */
    public String getIconPath() {
        return iconPath;
    }

    // ==================== Restriction Methods ====================

    /**
     * Gets the list of class IDs that can choose this profession.
     *
     * @return unmodifiable list of allowed class IDs (empty = all allowed)
     */
    public List<String> getAllowedClasses() {
        return Collections.unmodifiableList(allowedClasses);
    }

    /**
     * Checks if this profession has class restrictions.
     *
     * @return true if only certain classes can choose this profession
     */
    public boolean hasClassRestrictions() {
        return !allowedClasses.isEmpty();
    }

    /**
     * Checks if the specified class can choose this profession.
     *
     * @param classId the class ID to check
     * @return true if the class is allowed to choose this profession
     */
    public boolean isClassAllowed(String classId) {
        if (classId == null || classId.isBlank()) {
            return false;
        }
        // Empty list means all classes are allowed
        if (allowedClasses.isEmpty()) {
            return true;
        }
        return allowedClasses.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(classId.trim()));
    }

    // ==================== Resource Bonus Methods ====================

    /**
     * Gets all resource bonuses.
     *
     * @return unmodifiable map of resource bonuses
     */
    public Map<String, Integer> getResourceBonuses() {
        return Collections.unmodifiableMap(resourceBonuses);
    }

    /**
     * Gets the bonus percentage for a specific resource type.
     *
     * @param resourceType the resource type (e.g., "ore", "herb", "wood")
     * @return the bonus percentage, or 0 if no bonus exists
     */
    public int getResourceBonus(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return 0;
        }
        return resourceBonuses.getOrDefault(resourceType.toLowerCase(), 0);
    }

    /**
     * Checks if this profession provides a bonus for the specified resource.
     *
     * @param resourceType the resource type
     * @return true if a bonus exists
     */
    public boolean hasResourceBonus(String resourceType) {
        return getResourceBonus(resourceType) != 0;
    }

    /**
     * Calculates the effective yield after applying the resource bonus.
     *
     * @param resourceType the resource type
     * @param baseYield    the base yield amount
     * @return the yield after bonus is applied
     */
    public double calculateBonusYield(String resourceType, double baseYield) {
        int bonusPercent = getResourceBonus(resourceType);
        return baseYield * (1.0 + bonusPercent / 100.0);
    }

    /**
     * Calculates the effective yield at a specific profession level.
     * Bonuses scale with profession level (full bonus at max level).
     *
     * @param resourceType    the resource type
     * @param baseYield       the base yield amount
     * @param professionLevel the current profession level
     * @return the yield after level-scaled bonus is applied
     */
    public double calculateBonusYieldAtLevel(String resourceType, double baseYield, int professionLevel) {
        int bonusPercent = getResourceBonus(resourceType);
        double levelMultiplier = (double) Math.min(professionLevel, maxLevel) / maxLevel;
        double scaledBonus = bonusPercent * levelMultiplier;
        return baseYield * (1.0 + scaledBonus / 100.0);
    }

    // ==================== Progression Methods ====================

    /**
     * Gets the maximum level for this profession.
     *
     * @return the max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Checks if the given level is the maximum level.
     *
     * @param level the level to check
     * @return true if at max level
     */
    public boolean isMaxLevel(int level) {
        return level >= maxLevel;
    }

    /**
     * Calculates the progress percentage toward max level.
     *
     * @param currentLevel the current profession level
     * @return the progress percentage (0.0 to 1.0)
     */
    public double getLevelProgress(int currentLevel) {
        return Math.min(1.0, (double) currentLevel / maxLevel);
    }

    // ==================== JSON Serialization ====================

    /**
     * Serializes this Profession to a JSON string.
     *
     * @return JSON string representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes a Profession from a JSON string.
     *
     * @param json the JSON string
     * @return the deserialized Profession
     */
    public static Profession fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        return GSON.fromJson(json, Profession.class);
    }

    /**
     * Loads a Profession from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded Profession
     * @throws IOException if the file cannot be read
     */
    public static Profession fromFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, Profession.class);
        }
    }

    // ==================== Builder Pattern ====================

    /**
     * Creates a new Builder for constructing Profession instances.
     *
     * @param id the unique profession ID
     * @return a new Builder instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder class for constructing Profession instances.
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private String description;
        private String iconPath;
        private final List<String> allowedClasses = new ArrayList<>();
        private final Map<String, Integer> resourceBonuses = new HashMap<>();
        private int maxLevel = DEFAULT_MAX_LEVEL;

        /**
         * Creates a new Builder with the specified profession ID.
         *
         * @param id the unique profession ID
         */
        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Profession ID cannot be null");
        }

        /**
         * Sets the display name.
         *
         * @param displayName the display name
         * @return this builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the icon path.
         *
         * @param iconPath the icon path
         * @return this builder
         */
        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }

        /**
         * Adds an allowed class.
         *
         * @param classId the class ID to allow
         * @return this builder
         */
        public Builder allowClass(String classId) {
            if (classId != null && !classId.isBlank()) {
                allowedClasses.add(classId.trim().toLowerCase());
            }
            return this;
        }

        /**
         * Adds multiple allowed classes.
         *
         * @param classIds the class IDs to allow
         * @return this builder
         */
        public Builder allowClasses(String... classIds) {
            for (String classId : classIds) {
                allowClass(classId);
            }
            return this;
        }

        /**
         * Adds multiple allowed classes from a collection.
         *
         * @param classIds the class IDs to allow
         * @return this builder
         */
        public Builder allowClasses(Collection<String> classIds) {
            if (classIds != null) {
                classIds.forEach(this::allowClass);
            }
            return this;
        }

        /**
         * Adds a resource bonus.
         *
         * @param resourceType the resource type
         * @param bonusPercent the bonus percentage
         * @return this builder
         */
        public Builder resourceBonus(String resourceType, int bonusPercent) {
            if (resourceType != null && !resourceType.isBlank()) {
                resourceBonuses.put(resourceType.toLowerCase(), bonusPercent);
            }
            return this;
        }

        /**
         * Sets all resource bonuses from a map.
         *
         * @param bonuses the bonuses map
         * @return this builder
         */
        public Builder resourceBonuses(Map<String, Integer> bonuses) {
            if (bonuses != null) {
                bonuses.forEach(this::resourceBonus);
            }
            return this;
        }

        /**
         * Sets the maximum level.
         *
         * @param maxLevel the max level
         * @return this builder
         */
        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        /**
         * Builds the Profession instance.
         *
         * @return the constructed Profession
         */
        public Profession build() {
            return new Profession(
                    id,
                    displayName,
                    description,
                    iconPath,
                    allowedClasses,
                    resourceBonuses,
                    maxLevel
            );
        }
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        return "Profession{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", maxLevel=" + maxLevel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profession that = (Profession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
