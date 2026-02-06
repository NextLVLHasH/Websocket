package com.NextLVLHasH.Websockets.rpg.combat;

/**
 * Enumeration of effect categories for dispel mechanics and immunity systems.
 * Each category represents a distinct type of magical or physical affliction
 * that can be targeted by specific dispel abilities or resisted by immunities.
 * 
 * <p>Categories are used for:
 * <ul>
 *     <li>Dispel targeting - cleanse magic effects vs cure poison</li>
 *     <li>Immunity systems - immunity to stun, immunity to poison</li>
 *     <li>Resistance calculations - magic resistance, poison resistance</li>
 *     <li>UI grouping and filtering of active effects</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum EffectCategory {
    
    /**
     * General magical effects that can be dispelled by magic removal abilities.
     * Includes buffs, debuffs, and magical crowd control.
     */
    MAGIC(true, false, "#9966FF"),
    
    /**
     * Dark magic curses with harmful effects.
     * Requires specific curse removal abilities to dispel.
     */
    CURSE(true, false, "#4B0082"),
    
    /**
     * Toxic effects that deal damage over time.
     * Can be cured by antidotes or poison removal abilities.
     */
    POISON(true, false, "#32CD32"),
    
    /**
     * Physical damage over time from wounds.
     * Stopped by bandages, healing, or bleed removal.
     */
    BLEED(false, false, "#8B0000"),
    
    /**
     * Fire-based damage over time.
     * Can be extinguished by water or fire removal abilities.
     */
    BURN(false, false, "#FF4500"),
    
    /**
     * Cold-based effects that slow or immobilize.
     * Movement impairing; removed by warmth or break free abilities.
     */
    FREEZE(true, true, "#00BFFF"),
    
    /**
     * Complete incapacitation preventing all actions.
     * Movement and ability impairing; breaks on damage in some systems.
     */
    STUN(false, true, "#FFD700"),
    
    /**
     * Immobilization that prevents movement but allows actions.
     * Movement impairing only; can still attack and cast.
     */
    ROOT(false, true, "#8B4513"),
    
    /**
     * Prevents spellcasting and ability usage.
     * Does not impair movement; affects casters primarily.
     */
    SILENCE(false, false, "#C0C0C0"),
    
    /**
     * Protective barrier effects that absorb damage.
     * Generally positive; cannot be dispelled by enemies.
     */
    SHIELD(false, false, "#87CEEB"),
    
    /**
     * Area effects that pulse from a source.
     * Usually tied to a source entity; ends when source dies.
     */
    AURA(false, false, "#DDA0DD");

    private final boolean canDispel;
    private final boolean movementImpairing;
    private final String color;

    /**
     * Constructs an EffectCategory with the specified properties.
     *
     * @param canDispel whether effects in this category can be dispelled
     * @param movementImpairing whether this category impairs movement
     * @param color the hex color code for UI display
     */
    EffectCategory(boolean canDispel, boolean movementImpairing, String color) {
        this.canDispel = canDispel;
        this.movementImpairing = movementImpairing;
        this.color = color;
    }

    /**
     * Checks if effects in this category can be dispelled.
     * Some categories like SHIELD cannot be dispelled by enemies.
     *
     * @return true if effects can be dispelled
     */
    public boolean canDispel() {
        return canDispel;
    }

    /**
     * Checks if this category impairs movement.
     * Movement impairing effects include stun, root, and freeze.
     *
     * @return true if this category impairs movement
     */
    public boolean isMovementImpairing() {
        return movementImpairing;
    }

    /**
     * Gets the display color for this category in hex format.
     *
     * @return the hex color string
     */
    public String getColor() {
        return color;
    }

    /**
     * Checks if this category represents a damage over time effect.
     *
     * @return true if this is a DOT category
     */
    public boolean isDamageOverTime() {
        return this == POISON || this == BLEED || this == BURN;
    }

    /**
     * Checks if this category is a hard crowd control.
     * Hard CC completely prevents actions or movement.
     *
     * @return true if this is hard CC
     */
    public boolean isHardCC() {
        return this == STUN || this == FREEZE;
    }

    /**
     * Checks if this category is a soft crowd control.
     * Soft CC partially restricts but doesn't fully incapacitate.
     *
     * @return true if this is soft CC
     */
    public boolean isSoftCC() {
        return this == ROOT || this == SILENCE;
    }

    /**
     * Checks if this category is protective/beneficial.
     *
     * @return true if this is a protective category
     */
    public boolean isProtective() {
        return this == SHIELD || this == AURA;
    }

    /**
     * Checks if this category is magical in nature.
     *
     * @return true if this is a magical category
     */
    public boolean isMagical() {
        return this == MAGIC || this == CURSE || this == FREEZE;
    }

    /**
     * Checks if this category is physical in nature.
     *
     * @return true if this is a physical category
     */
    public boolean isPhysical() {
        return this == BLEED || this == STUN || this == ROOT;
    }

    /**
     * Gets the appropriate resistance attribute for this category.
     * Returns a suggested resistance type string.
     *
     * @return the resistance type name
     */
    public String getResistanceType() {
        return switch (this) {
            case MAGIC, CURSE -> "magic_resistance";
            case POISON -> "poison_resistance";
            case BLEED -> "bleed_resistance";
            case BURN -> "fire_resistance";
            case FREEZE -> "cold_resistance";
            case STUN, ROOT, SILENCE -> "crowd_control_resistance";
            case SHIELD, AURA -> "none";
        };
    }
}
