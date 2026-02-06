package com.NextLVLHasH.Websockets.rpg.character;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a character class in the RPG system.
 * <p>
 * A character class defines the archetype of a player's character, including
 * base attributes, attribute growth per level, starting skills, resource pools,
 * equipment restrictions, and combat modifiers.
 * </p>
 *
 * <p>Example classes: Warrior, Mage, Rogue, Priest, Ranger</p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class CharacterClass {

    // ==================== Identity ====================

    /**
     * Unique identifier for this class (e.g., "warrior", "mage").
     */
    @SerializedName("id")
    private final String id;

    /**
     * Human-readable display name (e.g., "Warrior", "Fire Mage").
     */
    @SerializedName("display_name")
    private final String displayName;

    /**
     * Description of the class and its playstyle.
     */
    @SerializedName("description")
    private final String description;

    /**
     * Path to the class icon for UI display.
     */
    @SerializedName("icon_path")
    private final String iconPath;

    // ==================== Attributes ====================

    /**
     * Base attribute values at level 1.
     */
    @SerializedName("base_attributes")
    private final Map<AttributeType, Integer> baseAttributes;

    /**
     * Attribute growth multipliers per level.
     * A value of 1.5 means the attribute gains 1-2 points per level.
     */
    @SerializedName("attribute_growth")
    private final Map<AttributeType, Double> attributeGrowth;

    // ==================== Skills ====================

    /**
     * List of skill IDs that are unlocked at character creation.
     */
    @SerializedName("starting_skills")
    private final List<String> startingSkills;

    // ==================== Resources ====================

    /**
     * Base health pool at level 1.
     */
    @SerializedName("base_health")
    private final double baseHealth;

    /**
     * Base mana pool at level 1.
     */
    @SerializedName("base_mana")
    private final double baseMana;

    /**
     * Base stamina pool at level 1.
     */
    @SerializedName("base_stamina")
    private final double baseStamina;

    /**
     * Health gained per level.
     */
    @SerializedName("health_per_level")
    private final double healthPerLevel;

    /**
     * Mana gained per level.
     */
    @SerializedName("mana_per_level")
    private final double manaPerLevel;

    /**
     * Stamina gained per level.
     */
    @SerializedName("stamina_per_level")
    private final double staminaPerLevel;

    /**
     * Health regeneration rate per second.
     */
    @SerializedName("health_regen")
    private final double healthRegen;

    /**
     * Mana regeneration rate per second.
     */
    @SerializedName("mana_regen")
    private final double manaRegen;

    /**
     * Stamina regeneration rate per second.
     */
    @SerializedName("stamina_regen")
    private final double staminaRegen;

    // ==================== Equipment ====================

    /**
     * Equipment restrictions and requirements for this class.
     */
    @SerializedName("equipment_rules")
    private final EquipmentRestriction equipmentRules;

    // ==================== Modifiers ====================

    /**
     * Combat and gameplay modifiers.
     * Common keys: "physicalDamage", "magicalDamage", "criticalChance", "criticalDamage"
     */
    @SerializedName("modifiers")
    private final Map<String, Double> modifiers;

    // ==================== Constants ====================

    private static final double DEFAULT_BASE_HEALTH = 100.0;
    private static final double DEFAULT_BASE_MANA = 50.0;
    private static final double DEFAULT_BASE_STAMINA = 100.0;
    private static final double DEFAULT_HEALTH_PER_LEVEL = 10.0;
    private static final double DEFAULT_MANA_PER_LEVEL = 5.0;
    private static final double DEFAULT_STAMINA_PER_LEVEL = 5.0;
    private static final double DEFAULT_HEALTH_REGEN = 1.0;
    private static final double DEFAULT_MANA_REGEN = 2.0;
    private static final double DEFAULT_STAMINA_REGEN = 5.0;
    private static final int DEFAULT_BASE_ATTRIBUTE = 10;
    private static final double DEFAULT_ATTRIBUTE_GROWTH = 1.0;

    // Gson instance for JSON operations
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ==================== Constructor ====================

    /**
     * Creates a new CharacterClass with all specified parameters.
     * Use {@link Builder} for easier construction.
     */
    private CharacterClass(
            String id,
            String displayName,
            String description,
            String iconPath,
            Map<AttributeType, Integer> baseAttributes,
            Map<AttributeType, Double> attributeGrowth,
            List<String> startingSkills,
            double baseHealth,
            double baseMana,
            double baseStamina,
            double healthPerLevel,
            double manaPerLevel,
            double staminaPerLevel,
            double healthRegen,
            double manaRegen,
            double staminaRegen,
            EquipmentRestriction equipmentRules,
            Map<String, Double> modifiers) {

        this.id = Objects.requireNonNull(id, "Class ID cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";

        this.baseAttributes = baseAttributes != null 
                ? new EnumMap<>(baseAttributes) 
                : new EnumMap<>(AttributeType.class);
        this.attributeGrowth = attributeGrowth != null 
                ? new EnumMap<>(attributeGrowth) 
                : new EnumMap<>(AttributeType.class);
        this.startingSkills = startingSkills != null 
                ? new ArrayList<>(startingSkills) 
                : new ArrayList<>();

        this.baseHealth = baseHealth > 0 ? baseHealth : DEFAULT_BASE_HEALTH;
        this.baseMana = baseMana >= 0 ? baseMana : DEFAULT_BASE_MANA;
        this.baseStamina = baseStamina >= 0 ? baseStamina : DEFAULT_BASE_STAMINA;

        this.healthPerLevel = healthPerLevel >= 0 ? healthPerLevel : DEFAULT_HEALTH_PER_LEVEL;
        this.manaPerLevel = manaPerLevel >= 0 ? manaPerLevel : DEFAULT_MANA_PER_LEVEL;
        this.staminaPerLevel = staminaPerLevel >= 0 ? staminaPerLevel : DEFAULT_STAMINA_PER_LEVEL;

        this.healthRegen = healthRegen >= 0 ? healthRegen : DEFAULT_HEALTH_REGEN;
        this.manaRegen = manaRegen >= 0 ? manaRegen : DEFAULT_MANA_REGEN;
        this.staminaRegen = staminaRegen >= 0 ? staminaRegen : DEFAULT_STAMINA_REGEN;

        this.equipmentRules = equipmentRules != null ? equipmentRules : new EquipmentRestriction();
        this.modifiers = modifiers != null ? new HashMap<>(modifiers) : new HashMap<>();
    }

    // ==================== Identity Getters ====================

    /**
     * Gets the unique identifier for this class.
     *
     * @return the class ID
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
     * Gets the class description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the path to the class icon.
     *
     * @return the icon path
     */
    public String getIconPath() {
        return iconPath;
    }

    // ==================== Attribute Getters ====================

    /**
     * Gets the base attributes at level 1.
     *
     * @return unmodifiable map of base attributes
     */
    public Map<AttributeType, Integer> getBaseAttributes() {
        return Collections.unmodifiableMap(baseAttributes);
    }

    /**
     * Gets the base value for a specific attribute.
     *
     * @param type the attribute type
     * @return the base value, or default if not set
     */
    public int getBaseAttribute(AttributeType type) {
        return baseAttributes.getOrDefault(type, DEFAULT_BASE_ATTRIBUTE);
    }

    /**
     * Gets the attribute growth rates per level.
     *
     * @return unmodifiable map of attribute growth rates
     */
    public Map<AttributeType, Double> getAttributeGrowth() {
        return Collections.unmodifiableMap(attributeGrowth);
    }

    /**
     * Gets the growth rate for a specific attribute.
     *
     * @param type the attribute type
     * @return the growth rate per level
     */
    public double getAttributeGrowthRate(AttributeType type) {
        return attributeGrowth.getOrDefault(type, DEFAULT_ATTRIBUTE_GROWTH);
    }

    /**
     * Calculates the total attribute value at a given level.
     *
     * @param type  the attribute type
     * @param level the character level
     * @return the calculated attribute value
     */
    public int calculateAttributeAtLevel(AttributeType type, int level) {
        int base = getBaseAttribute(type);
        double growth = getAttributeGrowthRate(type);
        return base + (int) Math.floor(growth * (level - 1));
    }

    /**
     * Gets the primary attribute for this class (highest growth rate).
     * @return the primary AttributeType, or STRENGTH as default
     */
    public AttributeType getPrimaryAttribute() {
        return attributeGrowth.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(AttributeType.STRENGTH);
    }

    /**
     * Gets the secondary attribute for this class (second highest growth rate).
     * @return the secondary AttributeType, or CONSTITUTION as default
     */
    public AttributeType getSecondaryAttribute() {
        AttributeType primary = getPrimaryAttribute();
        return attributeGrowth.entrySet().stream()
            .filter(e -> e.getKey() != primary)
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(AttributeType.CONSTITUTION);
    }

    // ==================== Skill Getters ====================

    /**
     * Gets the list of starting skill IDs.
     *
     * @return unmodifiable list of starting skill IDs
     */
    public List<String> getStartingSkills() {
        return Collections.unmodifiableList(startingSkills);
    }

    /**
     * Checks if a skill is a starting skill for this class.
     *
     * @param skillId the skill ID to check
     * @return true if it's a starting skill
     */
    public boolean hasStartingSkill(String skillId) {
        return startingSkills.contains(skillId);
    }

    // ==================== Resource Getters ====================

    /**
     * Gets the base health at level 1.
     *
     * @return the base health
     */
    public double getBaseHealth() {
        return baseHealth;
    }

    /**
     * Gets the base mana at level 1.
     *
     * @return the base mana
     */
    public double getBaseMana() {
        return baseMana;
    }

    /**
     * Gets the base stamina at level 1.
     *
     * @return the base stamina
     */
    public double getBaseStamina() {
        return baseStamina;
    }

    /**
     * Gets the health gained per level.
     *
     * @return health per level
     */
    public double getHealthPerLevel() {
        return healthPerLevel;
    }

    /**
     * Gets the mana gained per level.
     *
     * @return mana per level
     */
    public double getManaPerLevel() {
        return manaPerLevel;
    }

    /**
     * Gets the stamina gained per level.
     *
     * @return stamina per level
     */
    public double getStaminaPerLevel() {
        return staminaPerLevel;
    }

    /**
     * Gets the health regeneration rate per second.
     *
     * @return health regen rate
     */
    public double getHealthRegen() {
        return healthRegen;
    }

    /**
     * Gets the mana regeneration rate per second.
     *
     * @return mana regen rate
     */
    public double getManaRegen() {
        return manaRegen;
    }

    /**
     * Gets the stamina regeneration rate per second.
     *
     * @return stamina regen rate
     */
    public double getStaminaRegen() {
        return staminaRegen;
    }

    /**
     * Calculates the max health at a given level.
     *
     * @param level the character level
     * @return the max health at that level
     */
    public double calculateHealthAtLevel(int level) {
        return baseHealth + (healthPerLevel * (level - 1));
    }

    /**
     * Calculates the max mana at a given level.
     *
     * @param level the character level
     * @return the max mana at that level
     */
    public double calculateManaAtLevel(int level) {
        return baseMana + (manaPerLevel * (level - 1));
    }

    /**
     * Calculates the max stamina at a given level.
     *
     * @param level the character level
     * @return the max stamina at that level
     */
    public double calculateStaminaAtLevel(int level) {
        return baseStamina + (staminaPerLevel * (level - 1));
    }

    // ==================== Equipment Getters ====================

    /**
     * Gets the equipment restrictions for this class.
     *
     * @return the equipment restrictions
     */
    public EquipmentRestriction getEquipmentRules() {
        return equipmentRules;
    }

    // ==================== Modifier Getters ====================

    /**
     * Gets all combat modifiers.
     *
     * @return unmodifiable map of modifiers
     */
    public Map<String, Double> getModifiers() {
        return Collections.unmodifiableMap(modifiers);
    }

    /**
     * Gets a specific modifier value.
     *
     * @param key the modifier key
     * @return the modifier value, or 1.0 if not set (neutral modifier)
     */
    public double getModifier(String key) {
        return modifiers.getOrDefault(key, 1.0);
    }

    /**
     * Gets the physical damage modifier.
     *
     * @return the physical damage modifier
     */
    public double getPhysicalDamageModifier() {
        return getModifier("physicalDamage");
    }

    /**
     * Gets the magical damage modifier.
     *
     * @return the magical damage modifier
     */
    public double getMagicalDamageModifier() {
        return getModifier("magicalDamage");
    }

    /**
     * Gets the critical chance modifier.
     *
     * @return the critical chance modifier (0.0 to 1.0)
     */
    public double getCriticalChanceModifier() {
        return getModifier("criticalChance");
    }

    /**
     * Gets the critical damage multiplier.
     *
     * @return the critical damage multiplier
     */
    public double getCriticalDamageModifier() {
        return getModifier("criticalDamage");
    }

    // ==================== JSON Serialization ====================

    /**
     * Serializes this CharacterClass to a JSON string.
     *
     * @return JSON string representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes a CharacterClass from a JSON string.
     *
     * @param json the JSON string
     * @return the deserialized CharacterClass
     */
    public static CharacterClass fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        return GSON.fromJson(json, CharacterClass.class);
    }

    /**
     * Loads a CharacterClass from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded CharacterClass
     * @throws IOException if the file cannot be read
     */
    public static CharacterClass fromFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, CharacterClass.class);
        }
    }

    // ==================== Builder Pattern ====================

    /**
     * Creates a new Builder for constructing CharacterClass instances.
     *
     * @param id the unique class ID
     * @return a new Builder instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder class for constructing CharacterClass instances.
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private String description;
        private String iconPath;
        private final Map<AttributeType, Integer> baseAttributes = new EnumMap<>(AttributeType.class);
        private final Map<AttributeType, Double> attributeGrowth = new EnumMap<>(AttributeType.class);
        private final List<String> startingSkills = new ArrayList<>();
        private double baseHealth = DEFAULT_BASE_HEALTH;
        private double baseMana = DEFAULT_BASE_MANA;
        private double baseStamina = DEFAULT_BASE_STAMINA;
        private double healthPerLevel = DEFAULT_HEALTH_PER_LEVEL;
        private double manaPerLevel = DEFAULT_MANA_PER_LEVEL;
        private double staminaPerLevel = DEFAULT_STAMINA_PER_LEVEL;
        private double healthRegen = DEFAULT_HEALTH_REGEN;
        private double manaRegen = DEFAULT_MANA_REGEN;
        private double staminaRegen = DEFAULT_STAMINA_REGEN;
        private EquipmentRestriction equipmentRules;
        private final Map<String, Double> modifiers = new HashMap<>();

        /**
         * Creates a new Builder with the specified class ID.
         *
         * @param id the unique class ID
         */
        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Class ID cannot be null");
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
         * Sets a base attribute value.
         *
         * @param type  the attribute type
         * @param value the base value
         * @return this builder
         */
        public Builder baseAttribute(AttributeType type, int value) {
            if (type != null) {
                baseAttributes.put(type, value);
            }
            return this;
        }

        /**
         * Sets all base attributes from a map.
         *
         * @param attributes the attributes map
         * @return this builder
         */
        public Builder baseAttributes(Map<AttributeType, Integer> attributes) {
            if (attributes != null) {
                baseAttributes.putAll(attributes);
            }
            return this;
        }

        /**
         * Sets an attribute growth rate.
         *
         * @param type       the attribute type
         * @param growthRate the growth rate per level
         * @return this builder
         */
        public Builder attributeGrowth(AttributeType type, double growthRate) {
            if (type != null) {
                attributeGrowth.put(type, growthRate);
            }
            return this;
        }

        /**
         * Sets all attribute growth rates from a map.
         *
         * @param growth the growth rates map
         * @return this builder
         */
        public Builder attributeGrowth(Map<AttributeType, Double> growth) {
            if (growth != null) {
                attributeGrowth.putAll(growth);
            }
            return this;
        }

        /**
         * Adds a starting skill.
         *
         * @param skillId the skill ID
         * @return this builder
         */
        public Builder startingSkill(String skillId) {
            if (skillId != null && !skillId.isBlank()) {
                startingSkills.add(skillId);
            }
            return this;
        }

        /**
         * Adds multiple starting skills.
         *
         * @param skills the skill IDs
         * @return this builder
         */
        public Builder startingSkills(String... skills) {
            for (String skill : skills) {
                startingSkill(skill);
            }
            return this;
        }

        /**
         * Adds multiple starting skills from a collection.
         *
         * @param skills the skill IDs
         * @return this builder
         */
        public Builder startingSkills(Collection<String> skills) {
            if (skills != null) {
                skills.forEach(this::startingSkill);
            }
            return this;
        }

        /**
         * Sets the base health.
         *
         * @param baseHealth the base health
         * @return this builder
         */
        public Builder baseHealth(double baseHealth) {
            this.baseHealth = baseHealth;
            return this;
        }

        /**
         * Sets the base mana.
         *
         * @param baseMana the base mana
         * @return this builder
         */
        public Builder baseMana(double baseMana) {
            this.baseMana = baseMana;
            return this;
        }

        /**
         * Sets the base stamina.
         *
         * @param baseStamina the base stamina
         * @return this builder
         */
        public Builder baseStamina(double baseStamina) {
            this.baseStamina = baseStamina;
            return this;
        }

        /**
         * Sets the health per level.
         *
         * @param healthPerLevel health gained per level
         * @return this builder
         */
        public Builder healthPerLevel(double healthPerLevel) {
            this.healthPerLevel = healthPerLevel;
            return this;
        }

        /**
         * Sets the mana per level.
         *
         * @param manaPerLevel mana gained per level
         * @return this builder
         */
        public Builder manaPerLevel(double manaPerLevel) {
            this.manaPerLevel = manaPerLevel;
            return this;
        }

        /**
         * Sets the stamina per level.
         *
         * @param staminaPerLevel stamina gained per level
         * @return this builder
         */
        public Builder staminaPerLevel(double staminaPerLevel) {
            this.staminaPerLevel = staminaPerLevel;
            return this;
        }

        /**
         * Sets the health regeneration rate.
         *
         * @param healthRegen health regen per second
         * @return this builder
         */
        public Builder healthRegen(double healthRegen) {
            this.healthRegen = healthRegen;
            return this;
        }

        /**
         * Sets the mana regeneration rate.
         *
         * @param manaRegen mana regen per second
         * @return this builder
         */
        public Builder manaRegen(double manaRegen) {
            this.manaRegen = manaRegen;
            return this;
        }

        /**
         * Sets the stamina regeneration rate.
         *
         * @param staminaRegen stamina regen per second
         * @return this builder
         */
        public Builder staminaRegen(double staminaRegen) {
            this.staminaRegen = staminaRegen;
            return this;
        }

        /**
         * Sets the equipment restrictions.
         *
         * @param equipmentRules the equipment restrictions
         * @return this builder
         */
        public Builder equipmentRules(EquipmentRestriction equipmentRules) {
            this.equipmentRules = equipmentRules;
            return this;
        }

        /**
         * Sets a modifier value.
         *
         * @param key   the modifier key
         * @param value the modifier value
         * @return this builder
         */
        public Builder modifier(String key, double value) {
            if (key != null && !key.isBlank()) {
                modifiers.put(key, value);
            }
            return this;
        }

        /**
         * Sets the physical damage modifier.
         *
         * @param value the modifier value
         * @return this builder
         */
        public Builder physicalDamage(double value) {
            return modifier("physicalDamage", value);
        }

        /**
         * Sets the magical damage modifier.
         *
         * @param value the modifier value
         * @return this builder
         */
        public Builder magicalDamage(double value) {
            return modifier("magicalDamage", value);
        }

        /**
         * Sets the critical chance modifier.
         *
         * @param value the modifier value (0.0 to 1.0)
         * @return this builder
         */
        public Builder criticalChance(double value) {
            return modifier("criticalChance", value);
        }

        /**
         * Sets the critical damage modifier.
         *
         * @param value the modifier value
         * @return this builder
         */
        public Builder criticalDamage(double value) {
            return modifier("criticalDamage", value);
        }

        /**
         * Sets all modifiers from a map.
         *
         * @param modifiers the modifiers map
         * @return this builder
         */
        public Builder modifiers(Map<String, Double> modifiers) {
            if (modifiers != null) {
                this.modifiers.putAll(modifiers);
            }
            return this;
        }

        /**
         * Builds the CharacterClass instance.
         *
         * @return the constructed CharacterClass
         */
        public CharacterClass build() {
            return new CharacterClass(
                    id,
                    displayName,
                    description,
                    iconPath,
                    baseAttributes,
                    attributeGrowth,
                    startingSkills,
                    baseHealth,
                    baseMana,
                    baseStamina,
                    healthPerLevel,
                    manaPerLevel,
                    staminaPerLevel,
                    healthRegen,
                    manaRegen,
                    staminaRegen,
                    equipmentRules,
                    modifiers
            );
        }
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        return "CharacterClass{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", baseHealth=" + baseHealth +
                ", baseMana=" + baseMana +
                ", baseStamina=" + baseStamina +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterClass that = (CharacterClass) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
