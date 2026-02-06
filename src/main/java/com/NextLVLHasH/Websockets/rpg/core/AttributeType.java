package com.NextLVLHasH.Websockets.rpg.core;

import java.awt.Color;

/**
 * Enumeration of all RPG attribute types available in the system.
 * Each attribute has a display name, description, and associated color for UI rendering.
 */
public enum AttributeType {
    
    STRENGTH(
        "Strength",
        "Physical power affecting melee damage and carrying capacity",
        new Color(220, 50, 50)  // Red
    ),
    
    DEXTERITY(
        "Dexterity",
        "Agility and reflexes affecting attack speed, dodge chance, and ranged accuracy",
        new Color(50, 200, 50)  // Green
    ),
    
    INTELLIGENCE(
        "Intelligence",
        "Mental acuity affecting magic damage, mana pool, and spell effectiveness",
        new Color(50, 100, 220)  // Blue
    ),
    
    CONSTITUTION(
        "Constitution",
        "Physical resilience affecting health pool, stamina, and resistance to effects",
        new Color(200, 150, 50)  // Orange/Gold
    ),
    
    WISDOM(
        "Wisdom",
        "Spiritual insight affecting mana regeneration, healing power, and cooldown reduction",
        new Color(180, 50, 200)  // Purple
    ),
    
    CHARISMA(
        "Charisma",
        "Personal magnetism affecting NPC interactions, prices, and party buffs",
        new Color(255, 200, 50)  // Yellow/Gold
    );

    private final String displayName;
    private final String description;
    private final Color color;

    /**
     * Constructs an AttributeType with the specified properties.
     *
     * @param displayName the human-readable name of the attribute
     * @param description a brief description of what the attribute affects
     * @param color the color associated with this attribute for UI purposes
     */
    AttributeType(String displayName, String description, Color color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    /**
     * Gets the human-readable display name of this attribute.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description explaining what this attribute affects.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the color associated with this attribute for UI rendering.
     *
     * @return the Color object
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the hex color string for this attribute (useful for UI).
     *
     * @return hex color string in format "#RRGGBB"
     */
    public String getHexColor() {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Finds an AttributeType by its display name (case-insensitive).
     *
     * @param name the display name to search for
     * @return the matching AttributeType, or null if not found
     */
    public static AttributeType fromDisplayName(String name) {
        if (name == null) return null;
        for (AttributeType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
