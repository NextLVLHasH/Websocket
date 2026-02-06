package com.NextLVLHasH.Websockets.rpg.skills;

/**
 * Enumeration of targeting types for skills in the RPG system.
 * <p>
 * Target types define what a skill can be aimed at and how it selects
 * its targets. This affects both the UI (cursor behavior) and the
 * skill execution logic.
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public enum TargetType {

    /**
     * Skill targets the caster themselves.
     * Used for self-buffs, heals, and defensive abilities.
     */
    SELF(false, false),

    /**
     * Skill targets an enemy entity.
     * Used for attacks, debuffs, and offensive abilities.
     */
    ENEMY(true, true),

    /**
     * Skill targets an allied entity (including self).
     * Used for heals, buffs, and support abilities.
     */
    ALLY(true, false),

    /**
     * Skill targets a point on the ground.
     * Used for area-of-effect abilities and summons.
     */
    GROUND_POINT(true, false),

    /**
     * Skill fires in a direction from the caster.
     * Used for projectiles and cone/line attacks.
     */
    DIRECTION(true, false),

    /**
     * Skill has no target and activates immediately.
     * Used for auras, toggles, and self-centered effects.
     */
    NONE(false, false);

    private final boolean requiresTarget;
    private final boolean hostile;

    /**
     * Constructs a TargetType with the specified properties.
     *
     * @param requiresTarget whether the skill requires target selection
     * @param hostile whether the skill is hostile (damages/debuffs targets)
     */
    TargetType(boolean requiresTarget, boolean hostile) {
        this.requiresTarget = requiresTarget;
        this.hostile = hostile;
    }

    /**
     * Checks if this target type requires the player to select a target.
     * <p>
     * Skills with target requirements will enter a targeting mode when activated,
     * waiting for the player to select a valid target before executing.
     * </p>
     *
     * @return true if target selection is required
     */
    public boolean requiresTarget() {
        return requiresTarget;
    }

    /**
     * Checks if this target type is hostile (typically used against enemies).
     * <p>
     * Hostile skills will generally not work on friendly targets and may
     * trigger combat mechanics like aggro.
     * </p>
     *
     * @return true if this is a hostile targeting type
     */
    public boolean isHostile() {
        return hostile;
    }

    /**
     * Checks if this target type targets an entity (as opposed to a location).
     *
     * @return true if this targets an entity
     */
    public boolean targetsEntity() {
        return this == ENEMY || this == ALLY || this == SELF;
    }

    /**
     * Checks if this target type targets a location (ground or direction).
     *
     * @return true if this targets a location
     */
    public boolean targetsLocation() {
        return this == GROUND_POINT || this == DIRECTION;
    }

    /**
     * Checks if this target type can affect allies.
     *
     * @return true if the skill can target allies
     */
    public boolean canTargetAllies() {
        return this == ALLY || this == SELF || this == GROUND_POINT;
    }

    /**
     * Checks if this target type can affect enemies.
     *
     * @return true if the skill can target enemies
     */
    public boolean canTargetEnemies() {
        return this == ENEMY || this == GROUND_POINT || this == DIRECTION;
    }

    /**
     * Finds a TargetType by name (case-insensitive).
     *
     * @param name the name to search for
     * @return the matching TargetType, or null if not found
     */
    public static TargetType fromName(String name) {
        if (name == null) return null;
        for (TargetType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
