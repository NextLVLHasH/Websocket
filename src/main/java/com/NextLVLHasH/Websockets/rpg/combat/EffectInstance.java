package com.NextLVLHasH.Websockets.rpg.combat;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an active effect instance applied to a specific entity.
 * Tracks the runtime state of an effect including duration, stacks, and tick timing.
 * 
 * <p>This class maintains:
 * <ul>
 *     <li>Reference to the effect definition ({@link CombatEffect})</li>
 *     <li>Source and target entity identifiers</li>
 *     <li>Current stack count</li>
 *     <li>Timing information (applied, expires, last tick)</li>
 * </ul>
 * 
 * <p>The {@link #tick()} method should be called periodically by the combat manager
 * to process DOT/HOT effects and check expiration.
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public final class EffectInstance {

    private final String effectId;
    private final CombatEffect effect;
    private final UUID targetId;
    private final UUID sourceId;
    private final long appliedAt;
    
    private int stacks;
    private long expiresAt;
    private long lastTickAt;

    /**
     * Creates a new effect instance.
     *
     * @param effect the effect definition
     * @param targetId the UUID of the target entity
     * @param sourceId the UUID of the source entity (who applied the effect)
     * @param durationMillis the duration override in milliseconds (use effect default if <= 0)
     */
    public EffectInstance(CombatEffect effect, UUID targetId, UUID sourceId, int durationMillis) {
        this.effect = Objects.requireNonNull(effect, "Effect cannot be null");
        this.effectId = effect.getId();
        this.targetId = Objects.requireNonNull(targetId, "Target ID cannot be null");
        this.sourceId = sourceId; // Source can be null for environmental effects
        this.appliedAt = System.currentTimeMillis();
        this.stacks = 1;
        
        int duration = durationMillis > 0 ? durationMillis : effect.getDurationMillis();
        this.expiresAt = this.appliedAt + duration;
        this.lastTickAt = this.appliedAt;
    }

    /**
     * Creates a new effect instance with the effect's default duration.
     *
     * @param effect the effect definition
     * @param targetId the UUID of the target entity
     * @param sourceId the UUID of the source entity
     */
    public EffectInstance(CombatEffect effect, UUID targetId, UUID sourceId) {
        this(effect, targetId, sourceId, -1);
    }

    // ========== Getters ==========

    /**
     * Gets the effect ID.
     *
     * @return the effect identifier
     */
    public String getEffectId() {
        return effectId;
    }

    /**
     * Gets the effect definition.
     *
     * @return the CombatEffect
     */
    public CombatEffect getEffect() {
        return effect;
    }

    /**
     * Gets the target entity UUID.
     *
     * @return the target's UUID
     */
    public UUID getTargetId() {
        return targetId;
    }

    /**
     * Gets the source entity UUID.
     *
     * @return the source's UUID, or null for environmental effects
     */
    public UUID getSourceId() {
        return sourceId;
    }

    /**
     * Gets the current number of stacks.
     *
     * @return the stack count
     */
    public int getStacks() {
        return stacks;
    }

    /**
     * Gets the timestamp when this effect was applied.
     *
     * @return the applied timestamp in milliseconds
     */
    public long getAppliedAt() {
        return appliedAt;
    }

    /**
     * Gets the timestamp when this effect will expire.
     *
     * @return the expiration timestamp in milliseconds
     */
    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * Gets the timestamp of the last tick.
     *
     * @return the last tick timestamp in milliseconds
     */
    public long getLastTickAt() {
        return lastTickAt;
    }

    // ========== State Methods ==========

    /**
     * Checks if this effect has expired.
     *
     * @return true if the current time is past the expiration time
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    /**
     * Gets the remaining duration in milliseconds.
     *
     * @return the remaining duration, or 0 if expired
     */
    public long getRemainingDuration() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets the elapsed time since the effect was applied.
     *
     * @return the elapsed time in milliseconds
     */
    public long getElapsedDuration() {
        return System.currentTimeMillis() - appliedAt;
    }

    /**
     * Gets the total duration of this effect instance.
     *
     * @return the total duration in milliseconds
     */
    public long getTotalDuration() {
        return expiresAt - appliedAt;
    }

    /**
     * Gets the progress of this effect as a value from 0.0 to 1.0.
     * 0.0 = just applied, 1.0 = expired.
     *
     * @return the progress ratio
     */
    public double getProgress() {
        long total = getTotalDuration();
        if (total <= 0) return 1.0;
        
        long elapsed = getElapsedDuration();
        return Math.min(1.0, Math.max(0.0, (double) elapsed / total));
    }

    /**
     * Gets the inverse progress (time remaining ratio).
     * 1.0 = just applied, 0.0 = expired.
     *
     * @return the remaining progress ratio
     */
    public double getRemainingProgress() {
        return 1.0 - getProgress();
    }

    /**
     * Checks if enough time has passed for another tick.
     *
     * @return true if ready for the next tick
     */
    public boolean isReadyForTick() {
        return System.currentTimeMillis() - lastTickAt >= effect.getTickIntervalMillis();
    }

    // ========== Modification Methods ==========

    /**
     * Adds a stack to this effect, up to the maximum.
     * Also refreshes the duration.
     *
     * @return the new stack count
     */
    public int addStack() {
        if (effect.isStackable() && stacks < effect.getMaxStacks()) {
            stacks++;
        }
        // Refresh duration on stack application
        refreshDuration();
        return stacks;
    }

    /**
     * Adds multiple stacks to this effect.
     *
     * @param count the number of stacks to add
     * @return the new stack count
     */
    public int addStacks(int count) {
        if (effect.isStackable()) {
            stacks = Math.min(effect.getMaxStacks(), stacks + count);
        }
        refreshDuration();
        return stacks;
    }

    /**
     * Removes a stack from this effect.
     *
     * @return the new stack count
     */
    public int removeStack() {
        if (stacks > 0) {
            stacks--;
        }
        return stacks;
    }

    /**
     * Sets the stack count directly.
     *
     * @param stacks the new stack count
     */
    public void setStacks(int stacks) {
        this.stacks = Math.max(0, Math.min(effect.getMaxStacks(), stacks));
    }

    /**
     * Refreshes the duration to the full amount from the effect definition.
     */
    public void refreshDuration() {
        this.expiresAt = System.currentTimeMillis() + effect.getDurationMillis();
    }

    /**
     * Extends the duration by the given amount.
     *
     * @param millis the time to add in milliseconds
     */
    public void extendDuration(int millis) {
        this.expiresAt += millis;
    }

    /**
     * Reduces the duration by the given amount.
     *
     * @param millis the time to remove in milliseconds
     */
    public void reduceDuration(int millis) {
        this.expiresAt = Math.max(System.currentTimeMillis(), expiresAt - millis);
    }

    /**
     * Forces the effect to expire immediately.
     */
    public void expire() {
        this.expiresAt = System.currentTimeMillis();
    }

    // ========== Tick Processing ==========

    /**
     * Processes a tick of this effect.
     * Call this method periodically to process DOT/HOT effects.
     *
     * @return a TickResult containing damage/healing dealt, or null if not ready to tick
     */
    public TickResult tick() {
        long now = System.currentTimeMillis();
        
        // Check if it's time for a tick
        if (now - lastTickAt < effect.getTickIntervalMillis()) {
            return null;
        }
        
        // Check if expired
        if (isExpired()) {
            return null;
        }
        
        lastTickAt = now;
        
        // Calculate tick values based on stacks
        double damage = effect.getDamagePerTick() * stacks;
        double healing = effect.getHealingPerTick() * stacks;
        
        return new TickResult(damage, healing, effect.getDamageType());
    }

    /**
     * Forces a tick regardless of timing.
     * Useful for immediate effect application.
     *
     * @return a TickResult containing damage/healing dealt
     */
    public TickResult forceTick() {
        lastTickAt = System.currentTimeMillis();
        
        double damage = effect.getDamagePerTick() * stacks;
        double healing = effect.getHealingPerTick() * stacks;
        
        return new TickResult(damage, healing, effect.getDamageType());
    }

    // ========== Utility Methods ==========

    /**
     * Checks if this effect is from the same source.
     *
     * @param sourceId the source UUID to compare
     * @return true if from the same source
     */
    public boolean isFromSource(UUID sourceId) {
        if (this.sourceId == null) return sourceId == null;
        return this.sourceId.equals(sourceId);
    }

    /**
     * Checks if this effect should be removed on death.
     *
     * @return true if should be removed on death
     */
    public boolean shouldRemoveOnDeath() {
        return effect.isRemoveOnDeath();
    }

    /**
     * Checks if this effect can be dispelled.
     *
     * @return true if can be dispelled
     */
    public boolean canDispel() {
        return effect.canDispel();
    }

    /**
     * Gets a formatted duration string (e.g., "5.2s").
     *
     * @return the formatted remaining duration
     */
    public String getFormattedDuration() {
        long remaining = getRemainingDuration();
        if (remaining >= 60000) {
            return String.format("%.1fm", remaining / 60000.0);
        } else {
            return String.format("%.1fs", remaining / 1000.0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EffectInstance that = (EffectInstance) o;
        return Objects.equals(effectId, that.effectId) &&
               Objects.equals(targetId, that.targetId) &&
               Objects.equals(sourceId, that.sourceId) &&
               appliedAt == that.appliedAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectId, targetId, sourceId, appliedAt);
    }

    @Override
    public String toString() {
        return "EffectInstance{" +
                "effectId='" + effectId + '\'' +
                ", target=" + targetId +
                ", source=" + sourceId +
                ", stacks=" + stacks +
                ", remaining=" + getFormattedDuration() +
                '}';
    }

    /**
     * Result of processing a tick, containing damage and healing values.
     */
    public static final class TickResult {
        private final double damage;
        private final double healing;
        private final DamageType damageType;

        /**
         * Creates a new tick result.
         *
         * @param damage the damage dealt
         * @param healing the healing applied
         * @param damageType the type of damage dealt
         */
        public TickResult(double damage, double healing, DamageType damageType) {
            this.damage = damage;
            this.healing = healing;
            this.damageType = damageType;
        }

        /**
         * Gets the damage dealt this tick.
         *
         * @return the damage amount
         */
        public double getDamage() {
            return damage;
        }

        /**
         * Gets the healing applied this tick.
         *
         * @return the healing amount
         */
        public double getHealing() {
            return healing;
        }

        /**
         * Gets the damage type for this tick's damage.
         *
         * @return the DamageType, or null if no damage
         */
        public DamageType getDamageType() {
            return damageType;
        }

        /**
         * Checks if this tick dealt damage.
         *
         * @return true if damage > 0
         */
        public boolean hasDamage() {
            return damage > 0;
        }

        /**
         * Checks if this tick applied healing.
         *
         * @return true if healing > 0
         */
        public boolean hasHealing() {
            return healing > 0;
        }

        @Override
        public String toString() {
            return "TickResult{damage=" + damage + ", healing=" + healing + ", type=" + damageType + '}';
        }
    }
}
