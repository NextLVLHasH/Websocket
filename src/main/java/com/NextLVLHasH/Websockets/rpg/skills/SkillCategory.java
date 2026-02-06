package com.NextLVLHasH.Websockets.rpg.skills;

/**
 * Enumeration of skill categories in the RPG system.
 * 
 * Categories organize skills into three main branches:
 * <ul>
 *   <li>COMBAT - PvP, PvE, damage dealing, defense, and combat techniques</li>
 *   <li>UTILITIES - Mining, building, crafting, and general utility skills</li>
 *   <li>MAGIC - Spells, enchantments, magical abilities, and mana-based skills</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum SkillCategory {
    
    /**
     * Combat skills for PvP and PvE encounters.
     * Includes: Attack skills, defensive abilities, weapon mastery, combat techniques.
     * XP Source: Killing mobs (PvE) and killing players (PvP).
     */
    COMBAT("Combat", "Combat skills for PvP and PvE encounters", "#CC3333", "⚔"),
    
    /**
     * Utility skills for mining, building, and crafting.
     * Includes: Mining efficiency, building speed, resource gathering, tool mastery.
     * XP Source: Mining blocks and placing/building blocks.
     */
    UTILITIES("Utilities", "Mining, building, and crafting skills", "#33AA33", "🔧"),
    
    /**
     * Magic skills for spells and enchantments.
     * Includes: Offensive spells, healing, buffs, elemental magic, enchanting.
     * XP Source: Casting spells, using magical items, mana expenditure.
     */
    MAGIC("Magic", "Spells, enchantments, and magical abilities", "#6633CC", "✨");
    
    private final String displayName;
    private final String description;
    private final String color;
    private final String icon;
    
    /**
     * Constructs a SkillCategory.
     *
     * @param displayName human-readable name
     * @param description brief description of the category
     * @param color hex color code for UI display
     * @param icon emoji/icon for display
     */
    SkillCategory(String displayName, String description, String color, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.icon = icon;
    }
    
    /**
     * Gets the display name of this category.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of this category.
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the hex color code for UI display.
     * @return the color code (e.g., "#CC3333")
     */
    public String getColor() {
        return color;
    }
    
    /**
     * Gets the icon/emoji for this category.
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * Checks if skills in this category provide combat bonuses.
     * @return true if combat-focused
     */
    public boolean isCombatFocused() {
        return this == COMBAT;
    }
    
    /**
     * Checks if skills in this category provide utility bonuses.
     * @return true if utility-focused
     */
    public boolean isUtilityFocused() {
        return this == UTILITIES;
    }
    
    /**
     * Checks if skills in this category involve magic.
     * @return true if magic-focused
     */
    public boolean isMagicFocused() {
        return this == MAGIC;
    }
    
    /**
     * Gets a category by its name (case-insensitive).
     * 
     * @param name the category name
     * @return the matching category, or null if not found
     */
    public static SkillCategory fromName(String name) {
        if (name == null) return null;
        
        for (SkillCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) || 
                category.displayName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }
    
    /**
     * Gets the XP sources for this category.
     * 
     * @return description of how to earn XP in this category
     */
    public String getXPSources() {
        return switch (this) {
            case COMBAT -> "Kill mobs (PvE) and players (PvP)";
            case UTILITIES -> "Mine blocks and build structures";
            case MAGIC -> "Cast spells and use magical items";
        };
    }
}
