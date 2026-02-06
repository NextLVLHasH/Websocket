package com.NextLVLHasH.Websockets.rpg.progression;

import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.util.*;

/**
 * Represents the stat changes and rewards that occur when a player levels up.
 * <p>
 * A LevelUpModifier contains all the increases that should be applied to a
 * player's stats when they gain a level. This includes attribute increases,
 * resource pool increases, regeneration rate increases, and skill point awards.
 * </p>
 * <p>
 * Modifiers can be created manually or generated from a CharacterClass to
 * match the class's defined progression.
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class LevelUpModifier {

    /**
     * Attribute increases to apply on level-up.
     * Maps AttributeType to the amount to increase.
     */
    private final Map<AttributeType, Double> attributeIncreases;

    /**
     * Health pool increase on level-up.
     */
    private final double healthIncrease;

    /**
     * Mana pool increase on level-up.
     */
    private final double manaIncrease;

    /**
     * Stamina pool increase on level-up.
     */
    private final double staminaIncrease;

    /**
     * Health regeneration rate increase on level-up.
     */
    private final double healthRegenIncrease;

    /**
     * Mana regeneration rate increase on level-up.
     */
    private final double manaRegenIncrease;

    /**
     * Stamina regeneration rate increase on level-up.
     */
    private final double staminaRegenIncrease;

    /**
     * Number of skill points awarded on level-up.
     */
    private final int skillPointsAwarded;

    /**
     * Creates a new LevelUpModifier with all specified values.
     *
     * @param attributeIncreases map of attribute increases
     * @param healthIncrease health pool increase
     * @param manaIncrease mana pool increase
     * @param staminaIncrease stamina pool increase
     * @param healthRegenIncrease health regen increase
     * @param manaRegenIncrease mana regen increase
     * @param staminaRegenIncrease stamina regen increase
     * @param skillPointsAwarded skill points to award
     */
    public LevelUpModifier(
            Map<AttributeType, Double> attributeIncreases,
            double healthIncrease,
            double manaIncrease,
            double staminaIncrease,
            double healthRegenIncrease,
            double manaRegenIncrease,
            double staminaRegenIncrease,
            int skillPointsAwarded) {

        this.attributeIncreases = attributeIncreases != null
                ? new EnumMap<>(attributeIncreases)
                : new EnumMap<>(AttributeType.class);
        this.healthIncrease = Math.max(0, healthIncrease);
        this.manaIncrease = Math.max(0, manaIncrease);
        this.staminaIncrease = Math.max(0, staminaIncrease);
        this.healthRegenIncrease = Math.max(0, healthRegenIncrease);
        this.manaRegenIncrease = Math.max(0, manaRegenIncrease);
        this.staminaRegenIncrease = Math.max(0, staminaRegenIncrease);
        this.skillPointsAwarded = Math.max(0, skillPointsAwarded);
    }

    /**
     * Creates a LevelUpModifier from a CharacterClass's growth rates.
     * <p>
     * This factory method extracts the per-level increases from a CharacterClass
     * to create a modifier that matches the class's progression.
     * </p>
     *
     * @param cls the CharacterClass to extract modifiers from
     * @return a new LevelUpModifier based on the class's growth rates
     * @throws NullPointerException if cls is null
     */
    public static LevelUpModifier fromCharacterClass(CharacterClass cls) {
        Objects.requireNonNull(cls, "CharacterClass cannot be null");

        // Extract attribute growth rates
        Map<AttributeType, Double> attributeIncreases = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            double growthRate = cls.getAttributeGrowthRate(type);
            if (growthRate > 0) {
                attributeIncreases.put(type, growthRate);
            }
        }

        return new LevelUpModifier(
                attributeIncreases,
                cls.getHealthPerLevel(),
                cls.getManaPerLevel(),
                cls.getStaminaPerLevel(),
                0.1,  // Default health regen increase per level
                0.1,  // Default mana regen increase per level
                0.2,  // Default stamina regen increase per level
                1     // Default 1 skill point per level
        );
    }

    /**
     * Creates a LevelUpModifier from a CharacterClass with custom skill point award.
     *
     * @param cls the CharacterClass to extract modifiers from
     * @param skillPointsPerLevel skill points to award per level
     * @return a new LevelUpModifier based on the class's growth rates
     */
    public static LevelUpModifier fromCharacterClass(CharacterClass cls, int skillPointsPerLevel) {
        Objects.requireNonNull(cls, "CharacterClass cannot be null");

        Map<AttributeType, Double> attributeIncreases = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            double growthRate = cls.getAttributeGrowthRate(type);
            if (growthRate > 0) {
                attributeIncreases.put(type, growthRate);
            }
        }

        return new LevelUpModifier(
                attributeIncreases,
                cls.getHealthPerLevel(),
                cls.getManaPerLevel(),
                cls.getStaminaPerLevel(),
                0.1,
                0.1,
                0.2,
                skillPointsPerLevel
        );
    }

    /**
     * Applies this modifier to a player's RPG data.
     * <p>
     * This method increases all stats according to the modifier values.
     * Attributes are increased by the specified amounts (rounded down),
     * and current resources are increased along with maximums to maintain
     * the same ratio of current/max.
     * </p>
     *
     * @param data the PlayerRPGData to modify
     * @throws NullPointerException if data is null
     */
    public void apply(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        // Apply attribute increases
        for (Map.Entry<AttributeType, Double> entry : attributeIncreases.entrySet()) {
            AttributeType type = entry.getKey();
            int increase = (int) Math.floor(entry.getValue());
            if (increase > 0) {
                int currentBase = data.getBaseAttribute(type);
                data.setBaseAttribute(type, currentBase + increase);
                
                // Also update current attribute
                int currentCurrent = data.getCurrentAttribute(type);
                data.setCurrentAttribute(type, currentCurrent + increase);
            }
        }

        // Apply health increase (maintain current/max ratio)
        if (healthIncrease > 0) {
            double oldMax = data.getMaxHealth();
            double oldCurrent = data.getCurrentHealth();
            double ratio = oldMax > 0 ? oldCurrent / oldMax : 1.0;
            
            double newMax = oldMax + healthIncrease;
            data.setMaxHealth(newMax);
            data.setCurrentHealth(newMax * ratio);
        }

        // Apply mana increase (maintain current/max ratio)
        if (manaIncrease > 0) {
            double oldMax = data.getMaxMana();
            double oldCurrent = data.getCurrentMana();
            double ratio = oldMax > 0 ? oldCurrent / oldMax : 1.0;
            
            double newMax = oldMax + manaIncrease;
            data.setMaxMana(newMax);
            data.setCurrentMana(newMax * ratio);
        }

        // Apply stamina increase (maintain current/max ratio)
        if (staminaIncrease > 0) {
            double oldMax = data.getMaxStamina();
            double oldCurrent = data.getCurrentStamina();
            double ratio = oldMax > 0 ? oldCurrent / oldMax : 1.0;
            
            double newMax = oldMax + staminaIncrease;
            data.setMaxStamina(newMax);
            data.setCurrentStamina(newMax * ratio);
        }

        // Apply regeneration increases
        if (healthRegenIncrease > 0) {
            data.setHealthRegen(data.getHealthRegen() + healthRegenIncrease);
        }
        if (manaRegenIncrease > 0) {
            data.setManaRegen(data.getManaRegen() + manaRegenIncrease);
        }
        if (staminaRegenIncrease > 0) {
            data.setStaminaRegen(data.getStaminaRegen() + staminaRegenIncrease);
        }

        // Award skill points
        if (skillPointsAwarded > 0) {
            data.setSkillPoints(data.getSkillPoints() + skillPointsAwarded);
        }
    }

    // ==================== Getters ====================

    /**
     * Gets the attribute increases map (unmodifiable view).
     *
     * @return unmodifiable map of attribute increases
     */
    public Map<AttributeType, Double> getAttributeIncreases() {
        return Collections.unmodifiableMap(attributeIncreases);
    }

    /**
     * Gets the increase for a specific attribute.
     *
     * @param type the attribute type
     * @return the increase amount, or 0 if not set
     */
    public double getAttributeIncrease(AttributeType type) {
        return attributeIncreases.getOrDefault(type, 0.0);
    }

    /**
     * Gets the health pool increase.
     *
     * @return the health increase
     */
    public double getHealthIncrease() {
        return healthIncrease;
    }

    /**
     * Gets the mana pool increase.
     *
     * @return the mana increase
     */
    public double getManaIncrease() {
        return manaIncrease;
    }

    /**
     * Gets the stamina pool increase.
     *
     * @return the stamina increase
     */
    public double getStaminaIncrease() {
        return staminaIncrease;
    }

    /**
     * Gets the health regeneration increase.
     *
     * @return the health regen increase
     */
    public double getHealthRegenIncrease() {
        return healthRegenIncrease;
    }

    /**
     * Gets the mana regeneration increase.
     *
     * @return the mana regen increase
     */
    public double getManaRegenIncrease() {
        return manaRegenIncrease;
    }

    /**
     * Gets the stamina regeneration increase.
     *
     * @return the stamina regen increase
     */
    public double getStaminaRegenIncrease() {
        return staminaRegenIncrease;
    }

    /**
     * Gets the skill points awarded.
     *
     * @return the skill points
     */
    public int getSkillPointsAwarded() {
        return skillPointsAwarded;
    }

    // ==================== Per-Level Aliases ====================

    /**
     * Alias for getHealthIncrease() - gets health increase per level.
     *
     * @return the health increase per level
     */
    public double healthPerLevel() {
        return healthIncrease;
    }

    /**
     * Alias for getManaIncrease() - gets mana increase per level.
     *
     * @return the mana increase per level
     */
    public double manaPerLevel() {
        return manaIncrease;
    }

    /**
     * Alias for getStaminaIncrease() - gets stamina increase per level.
     *
     * @return the stamina increase per level
     */
    public double staminaPerLevel() {
        return staminaIncrease;
    }

    /**
     * Alias for getSkillPointsAwarded() - gets skill points per level.
     *
     * @return the skill points per level
     */
    public int skillPointsPerLevel() {
        return skillPointsAwarded;
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for constructing LevelUpModifier instances.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing LevelUpModifier instances.
     */
    public static class Builder {
        private final Map<AttributeType, Double> attributeIncreases = new EnumMap<>(AttributeType.class);
        private double healthIncrease = 0;
        private double manaIncrease = 0;
        private double staminaIncrease = 0;
        private double healthRegenIncrease = 0;
        private double manaRegenIncrease = 0;
        private double staminaRegenIncrease = 0;
        private int skillPointsAwarded = 1;

        private Builder() {}

        /**
         * Sets an attribute increase.
         *
         * @param type the attribute type
         * @param increase the increase amount
         * @return this builder
         */
        public Builder attributeIncrease(AttributeType type, double increase) {
            attributeIncreases.put(type, increase);
            return this;
        }

        /**
         * Sets the health increase.
         *
         * @param healthIncrease the health increase
         * @return this builder
         */
        public Builder healthIncrease(double healthIncrease) {
            this.healthIncrease = healthIncrease;
            return this;
        }

        /**
         * Sets the mana increase.
         *
         * @param manaIncrease the mana increase
         * @return this builder
         */
        public Builder manaIncrease(double manaIncrease) {
            this.manaIncrease = manaIncrease;
            return this;
        }

        /**
         * Sets the stamina increase.
         *
         * @param staminaIncrease the stamina increase
         * @return this builder
         */
        public Builder staminaIncrease(double staminaIncrease) {
            this.staminaIncrease = staminaIncrease;
            return this;
        }

        /**
         * Sets the health regeneration increase.
         *
         * @param healthRegenIncrease the health regen increase
         * @return this builder
         */
        public Builder healthRegenIncrease(double healthRegenIncrease) {
            this.healthRegenIncrease = healthRegenIncrease;
            return this;
        }

        /**
         * Sets the mana regeneration increase.
         *
         * @param manaRegenIncrease the mana regen increase
         * @return this builder
         */
        public Builder manaRegenIncrease(double manaRegenIncrease) {
            this.manaRegenIncrease = manaRegenIncrease;
            return this;
        }

        /**
         * Sets the stamina regeneration increase.
         *
         * @param staminaRegenIncrease the stamina regen increase
         * @return this builder
         */
        public Builder staminaRegenIncrease(double staminaRegenIncrease) {
            this.staminaRegenIncrease = staminaRegenIncrease;
            return this;
        }

        /**
         * Sets the skill points awarded.
         *
         * @param skillPointsAwarded the skill points
         * @return this builder
         */
        public Builder skillPointsAwarded(int skillPointsAwarded) {
            this.skillPointsAwarded = skillPointsAwarded;
            return this;
        }

        /**
         * Builds the LevelUpModifier.
         *
         * @return a new LevelUpModifier
         */
        public LevelUpModifier build() {
            return new LevelUpModifier(
                    attributeIncreases,
                    healthIncrease,
                    manaIncrease,
                    staminaIncrease,
                    healthRegenIncrease,
                    manaRegenIncrease,
                    staminaRegenIncrease,
                    skillPointsAwarded
            );
        }
    }

    @Override
    public String toString() {
        return "LevelUpModifier{" +
                "attributeIncreases=" + attributeIncreases +
                ", healthIncrease=" + healthIncrease +
                ", manaIncrease=" + manaIncrease +
                ", staminaIncrease=" + staminaIncrease +
                ", healthRegenIncrease=" + healthRegenIncrease +
                ", manaRegenIncrease=" + manaRegenIncrease +
                ", staminaRegenIncrease=" + staminaRegenIncrease +
                ", skillPointsAwarded=" + skillPointsAwarded +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LevelUpModifier that = (LevelUpModifier) o;
        return Double.compare(that.healthIncrease, healthIncrease) == 0 &&
               Double.compare(that.manaIncrease, manaIncrease) == 0 &&
               Double.compare(that.staminaIncrease, staminaIncrease) == 0 &&
               Double.compare(that.healthRegenIncrease, healthRegenIncrease) == 0 &&
               Double.compare(that.manaRegenIncrease, manaRegenIncrease) == 0 &&
               Double.compare(that.staminaRegenIncrease, staminaRegenIncrease) == 0 &&
               skillPointsAwarded == that.skillPointsAwarded &&
               Objects.equals(attributeIncreases, that.attributeIncreases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeIncreases, healthIncrease, manaIncrease, staminaIncrease,
                healthRegenIncrease, manaRegenIncrease, staminaRegenIncrease, skillPointsAwarded);
    }
}
