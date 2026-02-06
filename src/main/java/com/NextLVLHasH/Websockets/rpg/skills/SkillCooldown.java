package com.NextLVLHasH.Websockets.rpg.skills;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks skill cooldowns for a single player.
 * <p>
 * This class manages cooldown timers for all skills a player has used,
 * allowing for checking, reducing, and resetting cooldowns.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SkillCooldown cooldowns = new SkillCooldown();
 * cooldowns.setCooldown("fireball", 5000); // 5 second cooldown
 * 
 * if (cooldowns.isOnCooldown("fireball")) {
 *     long remaining = cooldowns.getRemainingCooldown("fireball");
 *     // Can't use skill yet
 * }
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class SkillCooldown {

    /**
     * Map of skill IDs to their expiration timestamps (System.currentTimeMillis()).
     */
    private final Map<String, Long> cooldowns;

    /**
     * Creates a new SkillCooldown tracker with no active cooldowns.
     */
    public SkillCooldown() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new SkillCooldown tracker with pre-existing cooldowns.
     *
     * @param existingCooldowns map of skill IDs to expiration timestamps
     */
    public SkillCooldown(Map<String, Long> existingCooldowns) {
        this.cooldowns = new ConcurrentHashMap<>();
        if (existingCooldowns != null) {
            this.cooldowns.putAll(existingCooldowns);
        }
    }

    // ==================== Core Methods ====================

    /**
     * Sets a cooldown for the specified skill.
     *
     * @param skillId       the unique identifier of the skill
     * @param durationMillis the cooldown duration in milliseconds
     * @throws IllegalArgumentException if skillId is null or empty
     */
    public void setCooldown(String skillId, long durationMillis) {
        if (skillId == null || skillId.isEmpty()) {
            throw new IllegalArgumentException("Skill ID cannot be null or empty");
        }
        if (durationMillis <= 0) {
            cooldowns.remove(skillId);
            return;
        }
        long expiresAt = System.currentTimeMillis() + durationMillis;
        cooldowns.put(skillId, expiresAt);
    }

    /**
     * Checks if the specified skill is currently on cooldown.
     *
     * @param skillId the unique identifier of the skill
     * @return true if the skill is on cooldown, false otherwise
     */
    public boolean isOnCooldown(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return false;
        }
        Long expiresAt = cooldowns.get(skillId);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiresAt) {
            cooldowns.remove(skillId);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time for the specified skill.
     *
     * @param skillId the unique identifier of the skill
     * @return the remaining cooldown in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return 0L;
        }
        Long expiresAt = cooldowns.get(skillId);
        if (expiresAt == null) {
            return 0L;
        }
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(skillId);
            return 0L;
        }
        return remaining;
    }

    /**
     * Gets the remaining cooldown time for the specified skill in seconds.
     *
     * @param skillId the unique identifier of the skill
     * @return the remaining cooldown in seconds, or 0.0 if not on cooldown
     */
    public double getRemainingCooldownSeconds(String skillId) {
        return getRemainingCooldown(skillId) / 1000.0;
    }

    /**
     * Reduces the cooldown for the specified skill by the given amount.
     *
     * @param skillId         the unique identifier of the skill
     * @param reductionMillis the amount to reduce the cooldown by, in milliseconds
     * @return the new remaining cooldown in milliseconds, or 0 if cooldown is now complete
     */
    public long reduceCooldown(String skillId, long reductionMillis) {
        if (skillId == null || skillId.isEmpty() || reductionMillis <= 0) {
            return getRemainingCooldown(skillId);
        }
        Long expiresAt = cooldowns.get(skillId);
        if (expiresAt == null) {
            return 0L;
        }
        long newExpiresAt = expiresAt - reductionMillis;
        long currentTime = System.currentTimeMillis();
        if (newExpiresAt <= currentTime) {
            cooldowns.remove(skillId);
            return 0L;
        }
        cooldowns.put(skillId, newExpiresAt);
        return newExpiresAt - currentTime;
    }

    /**
     * Reduces the cooldown for the specified skill by a percentage.
     *
     * @param skillId            the unique identifier of the skill
     * @param reductionPercent   the percentage to reduce (0.0 to 1.0, where 0.5 = 50%)
     * @return the new remaining cooldown in milliseconds, or 0 if cooldown is now complete
     */
    public long reduceCooldownPercent(String skillId, double reductionPercent) {
        if (skillId == null || skillId.isEmpty() || reductionPercent <= 0) {
            return getRemainingCooldown(skillId);
        }
        long remaining = getRemainingCooldown(skillId);
        if (remaining <= 0) {
            return 0L;
        }
        long reduction = (long) (remaining * Math.min(1.0, reductionPercent));
        return reduceCooldown(skillId, reduction);
    }

    /**
     * Resets (removes) the cooldown for the specified skill.
     *
     * @param skillId the unique identifier of the skill
     */
    public void resetCooldown(String skillId) {
        if (skillId != null && !skillId.isEmpty()) {
            cooldowns.remove(skillId);
        }
    }

    /**
     * Resets (removes) all cooldowns for this player.
     */
    public void resetAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * Performs a tick operation, cleaning up expired cooldowns.
     * <p>
     * This method can be called periodically to remove expired entries
     * from the cooldown map, reducing memory usage.
     * </p>
     *
     * @return the number of expired cooldowns that were removed
     */
    public int tick() {
        long currentTime = System.currentTimeMillis();
        int removed = 0;
        
        Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() <= currentTime) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    // ==================== Query Methods ====================

    /**
     * Gets all skill IDs that are currently on cooldown.
     *
     * @return an unmodifiable set of skill IDs on cooldown
     */
    public Set<String> getSkillsOnCooldown() {
        tick(); // Clean up expired first
        return Collections.unmodifiableSet(new HashSet<>(cooldowns.keySet()));
    }

    /**
     * Gets the total number of skills currently on cooldown.
     *
     * @return the count of skills on cooldown
     */
    public int getCooldownCount() {
        tick(); // Clean up expired first
        return cooldowns.size();
    }

    /**
     * Checks if there are any active cooldowns.
     *
     * @return true if at least one skill is on cooldown
     */
    public boolean hasAnyCooldowns() {
        return getCooldownCount() > 0;
    }

    /**
     * Gets all cooldowns as a map of skill ID to remaining time in milliseconds.
     *
     * @return an unmodifiable map of skill IDs to remaining cooldown times
     */
    public Map<String, Long> getAllRemainingCooldowns() {
        tick(); // Clean up expired first
        Map<String, Long> remaining = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            long timeLeft = entry.getValue() - currentTime;
            if (timeLeft > 0) {
                remaining.put(entry.getKey(), timeLeft);
            }
        }
        return Collections.unmodifiableMap(remaining);
    }

    /**
     * Gets the raw cooldown data for serialization purposes.
     * <p>
     * This returns the internal map of skill IDs to expiration timestamps.
     * </p>
     *
     * @return an unmodifiable map of skill IDs to expiration timestamps
     */
    public Map<String, Long> getCooldownData() {
        return Collections.unmodifiableMap(new HashMap<>(cooldowns));
    }

    // ==================== Utility Methods ====================

    /**
     * Copies all cooldowns from another SkillCooldown instance.
     *
     * @param other the SkillCooldown to copy from
     */
    public void copyFrom(SkillCooldown other) {
        if (other != null) {
            this.cooldowns.clear();
            this.cooldowns.putAll(other.cooldowns);
        }
    }

    @Override
    public String toString() {
        return "SkillCooldown{" +
                "activeCooldowns=" + getCooldownCount() +
                ", skills=" + getSkillsOnCooldown() +
                '}';
    }
}
