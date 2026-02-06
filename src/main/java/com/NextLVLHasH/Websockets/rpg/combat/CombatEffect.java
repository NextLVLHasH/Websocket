package com.NextLVLHasH.Websockets.rpg.combat;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines a combat effect that can be applied to entities.
 * This class represents the template/definition of an effect, loaded from configuration.
 * Actual applied effects are represented by {@link EffectInstance}.
 * 
 * <p>Combat effects can:
 * <ul>
 *     <li>Modify attributes (buffs/debuffs)</li>
 *     <li>Deal damage over time (DOT)</li>
 *     <li>Heal over time (HOT)</li>
 *     <li>Apply crowd control (stun, root, silence)</li>
 *     <li>Stack multiple times for increased effect</li>
 * </ul>
 * 
 * <p>Effects are created using the Builder pattern:
 * <pre>
 * CombatEffect poison = CombatEffect.builder()
 *     .id("poison_weak")
 *     .displayName("Weak Poison")
 *     .type(EffectType.DOT)
 *     .category(EffectCategory.POISON)
 *     .duration(10000)
 *     .tickInterval(1000)
 *     .damagePerTick(5.0)
 *     .damageType(DamageType.POISON)
 *     .stackable(true)
 *     .maxStacks(5)
 *     .build();
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
@SuppressWarnings("unused")
public final class CombatEffect {

