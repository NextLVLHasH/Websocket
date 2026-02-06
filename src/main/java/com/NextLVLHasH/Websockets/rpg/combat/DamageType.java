package com.NextLVLHasH.Websockets.rpg.combat;

/**
 * Enumeration of all damage types in the combat system.
 * Each type has properties indicating whether it's magical or elemental,
 * and an associated display color for UI rendering.
 * 
 * <p>Damage types affect how damage is calculated and mitigated:
 * <ul>
 *     <li>Physical damage is reduced by armor</li>
 *     <li>Magical damage is reduced by magic resistance</li>
 *     <li>Elemental damage may have additional effects</li>
 *     <li>True damage ignores all defenses</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum DamageType {
    
    /**
     * Standard physical damage, reduced by armor.
     */
    PHYSICAL(false, false, "#C0C0C0"),
    
    /**
     * Pure magical damage, reduced by magic resistance.
     */
    MAGICAL(true, false, "#9966FF"),
    
    /**
     * True damage that bypasses all defenses.
     */
    TRUE(false, false, "#FFFFFF"),
    
    /**
     * Fire elemental damage, may apply burning effects.
     */
    FIRE(true, true, "#FF4500"),
    
    /**
     * Ice elemental damage, may apply slowing effects.
     */
    ICE(true, true, "#00BFFF"),
    
    /**
     * Lightning elemental damage, may apply shock effects.
     */
    LIGHTNING(true, true, "#FFD700"),
    
    /**
     * Poison damage, typically damage over time.
     */
    POISON(true, true, "#32CD32"),
    
    /**
     * Nature elemental damage, associated with druidic magic.
     */
    NATURE(true, true, "#228B22"),
    
    /**
     * Arcane magical damage, pure magical energy.
     */
    ARCANE(true, false, "#DA70D6"),
    
    /**
     * Holy damage, effective against undead and demons.
     */
    HOLY(true, false, "#FFFACD"),
    
    /**
     * Dark/Shadow damage, corrupted magical energy.
     */
    DARK(true, false, "#4B0082");

    private final boolean magical;
    private final boolean elemental;
    private final String color;

    /**
     * Constructs a DamageType with the specified properties.
     *
     * @param magical whether this damage type is magical
     * @param elemental whether this damage type is elemental
     * @param color the hex color code for UI display
     */
    DamageType(boolean magical, boolean elemental, String color) {
        this.magical = magical;
        this.elemental = elemental;
        this.color = color;
    }

    /**
     * Checks if this damage type is magical.
     * Magical damage is typically mitigated by magic resistance.
     *
     * @return true if this damage type is magical
     */
    public boolean isMagical() {
        return magical;
    }

    /**
     * Checks if this damage type is elemental.
     * Elemental damage may have special interactions with resistances
     * and can trigger additional status effects.
     *
     * @return true if this damage type is elemental
     */
    public boolean isElemental() {
        return elemental;
    }

    /**
     * Gets the hex color code for this damage type.
     * Used for UI rendering of damage numbers and effects.
     *
     * @return the hex color string in format "#RRGGBB"
     */
    public String getColor() {
        return color;
    }

    /**
     * Gets the display name for this damage type.
     * Converts the enum name to a user-friendly format.
     *
     * @return the display name
     */
    public String getDisplayName() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Checks if this damage type is physical.
     * Physical damage is mitigated by armor.
     *
     * @return true if this damage type is physical (non-magical)
     */
    public boolean isPhysical() {
        return !magical && this != TRUE;
    }

    /**
     * Checks if this damage type ignores defenses.
     * Only TRUE damage bypasses all damage mitigation.
     *
     * @return true if this damage type ignores defenses
     */
    public boolean ignoresDefenses() {
        return this == TRUE;
    }

    /**
     * Finds a DamageType by name (case-insensitive).
     *
     * @param name the name to search for
     * @return the matching DamageType, or null if not found
     */
    public static DamageType fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
