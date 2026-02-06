package com.NextLVLHasH.Websockets.rpg.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete data class representing all RPG-related data for a player.
 * This class holds character stats, attributes, skills, and progression data.
 */
public class PlayerRPGData {

    // Identity
    private final UUID playerId;
    private String playerName;

    // Character Creation
    private String selectedClass;
    private String selectedRace;
    private String selectedProfession;
    private boolean characterCreated;

    // Progression
    private int level;
    private long currentXP;
    private long xpToNextLevel;
    private int skillPoints;

    // Attributes
    private final Map<AttributeType, Integer> baseAttributes;
    private final Map<AttributeType, Integer> currentAttributes;

    // Resources
    private double maxHealth;
    private double currentHealth;
    private double maxMana;
    private double currentMana;
    private double maxStamina;
    private double currentStamina;

    // Regeneration rates (per second)
    private double healthRegen;
    private double manaRegen;
    private double staminaRegen;

    // Skills
    private final Set<String> unlockedSkills;
    private final Map<String, Long> skillCooldowns;

    // Effects (using String placeholder for now, will be EffectInstance later)
    private final List<String> activeEffects;

    // Statistics tracking for XP activities
    private final Map<String, Long> statistics;

    // Timestamps
    private final long createdAt;
    private long lastUpdated;

    // Constants
    private static final int DEFAULT_BASE_ATTRIBUTE = 10;
    private static final double DEFAULT_MAX_HEALTH = 100.0;
    private static final double DEFAULT_MAX_MANA = 50.0;
    private static final double DEFAULT_MAX_STAMINA = 100.0;
    private static final double DEFAULT_HEALTH_REGEN = 1.0;
    private static final double DEFAULT_MANA_REGEN = 2.0;
    private static final double DEFAULT_STAMINA_REGEN = 5.0;
    private static final long DEFAULT_XP_TO_NEXT_LEVEL = 100L;

    /**
     * Creates a new PlayerRPGData instance with default values.
     *
     * @param playerId   the unique identifier of the player
     * @param playerName the display name of the player
     */
    public PlayerRPGData(UUID playerId, String playerName) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");

        // Initialize character creation state
        this.selectedClass = null;
        this.selectedRace = null;
        this.selectedProfession = null;
        this.characterCreated = false;

        // Initialize progression
        this.level = 1;
        this.currentXP = 0L;
        this.xpToNextLevel = DEFAULT_XP_TO_NEXT_LEVEL;
        this.skillPoints = 0;

