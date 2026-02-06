package com.NextLVLHasH.Websockets.rpg.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single instance of damage in the combat system.
 * Contains all information about the damage source, target, type,
 * amounts at various calculation stages, and any effects applied.
 * 
 * <p>This class is immutable and should be created using the Builder pattern:
 * <pre>
 * Damage damage = Damage.builder()
 *     .attackerId(attackerUUID)
 *     .targetId(targetUUID)
 *     .type(DamageType.PHYSICAL)
 *     .rawAmount(100)
 *     .critical(true, 2.0)
 *     .sourceSkill("heavy_strike")
 *     .build();
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public final class Damage {

    private final UUID attackerId;
    private final UUID targetId;
    private final DamageType type;
    private final double rawAmount;
    private final double mitigatedAmount;
    private final double finalAmount;
    private final boolean isCritical;
    private final double critMultiplier;
    private final String sourceSkill;
    private final List<String> appliedEffects;
    private final long timestamp;

    /**
     * Private constructor - use Builder to create instances.
     */
    private Damage(Builder builder) {
        this.attackerId = builder.attackerId;
        this.targetId = builder.targetId;
        this.type = builder.type;
        this.rawAmount = builder.rawAmount;
        this.mitigatedAmount = builder.mitigatedAmount;
        this.finalAmount = builder.finalAmount;
        this.isCritical = builder.isCritical;
        this.critMultiplier = builder.critMultiplier;
        this.sourceSkill = builder.sourceSkill;
        this.appliedEffects = builder.appliedEffects != null 
            ? Collections.unmodifiableList(new ArrayList<>(builder.appliedEffects))
            : Collections.emptyList();
        this.timestamp = builder.timestamp > 0 ? builder.timestamp : System.currentTimeMillis();
    }

    /**
     * Gets the UUID of the attacker who dealt this damage.
     *
     * @return the attacker's UUID, or null for environmental damage
     */
    public UUID getAttackerId() {
        return attackerId;
    }

    /**
     * Gets the UUID of the target who received this damage.
     *
     * @return the target's UUID
     */
    public UUID getTargetId() {
        return targetId;
    }

    /**
     * Gets the type of damage dealt.
     *
     * @return the DamageType
     */
    public DamageType getType() {
        return type;
    }

    /**
     * Gets the raw damage amount before any mitigation.
     * This is the base damage including critical multiplier if applicable.
     *
     * @return the raw damage amount
     */
    public double getRawAmount() {
        return rawAmount;
    }

    /**
     * Gets the amount of damage that was mitigated by defenses.
     *
     * @return the mitigated damage amount
     */
    public double getMitigatedAmount() {
        return mitigatedAmount;
    }

    /**
     * Gets the final damage amount after all calculations.
     * This is the actual damage dealt to the target.
     *
     * @return the final damage amount
     */
    public double getFinalAmount() {
        return finalAmount;
    }

    /**
     * Checks if this damage instance was a critical hit.
     *
     * @return true if this was a critical hit
     */
    public boolean isCritical() {
        return isCritical;
    }

    /**
     * Gets the critical hit multiplier applied to this damage.
     *
     * @return the crit multiplier (1.0 if not critical)
     */
    public double getCritMultiplier() {
        return critMultiplier;
    }

    /**
     * Gets the skill ID that caused this damage, if any.
     *
     * @return the skill ID, or null for basic attacks
     */
    public String getSourceSkill() {
        return sourceSkill;
    }

    /**
     * Gets the list of effect IDs that were applied with this damage.
     *
     * @return unmodifiable list of effect IDs
     */
    public List<String> getAppliedEffects() {
        return appliedEffects;
    }

    /**
     * Gets the timestamp when this damage was dealt.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Calculates the percentage of damage that was mitigated.
     *
     * @return the mitigation percentage (0.0 to 1.0), or 0 if no raw damage
     */
    public double getMitigationPercentage() {
        if (rawAmount <= 0) {
            return 0.0;
        }
        return mitigatedAmount / rawAmount;
    }

    /**
     * Calculates the percentage of damage that got through defenses.
     *
     * @return the penetration percentage (0.0 to 1.0)
     */
    public double getPenetrationPercentage() {
        return 1.0 - getMitigationPercentage();
    }

    /**
     * Checks if this damage was from an environmental source.
     *
     * @return true if there is no attacker
     */
    public boolean isEnvironmental() {
        return attackerId == null;
    }

    /**
     * Checks if any effects were applied with this damage.
     *
     * @return true if effects were applied
     */
    public boolean hasAppliedEffects() {
        return !appliedEffects.isEmpty();
    }

    /**
     * Creates a new Builder for constructing Damage instances.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder pre-populated with values from this Damage.
     *
     * @return a new Builder with this damage's values
     */
    public Builder toBuilder() {
        return new Builder()
            .attackerId(attackerId)
            .targetId(targetId)
            .type(type)
            .rawAmount(rawAmount)
            .mitigatedAmount(mitigatedAmount)
            .finalAmount(finalAmount)
            .critical(isCritical, critMultiplier)
            .sourceSkill(sourceSkill)
            .appliedEffects(appliedEffects)
            .timestamp(timestamp);
    }

    @Override
    public String toString() {
        return "Damage{" +
            "attackerId=" + attackerId +
            ", targetId=" + targetId +
            ", type=" + type +
            ", rawAmount=" + rawAmount +
            ", mitigatedAmount=" + mitigatedAmount +
            ", finalAmount=" + finalAmount +
            ", isCritical=" + isCritical +
            ", critMultiplier=" + critMultiplier +
            ", sourceSkill='" + sourceSkill + '\'' +
            ", appliedEffects=" + appliedEffects +
            ", timestamp=" + timestamp +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Damage damage = (Damage) o;
        return Double.compare(damage.rawAmount, rawAmount) == 0 &&
            Double.compare(damage.mitigatedAmount, mitigatedAmount) == 0 &&
            Double.compare(damage.finalAmount, finalAmount) == 0 &&
            isCritical == damage.isCritical &&
            Double.compare(damage.critMultiplier, critMultiplier) == 0 &&
            timestamp == damage.timestamp &&
            Objects.equals(attackerId, damage.attackerId) &&
            Objects.equals(targetId, damage.targetId) &&
            type == damage.type &&
            Objects.equals(sourceSkill, damage.sourceSkill) &&
            Objects.equals(appliedEffects, damage.appliedEffects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackerId, targetId, type, rawAmount, mitigatedAmount, 
            finalAmount, isCritical, critMultiplier, sourceSkill, appliedEffects, timestamp);
    }

    /**
     * Builder class for constructing Damage instances.
     */
    public static final class Builder {
        private UUID attackerId;
        private UUID targetId;
        private DamageType type = DamageType.PHYSICAL;
        private double rawAmount;
        private double mitigatedAmount;
        private double finalAmount;
        private boolean isCritical;
        private double critMultiplier = 1.0;
        private String sourceSkill;
        private List<String> appliedEffects;
        private long timestamp;

        /**
         * Creates a new Builder with default values.
         */
        public Builder() {
            this.appliedEffects = new ArrayList<>();
        }

        /**
         * Sets the attacker's UUID.
         *
         * @param attackerId the attacker's UUID (null for environmental damage)
         * @return this builder
         */
        public Builder attackerId(UUID attackerId) {
            this.attackerId = attackerId;
            return this;
        }

        /**
         * Sets the target's UUID.
         *
         * @param targetId the target's UUID
         * @return this builder
         */
        public Builder targetId(UUID targetId) {
            this.targetId = targetId;
            return this;
        }

        /**
         * Sets the damage type.
         *
         * @param type the damage type
         * @return this builder
         */
        public Builder type(DamageType type) {
            this.type = Objects.requireNonNull(type, "Damage type cannot be null");
            return this;
        }

        /**
         * Sets the raw damage amount before mitigation.
         *
         * @param rawAmount the raw damage amount
         * @return this builder
         */
        public Builder rawAmount(double rawAmount) {
            this.rawAmount = Math.max(0, rawAmount);
            return this;
        }

        /**
         * Sets the mitigated damage amount.
         *
         * @param mitigatedAmount the amount of damage mitigated
         * @return this builder
         */
        public Builder mitigatedAmount(double mitigatedAmount) {
            this.mitigatedAmount = Math.max(0, mitigatedAmount);
            return this;
        }

        /**
         * Sets the final damage amount after calculations.
         *
         * @param finalAmount the final damage dealt
         * @return this builder
         */
        public Builder finalAmount(double finalAmount) {
            this.finalAmount = Math.max(0, finalAmount);
            return this;
        }

        /**
         * Sets the critical hit status and multiplier.
         *
         * @param isCritical whether this was a critical hit
         * @param multiplier the critical multiplier (ignored if not critical)
         * @return this builder
         */
        public Builder critical(boolean isCritical, double multiplier) {
            this.isCritical = isCritical;
            this.critMultiplier = isCritical ? Math.max(1.0, multiplier) : 1.0;
            return this;
        }

        /**
         * Sets only the critical flag with default multiplier.
         *
         * @param isCritical whether this was a critical hit
         * @return this builder
         */
        public Builder critical(boolean isCritical) {
            return critical(isCritical, this.critMultiplier);
        }

        /**
         * Sets only the critical multiplier.
         *
         * @param multiplier the critical multiplier
         * @return this builder
         */
        public Builder critMultiplier(double multiplier) {
            this.critMultiplier = Math.max(1.0, multiplier);
            return this;
        }

        /**
         * Sets the source skill ID.
         *
         * @param skillId the skill that caused this damage
         * @return this builder
         */
        public Builder sourceSkill(String skillId) {
            this.sourceSkill = skillId;
            return this;
        }

        /**
         * Adds an applied effect ID.
         *
         * @param effectId the effect ID to add
         * @return this builder
         */
        public Builder addAppliedEffect(String effectId) {
            if (effectId != null && !effectId.isEmpty()) {
                this.appliedEffects.add(effectId);
            }
            return this;
        }

        /**
         * Sets all applied effect IDs.
         *
         * @param effectIds the list of effect IDs
         * @return this builder
         */
        public Builder appliedEffects(List<String> effectIds) {
            this.appliedEffects = effectIds != null 
                ? new ArrayList<>(effectIds) 
                : new ArrayList<>();
            return this;
        }

        /**
         * Sets the timestamp for this damage.
         *
         * @param timestamp the timestamp in milliseconds
         * @return this builder
         */
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the Damage instance.
         *
         * @return the constructed Damage
         * @throws IllegalStateException if required fields are missing
         */
        public Damage build() {
            if (type == null) {
                throw new IllegalStateException("Damage type must be specified");
            }
            // Auto-calculate final amount if not set
            if (finalAmount == 0 && rawAmount > 0) {
                finalAmount = rawAmount - mitigatedAmount;
            }
            return new Damage(this);
        }
    }
}
