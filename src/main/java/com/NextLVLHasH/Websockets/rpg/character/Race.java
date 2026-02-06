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
 * Represents a playable race in the RPG system.
 * <p>
 * A race defines the physical and innate characteristics of a character, including
 * racial attribute bonuses, unique abilities, physical properties (height, speed),
 * damage resistances, and passive effects.
 * </p>
 *
 * <p>Example races: Human, Elf, Dwarf, Orc, Undead</p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class Race {

    // ==================== Identity ====================

    /**
     * Unique identifier for this race (e.g., "human", "elf").
     */
    @SerializedName("id")
    private final String id;

    /**
     * Human-readable display name (e.g., "Human", "High Elf").
     */
    @SerializedName("display_name")
    private final String displayName;

    /**
     * Description of the race and its lore.
     */
    @SerializedName("description")
    private final String description;

    /**
     * Path to the race icon for UI display.
     */
    @SerializedName("icon_path")
    private final String iconPath;

    /**
     * Path to the 3D model for this race.
     */
    @SerializedName("model_path")
    private final String modelPath;

    // ==================== Attributes ====================

    /**
     * Racial attribute bonuses (added to base attributes).
     */
    @SerializedName("racial_bonuses")
    private final Map<AttributeType, Integer> racialBonuses;

    // ==================== Abilities ====================

    /**
     * List of racial ability IDs unique to this race.
     */
    @SerializedName("racial_abilities")
    private final List<String> racialAbilities;

    // ==================== Physical Properties ====================

    /**
     * Height scale modifier (1.0 = normal, 0.8 = shorter, 1.2 = taller).
     */
    @SerializedName("height_scale")
    private final double heightScale;

    /**
     * Movement speed modifier (1.0 = normal, 1.1 = 10% faster).
     */
    @SerializedName("speed_modifier")
    private final double speedModifier;

    // ==================== Resistances ====================

    /**
     * Damage type resistances (e.g., "fire" -> 0.1 means 10% fire resistance).
     * Positive values = resistance, negative values = vulnerability.
     */
    @SerializedName("resistances")
    private final Map<String, Double> resistances;

    // ==================== Passive Effects ====================

    /**
     * List of passive effects that apply to characters of this race.
     */
    @SerializedName("passive_effects")
    private final List<PassiveEffect> passiveEffects;

    // ==================== Constants ====================

    private static final double DEFAULT_HEIGHT_SCALE = 1.0;
    private static final double DEFAULT_SPEED_MODIFIER = 1.0;

    // Gson instance for JSON operations
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ==================== Constructor ====================

    /**
     * Creates a new Race with all specified parameters.
     * Use {@link Builder} for easier construction.
     */
    private Race(
            String id,
            String displayName,
            String description,
            String iconPath,
            String modelPath,
            Map<AttributeType, Integer> racialBonuses,
            List<String> racialAbilities,
            double heightScale,
            double speedModifier,
            Map<String, Double> resistances,
            List<PassiveEffect> passiveEffects) {

        this.id = Objects.requireNonNull(id, "Race ID cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";
        this.modelPath = modelPath != null ? modelPath : "";

        this.racialBonuses = racialBonuses != null 
                ? new EnumMap<>(racialBonuses) 
                : new EnumMap<>(AttributeType.class);
        this.racialAbilities = racialAbilities != null 
                ? new ArrayList<>(racialAbilities) 
                : new ArrayList<>();

        this.heightScale = heightScale > 0 ? heightScale : DEFAULT_HEIGHT_SCALE;
        this.speedModifier = speedModifier > 0 ? speedModifier : DEFAULT_SPEED_MODIFIER;

        this.resistances = resistances != null 
                ? new HashMap<>(resistances) 
                : new HashMap<>();
        this.passiveEffects = passiveEffects != null 
                ? new ArrayList<>(passiveEffects) 
                : new ArrayList<>();
    }

    // ==================== Identity Getters ====================

    /**
     * Gets the unique identifier for this race.
     *
     * @return the race ID
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
     * Gets the race description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the path to the race icon.
     *
     * @return the icon path
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Gets the path to the 3D model.
     *
     * @return the model path
     */
    public String getModelPath() {
        return modelPath;
    }

    // ==================== Attribute Getters ====================

    /**
     * Gets the racial attribute bonuses.
     *
     * @return unmodifiable map of racial bonuses
     */
    public Map<AttributeType, Integer> getRacialBonuses() {
        return Collections.unmodifiableMap(racialBonuses);
    }

    /**
     * Gets the racial bonus for a specific attribute.
     *
     * @param type the attribute type
     * @return the bonus value, or 0 if no bonus exists
     */
    public int getRacialBonus(AttributeType type) {
        return racialBonuses.getOrDefault(type, 0);
    }

    /**
     * Checks if this race has a bonus for the specified attribute.
     *
     * @param type the attribute type
     * @return true if a bonus exists
     */
    public boolean hasRacialBonus(AttributeType type) {
        return racialBonuses.containsKey(type) && racialBonuses.get(type) != 0;
    }

    // ==================== Ability Getters ====================

    /**
     * Gets the list of racial ability IDs.
     *
     * @return unmodifiable list of racial ability IDs
     */
    public List<String> getRacialAbilities() {
        return Collections.unmodifiableList(racialAbilities);
    }

    /**
     * Checks if this race has the specified ability.
     *
     * @param abilityId the ability ID to check
     * @return true if the race has this ability
     */
    public boolean hasRacialAbility(String abilityId) {
        return racialAbilities.contains(abilityId);
    }

    // ==================== Physical Property Getters ====================

    /**
     * Gets the height scale modifier.
     *
     * @return the height scale (1.0 = normal)
     */
    public double getHeightScale() {
        return heightScale;
    }

    /**
     * Gets the movement speed modifier.
     *
     * @return the speed modifier (1.0 = normal)
     */
    public double getSpeedModifier() {
        return speedModifier;
    }

    // ==================== Resistance Getters ====================

    /**
     * Gets all damage resistances.
     *
     * @return unmodifiable map of resistances
     */
    public Map<String, Double> getResistances() {
        return Collections.unmodifiableMap(resistances);
    }

    /**
     * Gets the resistance for a specific damage type.
     *
     * @param damageType the damage type (e.g., "fire", "ice", "poison")
     * @return the resistance value (positive = resistant, negative = vulnerable)
     */
    public double getResistance(String damageType) {
        return resistances.getOrDefault(damageType.toLowerCase(), 0.0);
    }

    /**
     * Checks if this race has resistance to the specified damage type.
     *
     * @param damageType the damage type
     * @return true if resistant (positive resistance value)
     */
    public boolean isResistantTo(String damageType) {
        return getResistance(damageType) > 0;
    }

    /**
     * Checks if this race is vulnerable to the specified damage type.
     *
     * @param damageType the damage type
     * @return true if vulnerable (negative resistance value)
     */
    public boolean isVulnerableTo(String damageType) {
        return getResistance(damageType) < 0;
    }

    /**
     * Calculates the effective damage after applying resistance.
     *
     * @param damageType   the type of damage
     * @param baseDamage   the base damage amount
     * @return the damage after resistance is applied
     */
    public double calculateDamageAfterResistance(String damageType, double baseDamage) {
        double resistance = getResistance(damageType);
        return baseDamage * (1.0 - resistance);
    }

    // ==================== Passive Effect Getters ====================

    /**
     * Gets all passive effects for this race.
     *
     * @return unmodifiable list of passive effects
     */
    public List<PassiveEffect> getPassiveEffects() {
        return Collections.unmodifiableList(passiveEffects);
    }

    /**
     * Gets all permanent passive effects.
     *
     * @return list of permanent passive effects
     */
    public List<PassiveEffect> getPermanentPassiveEffects() {
        return passiveEffects.stream()
                .filter(PassiveEffect::isPermanent)
                .toList();
    }

    /**
     * Checks if this race has the specified passive effect.
     *
     * @param effectId the effect ID to check
     * @return true if the race has this passive effect
     */
    public boolean hasPassiveEffect(String effectId) {
        return passiveEffects.stream()
                .anyMatch(effect -> effect.getEffectId().equals(effectId));
    }

    // ==================== JSON Serialization ====================

    /**
     * Serializes this Race to a JSON string.
     *
     * @return JSON string representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes a Race from a JSON string.
     *
     * @param json the JSON string
     * @return the deserialized Race
     */
    public static Race fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        return GSON.fromJson(json, Race.class);
    }

    /**
     * Loads a Race from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded Race
     * @throws IOException if the file cannot be read
     */
    public static Race fromFile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, Race.class);
        }
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a passive effect that is applied to characters of this race.
     */
    public static class PassiveEffect {

        /**
         * The unique identifier of the effect.
         */
        @SerializedName("effect_id")
        private final String effectId;

        /**
         * Whether this effect is permanent (always active) or conditional.
         */
        @SerializedName("permanent")
        private final boolean permanent;

        /**
         * Optional description of the passive effect.
         */
        @SerializedName("description")
        private final String description;

        /**
         * Optional condition for when this effect is active (if not permanent).
         */
        @SerializedName("condition")
        private final String condition;

        /**
         * Creates a new PassiveEffect.
         *
         * @param effectId    the effect ID
         * @param permanent   whether the effect is permanent
         * @param description optional description
         * @param condition   optional activation condition
         */
        public PassiveEffect(String effectId, boolean permanent, String description, String condition) {
            this.effectId = Objects.requireNonNull(effectId, "Effect ID cannot be null");
            this.permanent = permanent;
            this.description = description != null ? description : "";
            this.condition = condition != null ? condition : "";
        }

        /**
         * Creates a permanent passive effect.
         *
         * @param effectId the effect ID
         */
        public PassiveEffect(String effectId) {
            this(effectId, true, null, null);
        }

        /**
         * Gets the effect ID.
         *
         * @return the effect ID
         */
        public String getEffectId() {
            return effectId;
        }

        /**
         * Checks if this effect is permanent.
         *
         * @return true if permanent
         */
        public boolean isPermanent() {
            return permanent;
        }

        /**
         * Gets the effect description.
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the activation condition.
         *
         * @return the condition, or empty string if none
         */
        public String getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            return "PassiveEffect{" +
                    "effectId='" + effectId + '\'' +
                    ", permanent=" + permanent +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PassiveEffect that = (PassiveEffect) o;
            return Objects.equals(effectId, that.effectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(effectId);
        }
    }

    // ==================== Builder Pattern ====================

    /**
     * Creates a new Builder for constructing Race instances.
     *
     * @param id the unique race ID
     * @return a new Builder instance
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder class for constructing Race instances.
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private String description;
        private String iconPath;
        private String modelPath;
        private final Map<AttributeType, Integer> racialBonuses = new EnumMap<>(AttributeType.class);
        private final List<String> racialAbilities = new ArrayList<>();
        private double heightScale = DEFAULT_HEIGHT_SCALE;
        private double speedModifier = DEFAULT_SPEED_MODIFIER;
        private final Map<String, Double> resistances = new HashMap<>();
        private final List<PassiveEffect> passiveEffects = new ArrayList<>();

        /**
         * Creates a new Builder with the specified race ID.
         *
         * @param id the unique race ID
         */
        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Race ID cannot be null");
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
         * Sets the model path.
         *
         * @param modelPath the model path
         * @return this builder
         */
        public Builder modelPath(String modelPath) {
            this.modelPath = modelPath;
            return this;
        }

        /**
         * Sets a racial attribute bonus.
         *
         * @param type  the attribute type
         * @param bonus the bonus value
         * @return this builder
         */
        public Builder racialBonus(AttributeType type, int bonus) {
            if (type != null) {
                racialBonuses.put(type, bonus);
            }
            return this;
        }

        /**
         * Sets all racial bonuses from a map.
         *
         * @param bonuses the bonuses map
         * @return this builder
         */
        public Builder racialBonuses(Map<AttributeType, Integer> bonuses) {
            if (bonuses != null) {
                racialBonuses.putAll(bonuses);
            }
            return this;
        }

        /**
         * Adds a racial ability.
         *
         * @param abilityId the ability ID
         * @return this builder
         */
        public Builder racialAbility(String abilityId) {
            if (abilityId != null && !abilityId.isBlank()) {
                racialAbilities.add(abilityId);
            }
            return this;
        }

        /**
         * Adds multiple racial abilities.
         *
         * @param abilities the ability IDs
         * @return this builder
         */
        public Builder racialAbilities(String... abilities) {
            for (String ability : abilities) {
                racialAbility(ability);
            }
            return this;
        }

        /**
         * Adds multiple racial abilities from a collection.
         *
         * @param abilities the ability IDs
         * @return this builder
         */
        public Builder racialAbilities(Collection<String> abilities) {
            if (abilities != null) {
                abilities.forEach(this::racialAbility);
            }
            return this;
        }

        /**
         * Sets the height scale.
         *
         * @param heightScale the height scale
         * @return this builder
         */
        public Builder heightScale(double heightScale) {
            this.heightScale = heightScale;
            return this;
        }

        /**
         * Sets the speed modifier.
         *
         * @param speedModifier the speed modifier
         * @return this builder
         */
        public Builder speedModifier(double speedModifier) {
            this.speedModifier = speedModifier;
            return this;
        }

        /**
         * Adds a damage resistance.
         *
         * @param damageType the damage type
         * @param resistance the resistance value
         * @return this builder
         */
        public Builder resistance(String damageType, double resistance) {
            if (damageType != null && !damageType.isBlank()) {
                resistances.put(damageType.toLowerCase(), resistance);
            }
            return this;
        }

        /**
         * Sets all resistances from a map.
         *
         * @param resistances the resistances map
         * @return this builder
         */
        public Builder resistances(Map<String, Double> resistances) {
            if (resistances != null) {
                resistances.forEach(this::resistance);
            }
            return this;
        }

        /**
         * Adds a passive effect.
         *
         * @param effect the passive effect
         * @return this builder
         */
        public Builder passiveEffect(PassiveEffect effect) {
            if (effect != null) {
                passiveEffects.add(effect);
            }
            return this;
        }

        /**
         * Adds a permanent passive effect by ID.
         *
         * @param effectId the effect ID
         * @return this builder
         */
        public Builder permanentPassiveEffect(String effectId) {
            if (effectId != null && !effectId.isBlank()) {
                passiveEffects.add(new PassiveEffect(effectId));
            }
            return this;
        }

        /**
         * Adds a conditional passive effect.
         *
         * @param effectId    the effect ID
         * @param description the effect description
         * @param condition   the activation condition
         * @return this builder
         */
        public Builder conditionalPassiveEffect(String effectId, String description, String condition) {
            if (effectId != null && !effectId.isBlank()) {
                passiveEffects.add(new PassiveEffect(effectId, false, description, condition));
            }
            return this;
        }

        /**
         * Adds multiple passive effects from a collection.
         *
         * @param effects the passive effects
         * @return this builder
         */
        public Builder passiveEffects(Collection<PassiveEffect> effects) {
            if (effects != null) {
                effects.forEach(this::passiveEffect);
            }
            return this;
        }

        /**
         * Builds the Race instance.
         *
         * @return the constructed Race
         */
        public Race build() {
            return new Race(
                    id,
                    displayName,
                    description,
                    iconPath,
                    modelPath,
                    racialBonuses,
                    racialAbilities,
                    heightScale,
                    speedModifier,
                    resistances,
                    passiveEffects
            );
        }
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        return "Race{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", heightScale=" + heightScale +
                ", speedModifier=" + speedModifier +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Race race = (Race) o;
        return Objects.equals(id, race.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
