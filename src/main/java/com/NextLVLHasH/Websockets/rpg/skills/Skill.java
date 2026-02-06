package com.NextLVLHasH.Websockets.rpg.skills;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a skill definition in the RPG system.
 * <p>
 * A Skill defines all the properties of an ability that a player can learn and use,
 * including its basic information, costs, cooldowns, targeting, and effect data.
 * Skills are typically loaded from JSON configuration files.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Skill fireball = Skill.builder()
 *     .id("fireball")
 *     .displayName("Fireball")
 *     .description("Hurls a ball of fire at the target")
 *     .type(SkillType.ACTIVE)
 *     .targetType(TargetType.ENEMY)
 *     .manaCost(25.0)
 *     .cooldownMillis(5000)
 *     .range(30.0)
 *     .scalingAttribute(AttributeType.INTELLIGENCE, 1.5)
 *     .build();
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class Skill {

    // ==================== Identity ====================

    /**
     * Unique identifier for this skill (e.g., "fireball", "power_strike").
     */
    @SerializedName("id")
    private final String id;

    /**
     * Human-readable display name (e.g., "Fireball", "Power Strike").
     */
    @SerializedName("display_name")
    private final String displayName;

    /**
     * Description of the skill and its effects.
     */
    @SerializedName("description")
    private final String description;

    /**
     * Path to the skill icon for UI display.
     */
    @SerializedName("icon_path")
    private final String iconPath;

    // ==================== Classification ====================

    /**
     * The type of skill (ACTIVE, PASSIVE, TOGGLE, ULTIMATE).
     */
    @SerializedName("type")
    private final SkillType type;

    /**
     * The targeting type for this skill.
     */
    @SerializedName("target_type")
    private final TargetType targetType;

    // ==================== Requirements ====================

    /**
     * Minimum character level required to learn this skill.
     */
    @SerializedName("required_level")
    private final int requiredLevel;

    /**
     * List of skill IDs that must be learned before this skill.
     */
    @SerializedName("prerequisite_skills")
    private final List<String> prerequisiteSkills;

    /**
     * Number of skill points required to unlock this skill.
     */
    @SerializedName("skill_point_cost")
    private final int skillPointCost;

    /**
     * List of permissions required to use this skill.
     */
    @SerializedName("required_permissions")
    private final List<String> requiredPermissions;

    // ==================== Costs & Cooldown ====================

    /**
     * Cooldown time in milliseconds before the skill can be used again.
     */
    @SerializedName("cooldown_millis")
    private final long cooldownMillis;

    /**
     * Mana cost to activate this skill.
     */
    @SerializedName("mana_cost")
    private final double manaCost;

    /**
     * Stamina cost to activate this skill.
     */
    @SerializedName("stamina_cost")
    private final double staminaCost;

    // ==================== Casting ====================

    /**
     * Cast time in milliseconds (0 for instant cast).
     */
    @SerializedName("cast_time_millis")
    private final int castTimeMillis;

    /**
     * Maximum range of the skill in blocks/units.
     */
    @SerializedName("range")
    private final double range;

    // ==================== Effects ====================

    /**
     * Custom effect data for skill execution.
     * Contains skill-specific parameters like damage values, durations, etc.
     */
    @SerializedName("effect_data")
    private final Map<String, Object> effectData;

    /**
     * Attributes that affect this skill's power and their scaling factors.
     * For example, INTELLIGENCE with 1.5 means 50% bonus from INT.
     */
    @SerializedName("scaling_attributes")
    private final Map<AttributeType, Double> scalingAttributes;

    // ==================== GSON Configuration ====================

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ==================== Constructor ====================

    /**
     * Private constructor - use Builder or fromJson() to create instances.
     */
    private Skill(
            String id,
            String displayName,
            String description,
            String iconPath,
            SkillType type,
            TargetType targetType,
            int requiredLevel,
            List<String> prerequisiteSkills,
            int skillPointCost,
            List<String> requiredPermissions,
            long cooldownMillis,
            double manaCost,
            double staminaCost,
            int castTimeMillis,
            double range,
            Map<String, Object> effectData,
            Map<AttributeType, Double> scalingAttributes) {

        this.id = Objects.requireNonNull(id, "Skill id cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";
        this.type = type != null ? type : SkillType.ACTIVE;
        this.targetType = targetType != null ? targetType : TargetType.NONE;
        this.requiredLevel = Math.max(1, requiredLevel);
        this.prerequisiteSkills = prerequisiteSkills != null
                ? Collections.unmodifiableList(new ArrayList<>(prerequisiteSkills))
                : Collections.emptyList();
        this.skillPointCost = Math.max(0, skillPointCost);
        this.requiredPermissions = requiredPermissions != null
                ? Collections.unmodifiableList(new ArrayList<>(requiredPermissions))
                : Collections.emptyList();
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.manaCost = Math.max(0, manaCost);
        this.staminaCost = Math.max(0, staminaCost);
        this.castTimeMillis = Math.max(0, castTimeMillis);
        this.range = Math.max(0, range);
        this.effectData = effectData != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(effectData))
                : Collections.emptyMap();
        this.scalingAttributes = scalingAttributes != null
                ? Collections.unmodifiableMap(new EnumMap<>(scalingAttributes))
                : Collections.emptyMap();
    }

    // ==================== Getters ====================

    /**
     * Gets the unique identifier of this skill.
     *
     * @return the skill ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable display name of this skill.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this skill.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the icon path for UI display.
     *
     * @return the icon path
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Gets the skill type (ACTIVE, PASSIVE, TOGGLE, ULTIMATE).
     *
     * @return the skill type
     */
    public SkillType getType() {
        return type;
    }

    /**
     * Gets the target type for this skill.
     *
     * @return the target type
     */
    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * Gets the minimum level required to learn this skill.
     *
     * @return the required level
     */
    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Gets the list of prerequisite skill IDs.
     *
     * @return unmodifiable list of prerequisite skill IDs
     */
    public List<String> getPrerequisiteSkills() {
        return prerequisiteSkills;
    }

    /**
     * Gets the skill point cost to unlock this skill.
     *
     * @return the skill point cost
     */
    public int getSkillPointCost() {
        return skillPointCost;
    }

    /**
     * Gets the list of required permissions.
     *
     * @return unmodifiable list of required permissions
     */
    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    /**
     * Gets the cooldown time in milliseconds.
     *
     * @return the cooldown in milliseconds
     */
    public long getCooldownMillis() {
        return cooldownMillis;
    }

    /**
     * Gets the cooldown time in seconds.
     *
     * @return the cooldown in seconds
     */
    public double getCooldownSeconds() {
        return cooldownMillis / 1000.0;
    }

    /**
     * Gets the mana cost of this skill.
     *
     * @return the mana cost
     */
    public double getManaCost() {
        return manaCost;
    }

    /**
     * Gets the stamina cost of this skill.
     *
     * @return the stamina cost
     */
    public double getStaminaCost() {
        return staminaCost;
    }

    /**
     * Gets the cast time in milliseconds.
     *
     * @return the cast time in milliseconds
     */
    public int getCastTimeMillis() {
        return castTimeMillis;
    }

    /**
     * Gets the cast time in seconds.
     *
     * @return the cast time in seconds
     */
    public double getCastTimeSeconds() {
        return castTimeMillis / 1000.0;
    }

    /**
     * Checks if this skill is instant cast (no cast time).
     *
     * @return true if the skill has no cast time
     */
    public boolean isInstantCast() {
        return castTimeMillis == 0;
    }

    /**
     * Gets the maximum range of this skill.
     *
     * @return the range in blocks/units
     */
    public double getRange() {
        return range;
    }

    /**
     * Gets the effect data map containing skill-specific parameters.
     *
     * @return unmodifiable map of effect data
     */
    public Map<String, Object> getEffectData() {
        return effectData;
    }

    /**
     * Gets a specific effect data value.
     *
     * @param key the key to look up
     * @param defaultValue the default value if key not found
     * @param <T> the expected type
     * @return the value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getEffectData(String key, T defaultValue) {
        Object value = effectData.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the scaling attributes and their multipliers.
     *
     * @return unmodifiable map of scaling attributes
     */
    public Map<AttributeType, Double> getScalingAttributes() {
        return scalingAttributes;
    }

    /**
     * Gets the scaling multiplier for a specific attribute.
     *
     * @param type the attribute type
     * @return the scaling multiplier, or 0 if not scaled
     */
    public double getScaling(AttributeType type) {
        return scalingAttributes.getOrDefault(type, 0.0);
    }

    /**
     * Checks if this skill scales with the given attribute.
     *
     * @param type the attribute type
     * @return true if the skill scales with this attribute
     */
    public boolean scalesWith(AttributeType type) {
        return scalingAttributes.containsKey(type) && scalingAttributes.get(type) > 0;
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if a player meets the level requirement for this skill.
     *
     * @param playerLevel the player's current level
     * @return true if the player meets the level requirement
     */
    public boolean meetsLevelRequirement(int playerLevel) {
        return playerLevel >= requiredLevel;
    }

    /**
     * Checks if a player has all prerequisite skills.
     *
     * @param learnedSkills set of skill IDs the player has learned
     * @return true if all prerequisites are met
     */
    public boolean meetsPrerequisites(Set<String> learnedSkills) {
        if (prerequisiteSkills.isEmpty()) {
            return true;
        }
        return learnedSkills.containsAll(prerequisiteSkills);
    }

    /**
     * Checks if a player has enough skill points.
     *
     * @param availablePoints the player's available skill points
     * @return true if the player has enough points
     */
    public boolean hasEnoughPoints(int availablePoints) {
        return availablePoints >= skillPointCost;
    }

    /**
     * Checks if a player has the required permissions.
     *
     * @param playerPermissions set of permissions the player has
     * @return true if all required permissions are met
     */
    public boolean hasRequiredPermissions(Set<String> playerPermissions) {
        if (requiredPermissions.isEmpty()) {
            return true;
        }
        return playerPermissions.containsAll(requiredPermissions);
    }

    /**
     * Checks if a player can afford the resource cost of this skill.
     *
     * @param currentMana the player's current mana
     * @param currentStamina the player's current stamina
     * @return true if the player can afford the cost
     */
    public boolean canAfford(double currentMana, double currentStamina) {
        return currentMana >= manaCost && currentStamina >= staminaCost;
    }

    /**
     * Calculates the scaled power of this skill based on attribute values.
     *
     * @param attributeValues map of attribute types to their values
     * @param basePower the base power of the skill effect
     * @return the scaled power value
     */
    public double calculateScaledPower(Map<AttributeType, Integer> attributeValues, double basePower) {
        double totalMultiplier = 1.0;
        for (Map.Entry<AttributeType, Double> entry : scalingAttributes.entrySet()) {
            int attributeValue = attributeValues.getOrDefault(entry.getKey(), 0);
            double scaling = entry.getValue();
            totalMultiplier += (attributeValue * scaling) / 100.0;
        }
        return basePower * totalMultiplier;
    }

    // ==================== JSON Serialization ====================

    /**
     * Loads a Skill from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded Skill
     * @throws IOException if the file cannot be read
     */
    public static Skill fromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return fromJson(reader);
        }
    }

    /**
     * Loads a Skill from a JSON reader.
     *
     * @param reader the JSON reader
     * @return the loaded Skill
     */
    public static Skill fromJson(Reader reader) {
        JsonSkill json = GSON.fromJson(reader, JsonSkill.class);
        return json.toSkill();
    }

    /**
     * Loads a Skill from a JSON string.
     *
     * @param json the JSON string
     * @return the loaded Skill
     */
    public static Skill fromJsonString(String json) {
        JsonSkill jsonSkill = GSON.fromJson(json, JsonSkill.class);
        return jsonSkill.toSkill();
    }

    /**
     * Converts this skill to a JSON string.
     *
     * @return the JSON representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Loads multiple skills from a JSON file containing an array.
     *
     * @param path the path to the JSON file
     * @return list of loaded skills
     * @throws IOException if the file cannot be read
     */
    public static List<Skill> loadSkillsFromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<JsonSkill>>(){}.getType();
            List<JsonSkill> jsonSkills = GSON.fromJson(reader, listType);
            List<Skill> skills = new ArrayList<>();
            for (JsonSkill json : jsonSkills) {
                skills.add(json.toSkill());
            }
            return skills;
        }
    }

    // ==================== JSON Helper Class ====================

    /**
     * Internal class for JSON deserialization with string-based enums.
     */
    private static class JsonSkill {
        String id;
        @SerializedName("display_name")
        String displayName;
        String description;
        @SerializedName("icon_path")
        String iconPath;
        String type;
        @SerializedName("target_type")
        String targetType;
        @SerializedName("required_level")
        int requiredLevel = 1;
        @SerializedName("prerequisite_skills")
        List<String> prerequisiteSkills;
        @SerializedName("skill_point_cost")
        int skillPointCost = 1;
        @SerializedName("required_permissions")
        List<String> requiredPermissions;
        @SerializedName("cooldown_millis")
        long cooldownMillis;
        @SerializedName("mana_cost")
        double manaCost;
        @SerializedName("stamina_cost")
        double staminaCost;
        @SerializedName("cast_time_millis")
        int castTimeMillis;
        double range;
        @SerializedName("effect_data")
        Map<String, Object> effectData;
        @SerializedName("scaling_attributes")
        Map<String, Double> scalingAttributes;

        Skill toSkill() {
            Map<AttributeType, Double> scalingMap = new EnumMap<>(AttributeType.class);
            if (scalingAttributes != null) {
                for (Map.Entry<String, Double> entry : scalingAttributes.entrySet()) {
                    try {
                        AttributeType attr = AttributeType.valueOf(entry.getKey().toUpperCase());
                        scalingMap.put(attr, entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // Skip unknown attributes
                    }
                }
            }

            return new Skill(
                    id,
                    displayName,
                    description,
                    iconPath,
                    SkillType.fromName(type),
                    TargetType.fromName(targetType),
                    requiredLevel,
                    prerequisiteSkills,
                    skillPointCost,
                    requiredPermissions,
                    cooldownMillis,
                    manaCost,
                    staminaCost,
                    castTimeMillis,
                    range,
                    effectData,
                    scalingMap
            );
        }
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for constructing Skills.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing Skill instances.
     */
    public static class Builder {
        private String id;
        private String displayName;
        private String description;
        private String iconPath;
        private SkillType type = SkillType.ACTIVE;
        private TargetType targetType = TargetType.NONE;
        private int requiredLevel = 1;
        private final List<String> prerequisiteSkills = new ArrayList<>();
        private int skillPointCost = 1;
        private final List<String> requiredPermissions = new ArrayList<>();
        private long cooldownMillis = 0;
        private double manaCost = 0;
        private double staminaCost = 0;
        private int castTimeMillis = 0;
        private double range = 0;
        private final Map<String, Object> effectData = new LinkedHashMap<>();
        private final Map<AttributeType, Double> scalingAttributes = new EnumMap<>(AttributeType.class);

        private Builder() {}

        /**
         * Sets the unique identifier for this skill.
         *
         * @param id the skill ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the display name for this skill.
         *
         * @param displayName the display name
         * @return this builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the description for this skill.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the icon path for UI display.
         *
         * @param iconPath the icon path
         * @return this builder
         */
        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }

        /**
         * Sets the skill type.
         *
         * @param type the skill type
         * @return this builder
         */
        public Builder type(SkillType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the target type.
         *
         * @param targetType the target type
         * @return this builder
         */
        public Builder targetType(TargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * Sets the required level to learn this skill.
         *
         * @param requiredLevel the required level
         * @return this builder
         */
        public Builder requiredLevel(int requiredLevel) {
            this.requiredLevel = requiredLevel;
            return this;
        }

        /**
         * Adds a prerequisite skill.
         *
         * @param skillId the prerequisite skill ID
         * @return this builder
         */
        public Builder prerequisiteSkill(String skillId) {
            this.prerequisiteSkills.add(skillId);
            return this;
        }

        /**
         * Sets all prerequisite skills.
         *
         * @param skillIds the list of prerequisite skill IDs
         * @return this builder
         */
        public Builder prerequisiteSkills(List<String> skillIds) {
            this.prerequisiteSkills.clear();
            if (skillIds != null) {
                this.prerequisiteSkills.addAll(skillIds);
            }
            return this;
        }

        /**
         * Sets the skill point cost.
         *
         * @param cost the skill point cost
         * @return this builder
         */
        public Builder skillPointCost(int cost) {
            this.skillPointCost = cost;
            return this;
        }

        /**
         * Adds a required permission.
         *
         * @param permission the permission string
         * @return this builder
         */
        public Builder requiredPermission(String permission) {
            this.requiredPermissions.add(permission);
            return this;
        }

        /**
         * Sets all required permissions.
         *
         * @param permissions the list of permissions
         * @return this builder
         */
        public Builder requiredPermissions(List<String> permissions) {
            this.requiredPermissions.clear();
            if (permissions != null) {
                this.requiredPermissions.addAll(permissions);
            }
            return this;
        }

        /**
         * Sets the cooldown in milliseconds.
         *
         * @param millis the cooldown in milliseconds
         * @return this builder
         */
        public Builder cooldownMillis(long millis) {
            this.cooldownMillis = millis;
            return this;
        }

        /**
         * Sets the cooldown in seconds.
         *
         * @param seconds the cooldown in seconds
         * @return this builder
         */
        public Builder cooldownSeconds(double seconds) {
            this.cooldownMillis = (long) (seconds * 1000);
            return this;
        }

        /**
         * Sets the mana cost.
         *
         * @param cost the mana cost
         * @return this builder
         */
        public Builder manaCost(double cost) {
            this.manaCost = cost;
            return this;
        }

        /**
         * Sets the stamina cost.
         *
         * @param cost the stamina cost
         * @return this builder
         */
        public Builder staminaCost(double cost) {
            this.staminaCost = cost;
            return this;
        }

        /**
         * Sets the cast time in milliseconds.
         *
         * @param millis the cast time in milliseconds
         * @return this builder
         */
        public Builder castTimeMillis(int millis) {
            this.castTimeMillis = millis;
            return this;
        }

        /**
         * Sets the cast time in seconds.
         *
         * @param seconds the cast time in seconds
         * @return this builder
         */
        public Builder castTimeSeconds(double seconds) {
            this.castTimeMillis = (int) (seconds * 1000);
            return this;
        }

        /**
         * Sets the maximum range.
         *
         * @param range the range in blocks/units
         * @return this builder
         */
        public Builder range(double range) {
            this.range = range;
            return this;
        }

        /**
         * Adds effect data.
         *
         * @param key the data key
         * @param value the data value
         * @return this builder
         */
        public Builder effectData(String key, Object value) {
            this.effectData.put(key, value);
            return this;
        }

        /**
         * Sets all effect data.
         *
         * @param data the effect data map
         * @return this builder
         */
        public Builder effectData(Map<String, Object> data) {
            this.effectData.clear();
            if (data != null) {
                this.effectData.putAll(data);
            }
            return this;
        }

        /**
         * Adds a scaling attribute.
         *
         * @param attribute the attribute type
         * @param scaling the scaling multiplier
         * @return this builder
         */
        public Builder scalingAttribute(AttributeType attribute, double scaling) {
            this.scalingAttributes.put(attribute, scaling);
            return this;
        }

        /**
         * Sets all scaling attributes.
         *
         * @param scalings the scaling map
         * @return this builder
         */
        public Builder scalingAttributes(Map<AttributeType, Double> scalings) {
            this.scalingAttributes.clear();
            if (scalings != null) {
                this.scalingAttributes.putAll(scalings);
            }
            return this;
        }

        /**
         * Builds the Skill instance.
         *
         * @return the constructed Skill
         * @throws IllegalStateException if required fields are missing
         */
        public Skill build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Skill id is required");
            }
            return new Skill(
                    id,
                    displayName,
                    description,
                    iconPath,
                    type,
                    targetType,
                    requiredLevel,
                    prerequisiteSkills,
                    skillPointCost,
                    requiredPermissions,
                    cooldownMillis,
                    manaCost,
                    staminaCost,
                    castTimeMillis,
                    range,
                    effectData,
                    scalingAttributes
            );
        }
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                ", targetType=" + targetType +
                ", requiredLevel=" + requiredLevel +
                ", cooldownMillis=" + cooldownMillis +
                '}';
    }
}