        // Initialize attributes with default values
        this.baseAttributes = new EnumMap<>(AttributeType.class);
        this.currentAttributes = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            baseAttributes.put(type, DEFAULT_BASE_ATTRIBUTE);
            currentAttributes.put(type, DEFAULT_BASE_ATTRIBUTE);
        }

        // Initialize resources
        this.maxHealth = DEFAULT_MAX_HEALTH;
        this.currentHealth = DEFAULT_MAX_HEALTH;
        this.maxMana = DEFAULT_MAX_MANA;
        this.currentMana = DEFAULT_MAX_MANA;
        this.maxStamina = DEFAULT_MAX_STAMINA;
        this.currentStamina = DEFAULT_MAX_STAMINA;

        // Initialize regeneration rates
        this.healthRegen = DEFAULT_HEALTH_REGEN;
        this.manaRegen = DEFAULT_MANA_REGEN;
        this.staminaRegen = DEFAULT_STAMINA_REGEN;

        // Initialize skills
        this.unlockedSkills = ConcurrentHashMap.newKeySet();
        this.skillCooldowns = new ConcurrentHashMap<>();

        // Initialize effects
        this.activeEffects = Collections.synchronizedList(new ArrayList<>());

        // Initialize statistics
        this.statistics = new ConcurrentHashMap<>();

        // Initialize timestamps
        this.createdAt = System.currentTimeMillis();
        this.lastUpdated = this.createdAt;
    }

    // ==================== Identity Getters/Setters ====================

    /**
     * Gets the player's unique identifier.
     *
     * @return the player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Gets the player's display name.
     *
     * @return the player name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Sets the player's display name.
     *
     * @param playerName the new player name
     */
    public void setPlayerName(String playerName) {
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        markUpdated();
    }

    // ==================== Character Creation Getters/Setters ====================

    /**
     * Gets the selected class ID.
     *
     * @return the class ID, or null if not selected
     */
    public String getSelectedClass() {
        return selectedClass;
    }

    /**
     * Sets the selected class ID.
     *
     * @param selectedClass the class ID to select
     */
    public void setSelectedClass(String selectedClass) {
        this.selectedClass = selectedClass;
        markUpdated();
    }

    /**
     * Gets the selected race ID.
     *
     * @return the race ID, or null if not selected
     */
    public String getSelectedRace() {
        return selectedRace;
    }

    /**
     * Sets the selected race ID.
     *
     * @param selectedRace the race ID to select
     */
    public void setSelectedRace(String selectedRace) {
        this.selectedRace = selectedRace;
        markUpdated();
    }

    /**
     * Gets the selected profession ID.
     *
     * @return the profession ID, or null if not selected
     */
    public String getSelectedProfession() {
        return selectedProfession;
    }

    /**
     * Sets the selected profession ID.
     *
     * @param selectedProfession the profession ID to select
     */
    public void setSelectedProfession(String selectedProfession) {
        this.selectedProfession = selectedProfession;
        markUpdated();
    }

    /**
     * Checks if the character has been created.
     *
     * @return true if character creation is complete
     */
    public boolean isCharacterCreated() {
        return characterCreated;
    }

    /**
     * Sets the character creation status.
     *
     * @param characterCreated true if character creation is complete
     */
    public void setCharacterCreated(boolean characterCreated) {
        this.characterCreated = characterCreated;
        markUpdated();
    }

    // ==================== Progression Getters/Setters ====================

    /**
     * Gets the current level.
     *
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the current level.
     *
     * @param level the new level
     */
    public void setLevel(int level) {
        this.level = Math.max(1, level);
        markUpdated();
    }

    /**
     * Gets the current XP amount.
     *
     * @return the current XP
     */
    public long getCurrentXP() {
        return currentXP;
    }

    /**
     * Sets the current XP amount.
     *
     * @param currentXP the new XP amount
     */
    public void setCurrentXP(long currentXP) {
        this.currentXP = Math.max(0, currentXP);
        markUpdated();
    }

    /**
     * Removes XP from the player (e.g., death penalty).
     * The XP cannot go below 0.
     *
     * @param amount the amount of XP to remove
     * @return the actual amount removed
     */
    public long removeXP(long amount) {
        if (amount <= 0) return 0;
        long oldXP = this.currentXP;
        this.currentXP = Math.max(0, currentXP - amount);
        markUpdated();
        return oldXP - this.currentXP;
    }

    /**
     * Gets the XP required for the next level.
     *
     * @return XP needed to level up
     */
    public long getXpToNextLevel() {
        return xpToNextLevel;
    }

    /**
     * Sets the XP required for the next level.
     *
     * @param xpToNextLevel XP needed to level up
     */
    public void setXpToNextLevel(long xpToNextLevel) {
        this.xpToNextLevel = Math.max(1, xpToNextLevel);
        markUpdated();
    }

    /**
     * Gets the available skill points.
     *
     * @return the skill points
     */
    public int getSkillPoints() {
        return skillPoints;
    }

    /**
     * Sets the available skill points.
     *
     * @param skillPoints the new skill point total
     */
    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
        markUpdated();
    }

    // ==================== Attribute Getters/Setters ====================

    /**
     * Gets the base attributes map (unmodifiable view).
     *
     * @return unmodifiable map of base attributes
     */
    public Map<AttributeType, Integer> getBaseAttributes() {
        return Collections.unmodifiableMap(baseAttributes);
    }

    /**
     * Gets a specific base attribute value.
     *
     * @param type the attribute type
     * @return the base attribute value
     */
    public int getBaseAttribute(AttributeType type) {
        return baseAttributes.getOrDefault(type, DEFAULT_BASE_ATTRIBUTE);
    }

    /**
     * Sets a base attribute value.
     *
     * @param type  the attribute type
     * @param value the new value
     */
    public void setBaseAttribute(AttributeType type, int value) {
        baseAttributes.put(type, Math.max(1, value));
        markUpdated();
    }

    /**
     * Gets the current attributes map (unmodifiable view).
     *
     * @return unmodifiable map of current attributes
     */
    public Map<AttributeType, Integer> getCurrentAttributes() {
        return Collections.unmodifiableMap(currentAttributes);
    }

    /**
     * Gets a specific current attribute value (includes modifiers).
     *
     * @param type the attribute type
     * @return the current attribute value
     */
    public int getCurrentAttribute(AttributeType type) {
        return currentAttributes.getOrDefault(type, DEFAULT_BASE_ATTRIBUTE);
    }

    /**
     * Sets a current attribute value.
     *
     * @param type  the attribute type
     * @param value the new value
     */
    public void setCurrentAttribute(AttributeType type, int value) {
        currentAttributes.put(type, Math.max(0, value));
        markUpdated();
    }

    /**
     * Recalculates current attributes from base attributes.
     * Override this to add modifier calculations.
     */
    public void recalculateCurrentAttributes() {
        for (AttributeType type : AttributeType.values()) {
            currentAttributes.put(type, baseAttributes.getOrDefault(type, DEFAULT_BASE_ATTRIBUTE));
        }
        markUpdated();
    }

    // ==================== Resource Getters/Setters ====================

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(1.0, maxHealth);
        this.currentHealth = Math.min(this.currentHealth, this.maxHealth);
        markUpdated();
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = Math.max(0.0, Math.min(currentHealth, maxHealth));
        markUpdated();
    }

    /**
     * Modifies the current health by the specified amount.
     * Positive values heal, negative values damage.
     * Result is clamped between 0 and maxHealth.
     *
     * @param amount the amount to modify (positive = heal, negative = damage)
     * @return the actual amount changed
     */
    public double modifyCurrentHealth(double amount) {
        double oldHealth = this.currentHealth;
        this.currentHealth = Math.max(0, Math.min(maxHealth, currentHealth + amount));
        markUpdated();
        return this.currentHealth - oldHealth;
    }

    public double getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(double maxMana) {
        this.maxMana = Math.max(0.0, maxMana);
        this.currentMana = Math.min(this.currentMana, this.maxMana);
        markUpdated();
    }

    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = Math.max(0.0, Math.min(currentMana, maxMana));
        markUpdated();
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(double maxStamina) {
        this.maxStamina = Math.max(0.0, maxStamina);
        this.currentStamina = Math.min(this.currentStamina, this.maxStamina);
        markUpdated();
    }

    public double getCurrentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(double currentStamina) {
        this.currentStamina = Math.max(0.0, Math.min(currentStamina, maxStamina));
        markUpdated();
    }

    public double getHealthRegen() {
        return healthRegen;
    }

    public void setHealthRegen(double healthRegen) {
        this.healthRegen = Math.max(0.0, healthRegen);
        markUpdated();
    }

    public double getManaRegen() {
        return manaRegen;
    }

    public void setManaRegen(double manaRegen) {
        this.manaRegen = Math.max(0.0, manaRegen);
        markUpdated();
    }

    public double getStaminaRegen() {
        return staminaRegen;
    }

    public void setStaminaRegen(double staminaRegen) {
        this.staminaRegen = Math.max(0.0, staminaRegen);
        markUpdated();
    }

    // ==================== Skills ====================

    /**
     * Gets the set of unlocked skill IDs (unmodifiable view).
     *
     * @return unmodifiable set of unlocked skills
     */
    public Set<String> getUnlockedSkills() {
        return Collections.unmodifiableSet(unlockedSkills);
    }

    /**
     * Gets the skill cooldowns map (unmodifiable view).
     *
     * @return unmodifiable map of skill cooldowns
     */
    public Map<String, Long> getSkillCooldowns() {
        return Collections.unmodifiableMap(skillCooldowns);
    }

    /**
     * Unlocks a skill for this player.
     *
     * @param skillId the skill ID to unlock
     * @return true if the skill was newly unlocked, false if already unlocked
     */
    public boolean unlockSkill(String skillId) {
        Objects.requireNonNull(skillId, "Skill ID cannot be null");
        boolean added = unlockedSkills.add(skillId);
        if (added) {
            markUpdated();
        }
        return added;
    }

    /**
     * Checks if a skill is unlocked.
     *
     * @param skillId the skill ID to check
     * @return true if the skill is unlocked
     */
    public boolean isSkillUnlocked(String skillId) {
        return skillId != null && unlockedSkills.contains(skillId);
    }

    /**
     * Checks if a skill is currently on cooldown.
     *
     * @param skillId the skill ID to check
     * @return true if the skill is on cooldown
     */
    public boolean isOnCooldown(String skillId) {
        if (skillId == null) return false;
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return false;
        if (System.currentTimeMillis() >= cooldownEnd) {
            skillCooldowns.remove(skillId);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time for a skill in milliseconds.
     *
     * @param skillId the skill ID to check
     * @return remaining cooldown in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(String skillId) {
        if (skillId == null) return 0L;
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return 0L;
        long remaining = cooldownEnd - System.currentTimeMillis();
        if (remaining <= 0) {
            skillCooldowns.remove(skillId);
            return 0L;
        }
        return remaining;
    }

    /**
     * Sets a cooldown for a skill.
     *
     * @param skillId        the skill ID
     * @param durationMillis the cooldown duration in milliseconds
     */
    public void setCooldown(String skillId, long durationMillis) {
        Objects.requireNonNull(skillId, "Skill ID cannot be null");
        if (durationMillis > 0) {
            skillCooldowns.put(skillId, System.currentTimeMillis() + durationMillis);
        } else {
            skillCooldowns.remove(skillId);
        }
        markUpdated();
    }

    /**
     * Clears the cooldown for a skill.
     *
     * @param skillId the skill ID
     */
    public void clearCooldown(String skillId) {
        if (skillId != null) {
            skillCooldowns.remove(skillId);
            markUpdated();
        }
    }

    /**
     * Clears all skill cooldowns.
     */
    public void clearAllCooldowns() {
        skillCooldowns.clear();
        markUpdated();
    }

    // ==================== Effects ====================

    /**
     * Gets the list of active effects (unmodifiable view).
     *
     * @return unmodifiable list of active effects
     */
    public List<String> getActiveEffects() {
        return Collections.unmodifiableList(activeEffects);
    }

    /**
     * Adds an active effect.
     *
     * @param effect the effect to add
     */
    public void addActiveEffect(String effect) {
        if (effect != null) {
            activeEffects.add(effect);
            markUpdated();
        }
    }

    /**
     * Removes an active effect.
     *
     * @param effect the effect to remove
     * @return true if the effect was removed
     */
    public boolean removeActiveEffect(String effect) {
        boolean removed = activeEffects.remove(effect);
        if (removed) {
            markUpdated();
        }
        return removed;
    }

    /**
     * Clears all active effects.
     */
    public void clearActiveEffects() {
        activeEffects.clear();
        markUpdated();
    }

    // ==================== Timestamps ====================

    /**
     * Gets the creation timestamp.
     *
     * @return the creation time in milliseconds since epoch
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the last update time in milliseconds since epoch
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Marks this data as updated (sets lastUpdated to current time).
     */
    public void markUpdated() {
        this.lastUpdated = System.currentTimeMillis();
    }

    // ==================== Progression Methods ====================

    /**
     * Adds experience points to the player, handling level ups.
     *
     * @param amount the XP to add (must be positive)
     * @return the number of levels gained
     */
    public int addXP(long amount) {
        if (amount <= 0) return 0;

        int levelsGained = 0;
        currentXP += amount;

        // Handle multiple level ups
        while (currentXP >= xpToNextLevel) {
            currentXP -= xpToNextLevel;
            levelsGained += levelUp();
        }

        markUpdated();
        return levelsGained;
    }

    /**
     * Performs a level up, increasing level and granting skill points.
     *
     * @return 1 if leveled up successfully, 0 otherwise
     */
    public int levelUp() {
        level++;
        
        // Grant skill points (default: 1 per level)
        skillPoints++;

        // Increase XP requirement for next level (exponential curve)
        xpToNextLevel = calculateXPForLevel(level + 1);

        // Increase max resources based on level
        maxHealth += 10;
        currentHealth = maxHealth;
        maxMana += 5;
        currentMana = maxMana;
        maxStamina += 5;
        currentStamina = maxStamina;

        markUpdated();
        return 1;
    }

    /**
     * Calculates the XP required to reach a specific level.
     *
     * @param targetLevel the target level
     * @return the XP required
     */
    private long calculateXPForLevel(int targetLevel) {
        // Exponential curve: base * (multiplier ^ level)
        return (long) (100 * Math.pow(1.5, targetLevel - 1));
    }

    /**
     * Adds skill points to the player.
     *
     * @param amount the number of skill points to add
     */
    public void addSkillPoints(int amount) {
        if (amount > 0) {
            this.skillPoints += amount;
            markUpdated();
        }
    }

    /**
     * Spends skill points (for purchasing skills or upgrades).
     *
     * @param amount the number of skill points to spend
     * @return true if successful, false if insufficient points
     */
    public boolean spendSkillPoints(int amount) {
        if (amount <= 0 || skillPoints < amount) {
            return false;
        }
        skillPoints -= amount;
        markUpdated();
        return true;
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if the player is alive.
     *
     * @return true if current health is greater than 0
     */
    public boolean isAlive() {
        return currentHealth > 0;
    }

    /**
     * Fully restores all resources to maximum.
     */
    public void fullRestore() {
        currentHealth = maxHealth;
        currentMana = maxMana;
        currentStamina = maxStamina;
        clearAllCooldowns();
        markUpdated();
    }

    /**
     * Applies damage to the player.
     *
     * @param amount the damage amount
     * @return the actual damage dealt (after clamping)
     */
    public double takeDamage(double amount) {
        if (amount <= 0) return 0;
        double actualDamage = Math.min(amount, currentHealth);
        currentHealth -= actualDamage;
        markUpdated();
        return actualDamage;
    }

    /**
     * Heals the player.
     *
     * @param amount the heal amount
     * @return the actual amount healed (after clamping)
     */
    public double heal(double amount) {
        if (amount <= 0) return 0;
        double missingHealth = maxHealth - currentHealth;
        double actualHeal = Math.min(amount, missingHealth);
        currentHealth += actualHeal;
        markUpdated();
        return actualHeal;
    }

    /**
     * Consumes mana.
     *
     * @param amount the mana cost
     * @return true if mana was consumed, false if insufficient
     */
    public boolean consumeMana(double amount) {
        if (amount <= 0) return true;
        if (currentMana < amount) return false;
        currentMana -= amount;
        markUpdated();
        return true;
    }

    /**
     * Consumes stamina.
     *
     * @param amount the stamina cost
     * @return true if stamina was consumed, false if insufficient
     */
    public boolean consumeStamina(double amount) {
        if (amount <= 0) return true;
        if (currentStamina < amount) return false;
        currentStamina -= amount;
        markUpdated();
        return true;
    }

    // ==================== Statistics Methods ====================

    /**
     * Increments a statistic by 1.
     *
     * @param statName the name of the statistic
     */
    public void incrementStat(String statName) {
        incrementStat(statName, 1);
    }

    /**
     * Increments a statistic by a specified amount.
     *
     * @param statName the name of the statistic
     * @param amount the amount to add
     */
    public void incrementStat(String statName, long amount) {
        statistics.merge(statName, amount, Long::sum);
        markUpdated();
    }

    /**
     * Gets the value of a statistic.
     *
     * @param statName the name of the statistic
     * @return the statistic value, or 0 if not set
     */
    public long getStat(String statName) {
        return statistics.getOrDefault(statName, 0L);
    }

    /**
     * Gets all statistics.
     *
     * @return unmodifiable map of all statistics
     */
    public Map<String, Long> getAllStats() {
        return Collections.unmodifiableMap(new HashMap<>(statistics));
    }

    /**
     * Resets a statistic to zero.
     *
     * @param statName the name of the statistic to reset
     */
    public void resetStat(String statName) {
        statistics.remove(statName);
        markUpdated();
    }

    @Override
    public String toString() {
        return "PlayerRPGData{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", level=" + level +
                ", class='" + selectedClass + '\'' +
                ", race='" + selectedRace + '\'' +
                ", characterCreated=" + characterCreated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerRPGData that = (PlayerRPGData) o;
        return Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }
}
