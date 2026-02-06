package com.NextLVLHasH.Websockets.rpg.restrictions;

/**
 * Enumeration of all weapon types available in the RPG system.
 * Each weapon type has properties defining its combat characteristics.
 */
public enum WeaponType {
    
    SWORD(true, false, false, false, "Sword", "A versatile melee blade"),
    AXE(true, false, false, false, "Axe", "A heavy chopping weapon"),
    MACE(true, false, false, false, "Mace", "A blunt crushing weapon"),
    SPEAR(true, false, false, true, "Spear", "A long-reach piercing weapon"),
    DAGGER(true, false, false, false, "Dagger", "A quick, lightweight blade"),
    BOW(false, true, false, true, "Bow", "A ranged weapon requiring arrows"),
    CROSSBOW(false, true, false, true, "Crossbow", "A mechanical ranged weapon"),
    STAFF(false, false, true, true, "Staff", "A magical conduit for spellcasting"),
    WAND(false, false, true, false, "Wand", "A lightweight magical implement"),
    SHIELD(true, false, false, false, "Shield", "A defensive equipment piece"),
    FIST(true, false, false, false, "Fist", "Unarmed combat");

    private final boolean melee;
    private final boolean ranged;
    private final boolean magical;
    private final boolean twoHanded;
    private final String displayName;
    private final String description;

    /**
     * Constructs a WeaponType with the specified properties.
     *
     * @param melee       whether this is a melee weapon
     * @param ranged      whether this is a ranged weapon
     * @param magical     whether this is a magical weapon
     * @param twoHanded   whether this requires two hands
     * @param displayName the human-readable name
     * @param description a brief description of the weapon type
     */
    WeaponType(boolean melee, boolean ranged, boolean magical, boolean twoHanded,
               String displayName, String description) {
        this.melee = melee;
        this.ranged = ranged;
        this.magical = magical;
        this.twoHanded = twoHanded;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Checks if this is a melee weapon.
     *
     * @return true if melee, false otherwise
     */
    public boolean isMelee() {
        return melee;
    }

    /**
     * Checks if this is a ranged weapon.
     *
     * @return true if ranged, false otherwise
     */
    public boolean isRanged() {
        return ranged;
    }

    /**
     * Checks if this is a magical weapon.
     *
     * @return true if magical, false otherwise
     */
    public boolean isMagical() {
        return magical;
    }

    /**
     * Checks if this weapon requires two hands.
     *
     * @return true if two-handed, false otherwise
     */
    public boolean isTwoHanded() {
        return twoHanded;
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
     * Gets the description of this weapon type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds a WeaponType by its name (case-insensitive).
     *
     * @param name the name to search for
     * @return the matching WeaponType, or null if not found
     */
    public static WeaponType fromString(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase();
        try {
            return WeaponType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (WeaponType type : values()) {
                if (type.displayName.equalsIgnoreCase(name.trim())) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Checks if this weapon can be used with a shield (must be one-handed melee).
     *
     * @return true if compatible with a shield
     */
    public boolean isShieldCompatible() {
        return !twoHanded && melee && this != SHIELD;
    }

    /**
     * Gets the primary attribute that affects this weapon's effectiveness.
     *
     * @return the primary scaling attribute name
     */
    public String getPrimaryAttribute() {
        if (magical) {
            return "INTELLIGENCE";
        } else if (ranged) {
            return "DEXTERITY";
        } else {
            return "STRENGTH";
        }
    }
}
