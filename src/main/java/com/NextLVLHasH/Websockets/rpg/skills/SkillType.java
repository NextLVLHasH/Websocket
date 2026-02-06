package com.NextLVLHasH.Websockets.rpg.skills;

/**
 * Enumeration of skill types in the RPG system.
 * <p>
 * Skill types categorize abilities based on how they are activated and behave:
 * <ul>
 *   <li>ACTIVE - Must be manually triggered by the player</li>
 *   <li>PASSIVE - Always in effect once learned</li>
 *   <li>TOGGLE - Can be switched on/off by the player</li>
 *   <li>ULTIMATE - Powerful ability with significant cooldown</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public enum SkillType {

    /**
     * An active skill that must be manually triggered by the player.
     * These skills typically consume resources and have cooldowns.
     */
    ACTIVE("Active skills require manual activation and consume resources"),

    /**
     * A passive skill that is always in effect once learned.
     * These skills provide permanent bonuses without player intervention.
     */
    PASSIVE("Passive skills are always active once unlocked"),

    /**
     * A toggle skill that can be switched on or off by the player.
     * While active, these may consume resources over time or modify behavior.
     */
    TOGGLE("Toggle skills can be switched on/off and may drain resources while active"),

    /**
     * An ultimate skill with significant power but long cooldown.
     * These are typically class-defining abilities with dramatic effects.
     */
    ULTIMATE("Ultimate skills are powerful abilities with long cooldowns");

    private final String description;

    /**
     * Constructs a SkillType with the specified description.
     *
     * @param description a brief description of this skill type
     */
    SkillType(String description) {
        this.description = description;
    }

    /**
     * Gets the description explaining this skill type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this skill type requires manual activation.
     *
     * @return true if the skill must be actively triggered
     */
    public boolean requiresActivation() {
        return this == ACTIVE || this == TOGGLE || this == ULTIMATE;
    }

    /**
     * Checks if this skill type has a cooldown.
     *
     * @return true if the skill typically has a cooldown
     */
    public boolean hasCooldown() {
        return this == ACTIVE || this == ULTIMATE;
    }

    /**
     * Checks if this skill type provides permanent effects.
     *
     * @return true if the skill provides passive benefits
     */
    public boolean isPermanentEffect() {
        return this == PASSIVE;
    }

    /**
     * Checks if this skill type can be toggled.
     *
     * @return true if the skill can be switched on/off
     */
    public boolean isToggleable() {
        return this == TOGGLE;
    }

    /**
     * Finds a SkillType by name (case-insensitive).
     *
     * @param name the name to search for
     * @return the matching SkillType, or null if not found
     */
    public static SkillType fromName(String name) {
        if (name == null) return null;
        for (SkillType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
