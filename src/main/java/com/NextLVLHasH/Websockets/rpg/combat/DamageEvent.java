package com.NextLVLHasH.Websockets.rpg.combat;

import java.util.Objects;

/**
 * Event object representing a damage event in the combat system.
 * Can be used by event listeners to modify or cancel damage before it's applied.
 * 
 * <p>This class allows event handlers to:
 * <ul>
 *     <li>Cancel the damage entirely</li>
 *     <li>Modify the damage amount</li>
 *     <li>Inspect the original damage details</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * DamageEvent event = new DamageEvent(damage);
 * 
 * // Event handler can modify damage
 * if (targetHasShield) {
 *     event.modifyDamage(event.getModifiedAmount() * 0.5); // 50% reduction
 * }
 * 
 * // Or cancel entirely
 * if (targetIsInvulnerable) {
 *     event.cancel();
 * }
 * 
 * // Apply damage only if not cancelled
 * if (!event.isCancelled()) {
 *     applyDamage(target, event.getModifiedAmount());
 * }
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public final class DamageEvent {

    private final Damage damage;
    private boolean cancelled;
    private double modifiedAmount;

    /**
     * Creates a new DamageEvent wrapping the given damage.
     *
     * @param damage the Damage instance to wrap
     * @throws NullPointerException if damage is null
     */
    public DamageEvent(Damage damage) {
        this.damage = Objects.requireNonNull(damage, "Damage cannot be null");
        this.cancelled = false;
        this.modifiedAmount = damage.getFinalAmount();
    }

    /**
     * Gets the underlying Damage object.
     *
     * @return the Damage instance
     */
    public Damage getDamage() {
        return damage;
    }

    /**
     * Checks if this event has been cancelled.
     *
     * @return true if the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels this damage event.
     * Cancelled events should not apply any damage.
     */
    public void cancel() {
        this.cancelled = true;
        this.modifiedAmount = 0;
    }

    /**
     * Sets the cancelled state of this event.
     *
     * @param cancelled whether to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if (cancelled) {
            this.modifiedAmount = 0;
        } else {
            // Restore to final amount if un-cancelling
            this.modifiedAmount = damage.getFinalAmount();
        }
    }

    /**
     * Gets the current modified damage amount.
     * This may differ from the original final damage if modified by handlers.
     *
     * @return the modified damage amount
     */
    public double getModifiedAmount() {
        return modifiedAmount;
    }

    /**
     * Modifies the damage amount.
     * The new amount is clamped to be non-negative.
     *
     * @param amount the new damage amount
     */
    public void modifyDamage(double amount) {
        this.modifiedAmount = Math.max(0, amount);
        // If damage is modified to 0, effectively cancel
        if (this.modifiedAmount == 0) {
            this.cancelled = true;
        }
    }

    /**
     * Sets the modified damage amount directly.
     * The new amount is clamped to be non-negative.
     *
     * @param damage the new damage amount
     */
    public void setModifiedDamage(double damage) {
        this.modifiedAmount = Math.max(0, damage);
    }

    /**
     * Adds a flat amount to the current damage.
     *
     * @param amount the amount to add (can be negative to reduce)
     */
    public void addDamage(double amount) {
        modifyDamage(this.modifiedAmount + amount);
    }

    /**
     * Multiplies the current damage by a modifier.
     *
     * @param multiplier the multiplier to apply
     */
    public void multiplyDamage(double multiplier) {
        modifyDamage(this.modifiedAmount * multiplier);
    }

    /**
     * Checks if the damage has been modified from its original value.
     *
     * @return true if the modified amount differs from the original final amount
     */
    public boolean isModified() {
        return Double.compare(modifiedAmount, damage.getFinalAmount()) != 0;
    }

    /**
     * Gets the difference between modified and original damage.
     *
     * @return the difference (positive if increased, negative if decreased)
     */
    public double getModificationDelta() {
        return modifiedAmount - damage.getFinalAmount();
    }

    /**
     * Resets the modified amount to the original final damage.
     * Also un-cancels the event if it was cancelled.
     */
    public void reset() {
        this.cancelled = false;
        this.modifiedAmount = damage.getFinalAmount();
    }

    /**
     * Gets the original raw damage amount.
     *
     * @return the raw damage before any mitigation
     */
    public double getRawAmount() {
        return damage.getRawAmount();
    }

    /**
     * Gets the original final damage amount.
     *
     * @return the final damage after calculation
     */
    public double getOriginalFinalAmount() {
        return damage.getFinalAmount();
    }

    /**
     * Gets the damage type.
     *
     * @return the DamageType
     */
    public DamageType getDamageType() {
        return damage.getType();
    }

    /**
     * Checks if this was a critical hit.
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return damage.isCritical();
    }

    /**
     * Gets the attacker's UUID.
     *
     * @return the attacker UUID, or null for environmental damage
     */
    public java.util.UUID getAttackerId() {
        return damage.getAttackerId();
    }

    /**
     * Gets the target's UUID.
     *
     * @return the target UUID
     */
    public java.util.UUID getTargetId() {
        return damage.getTargetId();
    }

    /**
     * Creates a new Damage object reflecting the modified state of this event.
     * Useful when the modified damage needs to be stored or further processed.
     *
     * @return a new Damage with the modified amount
     */
    public Damage toModifiedDamage() {
        if (!isModified()) {
            return damage;
        }
        
        double originalFinal = damage.getFinalAmount();
        double modRatio = originalFinal > 0 ? modifiedAmount / originalFinal : 0;
        
        return damage.toBuilder()
            .mitigatedAmount(damage.getMitigatedAmount() * modRatio)
            .finalAmount(modifiedAmount)
            .build();
    }

    @Override
    public String toString() {
        return "DamageEvent{" +
            "damage=" + damage +
            ", cancelled=" + cancelled +
            ", modifiedAmount=" + modifiedAmount +
            ", modified=" + isModified() +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DamageEvent that = (DamageEvent) o;
        return cancelled == that.cancelled &&
            Double.compare(that.modifiedAmount, modifiedAmount) == 0 &&
            damage.equals(that.damage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(damage, cancelled, modifiedAmount);
    }
}
