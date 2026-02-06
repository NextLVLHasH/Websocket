package com.NextLVLHasH.Websockets.rpg.attributes;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.Map;

/**
 * Enumeration of derived stats that are calculated from base attributes.
 * Each derived stat has a formula that takes attribute values and player level
 * to calculate the final stat value.
 * 
 * <p>Derived stats represent secondary statistics like health, mana, damage,
 * and various combat-related values that depend on the primary attributes.
 * 
 * <p>Default formulas:
 * <ul>
 *   <li>MAX_HEALTH = 100 + (CONSTITUTION * 10) + (level * 5)</li>
 *   <li>MAX_MANA = 50 + (INTELLIGENCE * 8) + (WISDOM * 2)</li>
 *   <li>MAX_STAMINA = 80 + (CONSTITUTION * 5) + (DEXTERITY * 3)</li>
 *   <li>HEALTH_REGEN = 1 + (CONSTITUTION * 0.1)</li>
 *   <li>MANA_REGEN = 1 + (WISDOM * 0.15)</li>
 *   <li>STAMINA_REGEN = 5 + (CONSTITUTION * 0.2)</li>
 *   <li>PHYSICAL_DAMAGE = 10 + (STRENGTH * 2)</li>
 *   <li>MAGICAL_DAMAGE = 10 + (INTELLIGENCE * 2.5)</li>
 *   <li>CRITICAL_CHANCE = 0.05 + (DEXTERITY * 0.005)</li>
 *   <li>CRITICAL_DAMAGE = 1.5 + (STRENGTH * 0.01)</li>
 *   <li>DODGE_CHANCE = (DEXTERITY * 0.003)</li>
 *   <li>MOVEMENT_SPEED = 100 + (DEXTERITY * 0.5)</li>
 *   <li>ATTACK_SPEED = 1.0 + (DEXTERITY * 0.01)</li>
 *   <li>ARMOR = (CONSTITUTION * 2) + (STRENGTH * 0.5)</li>
 *   <li>MAGIC_RESIST = (WISDOM * 2) + (INTELLIGENCE * 0.5)</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum DerivedStat {

    /**
     * Maximum health points.
     * Formula: 100 + (CONSTITUTION * 10) + (level * 5)
     */
    MAX_HEALTH(
        "Max Health",
        "Maximum hit points",
        "HP",
        (attrs, level) -> 100 + (attrs.getOrDefault(AttributeType.CONSTITUTION, 10) * 10) + (level * 5)
    ),

    /**
     * Maximum mana points.
     * Formula: 50 + (INTELLIGENCE * 8) + (WISDOM * 2)
     */
    MAX_MANA(
        "Max Mana",
        "Maximum mana for casting spells",
        "MP",
        (attrs, level) -> 50 + (attrs.getOrDefault(AttributeType.INTELLIGENCE, 10) * 8) 
                            + (attrs.getOrDefault(AttributeType.WISDOM, 10) * 2)
    ),

    /**
     * Maximum stamina points.
     * Formula: 80 + (CONSTITUTION * 5) + (DEXTERITY * 3)
     */
    MAX_STAMINA(
        "Max Stamina",
        "Maximum stamina for physical abilities",
        "SP",
        (attrs, level) -> 80 + (attrs.getOrDefault(AttributeType.CONSTITUTION, 10) * 5) 
                            + (attrs.getOrDefault(AttributeType.DEXTERITY, 10) * 3)
    ),

    /**
     * Health regeneration per second.
     * Formula: 1 + (CONSTITUTION * 0.1)
     */
    HEALTH_REGEN(
        "Health Regen",
        "Health regenerated per second",
        "HP/s",
        (attrs, level) -> 1 + (attrs.getOrDefault(AttributeType.CONSTITUTION, 10) * 0.1)
    ),

    /**
     * Mana regeneration per second.
     * Formula: 1 + (WISDOM * 0.15)
     */
    MANA_REGEN(
        "Mana Regen",
        "Mana regenerated per second",
        "MP/s",
        (attrs, level) -> 1 + (attrs.getOrDefault(AttributeType.WISDOM, 10) * 0.15)
    ),

    /**
     * Stamina regeneration per second.
     * Formula: 5 + (CONSTITUTION * 0.2)
     */
    STAMINA_REGEN(
        "Stamina Regen",
        "Stamina regenerated per second",
        "SP/s",
        (attrs, level) -> 5 + (attrs.getOrDefault(AttributeType.CONSTITUTION, 10) * 0.2)
    ),

    /**
     * Physical damage modifier.
     * Formula: 10 + (STRENGTH * 2)
     */
    PHYSICAL_DAMAGE(
        "Physical Damage",
        "Bonus physical damage dealt",
        "DMG",
        (attrs, level) -> 10 + (attrs.getOrDefault(AttributeType.STRENGTH, 10) * 2)
    ),

    /**
     * Magical damage modifier.
     * Formula: 10 + (INTELLIGENCE * 2.5)
     */
    MAGICAL_DAMAGE(
        "Magical Damage",
        "Bonus magical damage dealt",
        "MDMG",
        (attrs, level) -> 10 + (attrs.getOrDefault(AttributeType.INTELLIGENCE, 10) * 2.5)
    ),

    /**
     * Critical hit chance (0.0 to 1.0).
     * Formula: 0.05 + (DEXTERITY * 0.005)
     */
    CRITICAL_CHANCE(
        "Critical Chance",
        "Chance to deal critical hits",
        "%",
        (attrs, level) -> 0.05 + (attrs.getOrDefault(AttributeType.DEXTERITY, 10) * 0.005)
    ),

    /**
     * Critical damage multiplier.
     * Formula: 1.5 + (STRENGTH * 0.01)
     */
    CRITICAL_DAMAGE(
        "Critical Damage",
        "Critical hit damage multiplier",
        "x",
        (attrs, level) -> 1.5 + (attrs.getOrDefault(AttributeType.STRENGTH, 10) * 0.01)
    ),

    /**
     * Dodge chance (0.0 to 1.0).
     * Formula: DEXTERITY * 0.003
     */
    DODGE_CHANCE(
        "Dodge Chance",
        "Chance to dodge incoming attacks",
        "%",
        (attrs, level) -> attrs.getOrDefault(AttributeType.DEXTERITY, 10) * 0.003
    ),

    /**
     * Movement speed (percentage of base).
     * Formula: 100 + (DEXTERITY * 0.5)
     */
    MOVEMENT_SPEED(
        "Movement Speed",
        "Movement speed percentage",
        "%",
        (attrs, level) -> 100 + (attrs.getOrDefault(AttributeType.DEXTERITY, 10) * 0.5)
    ),

    /**
     * Attack speed multiplier.
     * Formula: 1.0 + (DEXTERITY * 0.01)
     */
    ATTACK_SPEED(
        "Attack Speed",
        "Attack speed multiplier",
        "x",
        (attrs, level) -> 1.0 + (attrs.getOrDefault(AttributeType.DEXTERITY, 10) * 0.01)
    ),

    /**
     * Physical armor/damage reduction.
     * Formula: (CONSTITUTION * 2) + (STRENGTH * 0.5)
     */
    ARMOR(
        "Armor",
        "Physical damage reduction",
        "DEF",
        (attrs, level) -> (attrs.getOrDefault(AttributeType.CONSTITUTION, 10) * 2) 
                        + (attrs.getOrDefault(AttributeType.STRENGTH, 10) * 0.5)
    ),

    /**
     * Magic resistance.
     * Formula: (WISDOM * 2) + (INTELLIGENCE * 0.5)
     */
    MAGIC_RESIST(
        "Magic Resist",
        "Magical damage reduction",
        "MDEF",
        (attrs, level) -> (attrs.getOrDefault(AttributeType.WISDOM, 10) * 2) 
                        + (attrs.getOrDefault(AttributeType.INTELLIGENCE, 10) * 0.5)
    );

    private final String displayName;
    private final String description;
    private final String suffix;
    private final DerivedStatFormula formula;

    /**
     * Constructs a DerivedStat with the specified properties.
     *
     * @param displayName the human-readable name
     * @param description a brief description
     * @param suffix the unit suffix for display (e.g., "HP", "%")
     * @param formula the calculation formula
     */
    DerivedStat(String displayName, String description, String suffix, DerivedStatFormula formula) {
        this.displayName = displayName;
        this.description = description;
        this.suffix = suffix;
        this.formula = formula;
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
     * Gets the description of this stat.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the unit suffix for display.
     *
     * @return the suffix (e.g., "HP", "%", "x")
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Calculates this derived stat using the provided attributes and level.
     *
     * @param attributes map of attribute types to their values
     * @param level the player's level
     * @return the calculated stat value
     */
    public double calculate(Map<AttributeType, Integer> attributes, int level) {
        return formula.calculate(attributes, level);
    }

    /**
     * Calculates this derived stat using an AttributeSet.
     *
     * @param attributeSet the attribute set to use
     * @param level the player's level
     * @return the calculated stat value
     */
    public double calculate(AttributeSet attributeSet, int level) {
        return formula.calculate(attributeSet.getAllValues(), level);
    }

    /**
     * Formats the value for display.
     *
     * @param value the calculated value
     * @return formatted string with appropriate precision and suffix
     */
    public String formatValue(double value) {
        return switch (this) {
            case CRITICAL_CHANCE, DODGE_CHANCE -> String.format("%.1f%%", value * 100);
            case MOVEMENT_SPEED -> String.format("%.0f%%", value);
            case CRITICAL_DAMAGE, ATTACK_SPEED -> String.format("%.2fx", value);
            case HEALTH_REGEN, MANA_REGEN, STAMINA_REGEN -> String.format("%.1f/s", value);
            default -> String.format("%.0f", value);
        };
    }

    /**
     * Gets the formula used to calculate this stat.
     *
     * @return the formula interface
     */
    public DerivedStatFormula getFormula() {
        return formula;
    }

    /**
     * Checks if this stat represents a percentage (0.0 to 1.0 range).
     *
     * @return true if this is a percentage stat
     */
    public boolean isPercentage() {
        return this == CRITICAL_CHANCE || this == DODGE_CHANCE;
    }

    /**
     * Checks if this stat represents a multiplier.
     *
     * @return true if this is a multiplier stat
     */
    public boolean isMultiplier() {
        return this == CRITICAL_DAMAGE || this == ATTACK_SPEED;
    }

    /**
     * Checks if this stat represents a resource pool (health, mana, stamina).
     *
     * @return true if this is a resource pool stat
     */
    public boolean isResourcePool() {
        return this == MAX_HEALTH || this == MAX_MANA || this == MAX_STAMINA;
    }

    /**
     * Checks if this stat represents a regeneration rate.
     *
     * @return true if this is a regen stat
     */
    public boolean isRegeneration() {
        return this == HEALTH_REGEN || this == MANA_REGEN || this == STAMINA_REGEN;
    }

    /**
     * Finds a DerivedStat by its display name (case-insensitive).
     *
     * @param name the display name to search for
     * @return the matching DerivedStat, or null if not found
     */
    public static DerivedStat fromDisplayName(String name) {
        if (name == null) return null;
        for (DerivedStat stat : values()) {
            if (stat.displayName.equalsIgnoreCase(name)) {
                return stat;
            }
        }
        return null;
    }

    /**
     * Functional interface for derived stat calculation formulas.
     * Takes attribute values and player level, returns the calculated stat value.
     */
    @FunctionalInterface
    public interface DerivedStatFormula {
        /**
         * Calculates the derived stat value.
         *
         * @param attributes map of attribute types to their values
         * @param level the player's level
         * @return the calculated stat value
         */
        double calculate(Map<AttributeType, Integer> attributes, int level);
    }
}
