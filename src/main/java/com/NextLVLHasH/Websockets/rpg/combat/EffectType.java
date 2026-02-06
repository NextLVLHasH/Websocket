package com.NextLVLHasH.Websockets.rpg.combat;

/**
 * Enumeration of effect types in the combat system.
 * Categorizes effects by their fundamental behavior and purpose.
 * 
 * <p>Effect types determine how an effect interacts with the target:
 * <ul>
 *     <li>BUFF - Positive temporary enhancements</li>
 *     <li>DEBUFF - Negative temporary impairments</li>
 *     <li>DOT - Damage dealt periodically over time</li>
 *     <li>HOT - Healing applied periodically over time</li>
 *     <li>CROWD_CONTROL - Movement or ability restrictions</li>
 * </ul>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum EffectType {
    
    /**
     * Positive effect that enhances the target's capabilities.
     * Examples: attack speed boost, damage increase, defense buff.
     */
    BUFF(true, true),
    
    /**
     * Negative effect that impairs the target's capabilities.
     * Examples: attack speed slow, weakness, armor reduction.
     */
    DEBUFF(false, true),
    
    /**
     * Damage over time effect that deals periodic damage.
     * Examples: poison, burning, bleeding.
     */
    DOT(false, true),
    
    /**
     * Healing over time effect that restores health periodically.
     * Examples: regeneration, healing ward, life steal aura.
     */
    HOT(true, true),
    
    /**
     * Crowd control effect that restricts movement or abilities.
     * Examples: stun, root, silence, fear.
     */
    CROWD_CONTROL(false, true);

    private final boolean positive;
    private final boolean timed;

    /**
     * Constructs an EffectType with the specified properties.
     *
     * @param positive whether this effect type is beneficial
     * @param timed whether this effect type has a duration
     */
    EffectType(boolean positive, boolean timed) {
        this.positive = positive;
        this.timed = timed;
    }

    /**
     * Checks if this effect type is positive/beneficial.
     * Positive effects help the target, while negative effects hinder them.
     *
     * @return true if this is a beneficial effect type
     */
    public boolean isPositive() {
        return positive;
    }

    /**
     * Checks if this effect type has a duration.
     * Timed effects expire after a set duration, while permanent effects persist.
     *
     * @return true if this effect type has a duration
     */
    public boolean isTimed() {
        return timed;
    }

    /**
     * Checks if this effect type deals damage over time.
     *
     * @return true if this is a DOT effect
     */
    public boolean isDamageOverTime() {
        return this == DOT;
    }

    /**
     * Checks if this effect type heals over time.
     *
     * @return true if this is a HOT effect
     */
    public boolean isHealOverTime() {
        return this == HOT;
    }

    /**
     * Checks if this effect type is crowd control.
     *
     * @return true if this is a crowd control effect
     */
    public boolean isCrowdControl() {
        return this == CROWD_CONTROL;
    }

    /**
     * Checks if this effect type modifies attributes.
     *
     * @return true if this effect can modify attributes
     */
    public boolean canModifyAttributes() {
        return this == BUFF || this == DEBUFF;
    }
}