    private static final Logger LOGGER = Logger.getLogger(CombatEffect.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    @SerializedName("id")
    private final String id;

    @SerializedName("display_name")
    private final String displayName;

    @SerializedName("description")
    private final String description;

    @SerializedName("icon_path")
    private final String iconPath;

    @SerializedName("type")
    private final EffectType type;

    @SerializedName("category")
    private final EffectCategory category;

    @SerializedName("duration_millis")
    private final int durationMillis;

    @SerializedName("tick_interval_millis")
    private final int tickIntervalMillis;

    @SerializedName("attribute_modifiers")
    private final Map<AttributeType, Double> attributeModifiers;

    @SerializedName("damage_per_tick")
    private final double damagePerTick;

    @SerializedName("healing_per_tick")
    private final double healingPerTick;

    @SerializedName("damage_type")
    private final DamageType damageType;

    @SerializedName("stackable")
    private final boolean stackable;

    @SerializedName("max_stacks")
    private final int maxStacks;

    @SerializedName("particle_effect")
    private final String particleEffect;

    @SerializedName("remove_on_death")
    private final boolean removeOnDeath;

    @SerializedName("can_dispel")
    private final boolean canDispel;

    /**
     * Private constructor - use Builder to create instances.
     */
    private CombatEffect(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Effect ID cannot be null");
        this.displayName = builder.displayName != null ? builder.displayName : builder.id;
        this.description = builder.description != null ? builder.description : "";
        this.iconPath = builder.iconPath;
        this.type = builder.type != null ? builder.type : EffectType.BUFF;
        this.category = builder.category != null ? builder.category : EffectCategory.MAGIC;
        this.durationMillis = Math.max(0, builder.durationMillis);
        this.tickIntervalMillis = Math.max(100, builder.tickIntervalMillis); // Min 100ms tick
        this.attributeModifiers = builder.attributeModifiers != null
                ? Collections.unmodifiableMap(new EnumMap<>(builder.attributeModifiers))
                : Collections.emptyMap();
        this.damagePerTick = Math.max(0, builder.damagePerTick);
        this.healingPerTick = Math.max(0, builder.healingPerTick);
        this.damageType = builder.damageType;
        this.stackable = builder.stackable;
        this.maxStacks = Math.max(1, builder.maxStacks);
        this.particleEffect = builder.particleEffect;
        this.removeOnDeath = builder.removeOnDeath;
        this.canDispel = builder.canDispel;
    }

    // ========== Getters ==========

    /**
     * Gets the unique identifier for this effect.
     *
     * @return the effect ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name shown in the UI.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of the effect.
     *
     * @return the effect description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the path to the icon resource.
     *
     * @return the icon path, or null if not set
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Gets the type of this effect.
     *
     * @return the EffectType
     */
    public EffectType getType() {
        return type;
    }

    /**
     * Gets the category of this effect for dispel/immunity purposes.
     *
     * @return the EffectCategory
     */
    public EffectCategory getCategory() {
        return category;
    }

    /**
     * Gets the default duration in milliseconds.
     *
     * @return the duration in milliseconds
     */
    public int getDurationMillis() {
        return durationMillis;
    }

    /**
     * Gets the interval between ticks in milliseconds.
     *
     * @return the tick interval in milliseconds
     */
    public int getTickIntervalMillis() {
        return tickIntervalMillis;
    }

    /**
     * Gets the attribute modifiers applied by this effect.
     * Values are additive modifiers (positive = bonus, negative = penalty).
     *
     * @return unmodifiable map of attribute modifiers
     */
    public Map<AttributeType, Double> getAttributeModifiers() {
        return attributeModifiers;
    }

    /**
     * Gets the modifier for a specific attribute.
     *
     * @param attribute the attribute to check
     * @return the modifier value, or 0.0 if not present
     */
    public double getAttributeModifier(AttributeType attribute) {
        return attributeModifiers.getOrDefault(attribute, 0.0);
    }

    /**
     * Gets the damage dealt per tick for DOT effects.
     *
     * @return the damage per tick
     */
    public double getDamagePerTick() {
        return damagePerTick;
    }

    /**
     * Gets the healing applied per tick for HOT effects.
     *
     * @return the healing per tick
     */
    public double getHealingPerTick() {
        return healingPerTick;
    }

    /**
     * Gets the damage type for DOT effects.
     *
     * @return the DamageType, or null if not applicable
     */
    public DamageType getDamageType() {
        return damageType;
    }

    /**
     * Checks if this effect can stack.
     *
     * @return true if stackable
     */
    public boolean isStackable() {
        return stackable;
    }

    /**
     * Gets the maximum number of stacks.
     *
     * @return the max stack count
     */
    public int getMaxStacks() {
        return maxStacks;
    }

    /**
     * Gets the particle effect identifier.
     *
     * @return the particle effect ID, or null if none
     */
    public String getParticleEffect() {
        return particleEffect;
    }

    /**
     * Checks if this effect should be removed on death.
     *
     * @return true if removed on death
     */
    public boolean isRemoveOnDeath() {
        return removeOnDeath;
    }

    /**
     * Checks if this effect can be dispelled.
     *
     * @return true if dispellable
     */
    public boolean canDispel() {
        return canDispel;
    }

    // ========== Utility Methods ==========

    /**
     * Checks if this effect deals damage over time.
     *
     * @return true if this is a DOT effect
     */
    public boolean isDamageOverTime() {
        return type == EffectType.DOT && damagePerTick > 0;
    }

    /**
     * Checks if this effect heals over time.
     *
     * @return true if this is a HOT effect
     */
    public boolean isHealOverTime() {
        return type == EffectType.HOT && healingPerTick > 0;
    }

    /**
     * Checks if this effect is positive/beneficial.
     *
     * @return true if this is a beneficial effect
     */
    public boolean isPositive() {
        return type.isPositive();
    }

    /**
     * Gets the total number of ticks for the effect's duration.
     *
     * @return the number of ticks
     */
    public int getTotalTicks() {
        if (tickIntervalMillis <= 0) return 0;
        return durationMillis / tickIntervalMillis;
    }

    /**
     * Calculates total damage for the full duration (per stack).
     *
     * @return total damage over duration
     */
    public double getTotalDamage() {
        return damagePerTick * getTotalTicks();
    }

    /**
     * Calculates total healing for the full duration (per stack).
     *
     * @return total healing over duration
     */
    public double getTotalHealing() {
        return healingPerTick * getTotalTicks();
    }

    // ========== JSON Loading ==========

    /**
     * Loads a CombatEffect from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded CombatEffect
     * @throws IOException if the file cannot be read
     */
    public static CombatEffect fromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return fromJson(reader);
        }
    }

    /**
     * Loads a CombatEffect from a JSON reader.
     *
     * @param reader the reader containing JSON data
     * @return the loaded CombatEffect
     */
    public static CombatEffect fromJson(Reader reader) {
        JsonEffectData data = GSON.fromJson(reader, JsonEffectData.class);
        return data.toEffect();
    }

    /**
     * Loads a CombatEffect from a JSON string.
     *
     * @param json the JSON string
     * @return the loaded CombatEffect
     */
    public static CombatEffect fromJsonString(String json) {
        JsonEffectData data = GSON.fromJson(json, JsonEffectData.class);
        return data.toEffect();
    }

    /**
     * Converts this effect to a JSON string.
     *
     * @return the JSON representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    // ========== Builder ==========

    /**
     * Creates a new Builder for constructing CombatEffect instances.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder pre-populated with this effect's values.
     *
     * @return a new Builder with copied values
     */
    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .displayName(displayName)
                .description(description)
                .iconPath(iconPath)
                .type(type)
                .category(category)
                .duration(durationMillis)
                .tickInterval(tickIntervalMillis)
                .attributeModifiers(new EnumMap<>(attributeModifiers))
                .damagePerTick(damagePerTick)
                .healingPerTick(healingPerTick)
                .damageType(damageType)
                .stackable(stackable)
                .maxStacks(maxStacks)
                .particleEffect(particleEffect)
                .removeOnDeath(removeOnDeath)
                .canDispel(canDispel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombatEffect that = (CombatEffect) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CombatEffect{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                ", category=" + category +
                ", duration=" + durationMillis + "ms" +
                '}';
    }

    /**
     * Builder class for constructing CombatEffect instances.
     */
    public static final class Builder {
        private String id;
        private String displayName;
        private String description;
        private String iconPath;
        private EffectType type;
        private EffectCategory category;
        private int durationMillis = 5000;
        private int tickIntervalMillis = 1000;
        private Map<AttributeType, Double> attributeModifiers;
        private double damagePerTick;
        private double healingPerTick;
        private DamageType damageType;
        private boolean stackable;
        private int maxStacks = 1;
        private String particleEffect;
        private boolean removeOnDeath = true;
        private boolean canDispel = true;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }

        public Builder type(EffectType type) {
            this.type = type;
            return this;
        }

        public Builder category(EffectCategory category) {
            this.category = category;
            return this;
        }

        public Builder duration(int millis) {
            this.durationMillis = millis;
            return this;
        }

        public Builder tickInterval(int millis) {
            this.tickIntervalMillis = millis;
            return this;
        }

        public Builder attributeModifiers(Map<AttributeType, Double> modifiers) {
            this.attributeModifiers = modifiers;
            return this;
        }

        public Builder addAttributeModifier(AttributeType attribute, double value) {
            if (this.attributeModifiers == null) {
                this.attributeModifiers = new EnumMap<>(AttributeType.class);
            }
            this.attributeModifiers.put(attribute, value);
            return this;
        }

        public Builder damagePerTick(double damage) {
            this.damagePerTick = damage;
            return this;
        }

        public Builder healingPerTick(double healing) {
            this.healingPerTick = healing;
            return this;
        }

        public Builder damageType(DamageType type) {
            this.damageType = type;
            return this;
        }

        public Builder stackable(boolean stackable) {
            this.stackable = stackable;
            return this;
        }

        public Builder maxStacks(int maxStacks) {
            this.maxStacks = maxStacks;
            return this;
        }

        public Builder particleEffect(String particleEffect) {
            this.particleEffect = particleEffect;
            return this;
        }

        public Builder removeOnDeath(boolean remove) {
            this.removeOnDeath = remove;
            return this;
        }

        public Builder canDispel(boolean canDispel) {
            this.canDispel = canDispel;
            return this;
        }

        /**
         * Builds the CombatEffect instance.
         *
         * @return the constructed CombatEffect
         * @throws NullPointerException if id is null
         */
        public CombatEffect build() {
            return new CombatEffect(this);
        }
    }

    /**
     * Internal class for JSON deserialization with flexible field mapping.
     */
    private static class JsonEffectData {
        @SerializedName("id")
        String id;

        @SerializedName("display_name")
        String displayName;

        @SerializedName("description")
        String description;

        @SerializedName("icon_path")
        String iconPath;

        @SerializedName("type")
        String type;

        @SerializedName("category")
        String category;

        @SerializedName("duration_millis")
        int durationMillis;

        @SerializedName("tick_interval_millis")
        int tickIntervalMillis;

        @SerializedName("attribute_modifiers")
        Map<String, Double> attributeModifiers;

        @SerializedName("damage_per_tick")
        double damagePerTick;

        @SerializedName("healing_per_tick")
        double healingPerTick;

        @SerializedName("damage_type")
        String damageType;

        @SerializedName("stackable")
        boolean stackable;

        @SerializedName("max_stacks")
        int maxStacks = 1;

        @SerializedName("particle_effect")
        String particleEffect;

        @SerializedName("remove_on_death")
        boolean removeOnDeath = true;

        @SerializedName("can_dispel")
        boolean canDispel = true;

        CombatEffect toEffect() {
            Builder builder = CombatEffect.builder()
                    .id(id)
                    .displayName(displayName)
                    .description(description)
                    .iconPath(iconPath)
                    .duration(durationMillis)
                    .tickInterval(tickIntervalMillis > 0 ? tickIntervalMillis : 1000)
                    .damagePerTick(damagePerTick)
                    .healingPerTick(healingPerTick)
                    .stackable(stackable)
                    .maxStacks(maxStacks > 0 ? maxStacks : 1)
                    .particleEffect(particleEffect)
                    .removeOnDeath(removeOnDeath)
                    .canDispel(canDispel);

            // Parse type
            if (type != null) {
                try {
                    builder.type(EffectType.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Unknown effect type: " + type);
                }
            }

            // Parse category
            if (category != null) {
                try {
                    builder.category(EffectCategory.valueOf(category.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Unknown effect category: " + category);
                }
            }

            // Parse damage type
            if (damageType != null) {
                try {
                    builder.damageType(DamageType.valueOf(damageType.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Unknown damage type: " + damageType);
                }
            }

            // Parse attribute modifiers
            if (attributeModifiers != null) {
                Map<AttributeType, Double> modMap = new EnumMap<>(AttributeType.class);
                for (Map.Entry<String, Double> entry : attributeModifiers.entrySet()) {
                    try {
                        AttributeType attr = AttributeType.valueOf(entry.getKey().toUpperCase());
                        modMap.put(attr, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.WARNING, "Unknown attribute type: " + entry.getKey());
                    }
                }
                builder.attributeModifiers(modMap);
            }

            return builder.build();
        }
    }
}
