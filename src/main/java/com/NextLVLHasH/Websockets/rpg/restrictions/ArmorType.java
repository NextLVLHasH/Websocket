package com.NextLVLHasH.Websockets.rpg.restrictions;

/**
 * Enumeration of all armor types available in the RPG system.
 * Each armor type has specific penalties and multipliers affecting gameplay.
 */
public enum ArmorType {
    
    HEAVY(0.30, 2.0, "Heavy Armor", "Plate and mail providing maximum protection"),
    MEDIUM(0.15, 1.5, "Medium Armor", "Leather and chain offering balanced protection"),
    LIGHT(0.05, 1.0, "Light Armor", "Studded leather providing minimal encumbrance"),
    CLOTH(0.0, 0.5, "Cloth Armor", "Robes and garments with magical properties"),
    NONE(0.0, 0.0, "No Armor", "Unarmored, maximum mobility");

    private final double movementPenalty;
    private final double armorMultiplier;
    private final String displayName;
    private final String description;

    /**
     * Constructs an ArmorType with the specified properties.
     *
     * @param movementPenalty the movement speed reduction (0.0 to 1.0, where 0.3 = 30% slower)
     * @param armorMultiplier the armor value multiplier for damage reduction
     * @param displayName     the human-readable name
     * @param description     a brief description of the armor type
     */
    ArmorType(double movementPenalty, double armorMultiplier, String displayName, String description) {
        this.movementPenalty = movementPenalty;
        this.armorMultiplier = armorMultiplier;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the movement speed penalty as a decimal (0.3 = 30% slower).
     *
     * @return the movement penalty
     */
    public double getMovementPenalty() {
        return movementPenalty;
    }

    /**
     * Gets the armor multiplier for damage reduction calculations.
     *
     * @return the armor multiplier
     */
    public double getArmorMultiplier() {
        return armorMultiplier;
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
     * Gets the description of this armor type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Calculates the effective movement speed after applying this armor's penalty.
     *
     * @param baseSpeed the base movement speed
     * @return the effective movement speed
     */
    public double calculateEffectiveSpeed(double baseSpeed) {
        return baseSpeed * (1.0 - movementPenalty);
    }

    /**
     * Calculates the effective armor value with this type's multiplier.
     *
     * @param baseArmor the base armor value of the equipment
     * @return the effective armor value
     */
    public double calculateEffectiveArmor(double baseArmor) {
        return baseArmor * armorMultiplier;
    }

    /**
     * Checks if this armor type allows spellcasting without penalty.
     *
     * @return true if spellcasting is unimpeded
     */
    public boolean allowsUnimpededSpellcasting() {
        return this == CLOTH || this == NONE;
    }

    /**
     * Checks if this armor type requires strength to wear effectively.
     *
     * @return true if strength requirement applies
     */
    public boolean requiresStrength() {
        return this == HEAVY || this == MEDIUM;
    }

    /**
     * Gets the minimum strength recommended for this armor type.
     *
     * @return the recommended minimum strength value
     */
    public int getRecommendedStrength() {
        return switch (this) {
            case HEAVY -> 16;
            case MEDIUM -> 12;
            case LIGHT -> 8;
            case CLOTH, NONE -> 1;
        };
    }

    /**
     * Finds an ArmorType by its name (case-insensitive).
     *
     * @param name the name to search for
     * @return the matching ArmorType, or null if not found
     */
    public static ArmorType fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase();
        try {
            return ArmorType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (ArmorType type : values()) {
                if (type.displayName.equalsIgnoreCase(name.trim())) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Gets the stealth penalty for this armor type.
     * Higher values indicate more noise/visibility.
     *
     * @return the stealth penalty (0.0 to 1.0)
     */
    public double getStealthPenalty() {
        return switch (this) {
            case HEAVY -> 0.50;
            case MEDIUM -> 0.25;
            case LIGHT -> 0.10;
            case CLOTH -> 0.0;
            case NONE -> 0.0;
        };
    }
}
