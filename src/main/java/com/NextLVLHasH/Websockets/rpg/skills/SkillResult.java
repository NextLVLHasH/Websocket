package com.NextLVLHasH.Websockets.rpg.skills;

import java.util.Objects;

/**
 * Represents the result of a skill activation attempt.
 * <p>
 * This class encapsulates the outcome of attempting to activate a skill,
 * including the status, a human-readable message, and any relevant data
 * such as remaining cooldown time or resource deficit.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SkillResult result = skillManager.activateSkill(playerId, "fireball", data, target);
 * 
 * if (result.isSuccess()) {
 *     // Skill was activated successfully
 * } else {
 *     switch (result.getStatus()) {
 *         case ON_COOLDOWN:
 *             long remaining = result.getCooldownRemaining();
 *             // Show cooldown message
 *             break;
 *         case INSUFFICIENT_MANA:
 *             double deficit = result.getResourceDeficit();
 *             // Show mana deficit message
 *             break;
 *     }
 * }
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class SkillResult {

    /**
     * Enumeration of possible skill activation statuses.
     */
    public enum Status {
        /**
         * The skill was activated successfully.
         */
        SUCCESS("Skill activated successfully"),

        /**
         * The skill is currently on cooldown.
         */
        ON_COOLDOWN("Skill is on cooldown"),

        /**
         * The player does not have enough mana to use the skill.
         */
        INSUFFICIENT_MANA("Not enough mana"),

        /**
         * The player does not have enough stamina to use the skill.
         */
        INSUFFICIENT_STAMINA("Not enough stamina"),

        /**
         * The player lacks the required permission to use the skill.
         */
        MISSING_PERMISSION("You don't have permission to use this skill"),

        /**
         * The player's level is too low to use the skill.
         */
        LEVEL_TOO_LOW("Your level is too low to use this skill"),

        /**
         * The skill has not been unlocked by the player.
         */
        SKILL_NOT_UNLOCKED("Skill has not been unlocked"),

        /**
         * The target is invalid for this skill.
         */
        INVALID_TARGET("Invalid target for this skill"),

        /**
         * The target is out of range for this skill.
         */
        OUT_OF_RANGE("Target is out of range"),

        /**
         * The skill activation was interrupted.
         */
        INTERRUPTED("Skill was interrupted"),

        /**
         * The skill does not exist or could not be found.
         */
        SKILL_NOT_FOUND("Skill not found"),

        /**
         * Generic failure status for unexpected errors.
         */
        FAILED("Skill activation failed");

        private final String defaultMessage;

        Status(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        /**
         * Gets the default message for this status.
         *
         * @return the default status message
         */
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    // ==================== Fields ====================

    private final Status status;
    private final String message;
    private final long cooldownRemaining;
    private final double resourceDeficit;
    private final String skillId;

    // ==================== Constructor ====================

    /**
     * Private constructor - use static factory methods to create instances.
     */
    private SkillResult(Status status, String message, long cooldownRemaining, double resourceDeficit, String skillId) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.message = message != null ? message : status.getDefaultMessage();
        this.cooldownRemaining = Math.max(0, cooldownRemaining);
        this.resourceDeficit = Math.max(0, resourceDeficit);
        this.skillId = skillId;
    }

    // ==================== Getters ====================

    /**
     * Gets the status of the skill activation attempt.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the human-readable message describing the result.
     *
     * @return the result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the remaining cooldown time in milliseconds.
     * <p>
     * This is only relevant when status is {@link Status#ON_COOLDOWN}.
     * </p>
     *
     * @return the remaining cooldown in milliseconds, or 0 if not applicable
     */
    public long getCooldownRemaining() {
        return cooldownRemaining;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @return the remaining cooldown in seconds, or 0 if not applicable
     */
    public double getCooldownRemainingSeconds() {
        return cooldownRemaining / 1000.0;
    }

    /**
     * Gets the resource deficit amount.
     * <p>
     * This is only relevant when status is {@link Status#INSUFFICIENT_MANA}
     * or {@link Status#INSUFFICIENT_STAMINA}.
     * </p>
     *
     * @return the resource deficit, or 0 if not applicable
     */
    public double getResourceDeficit() {
        return resourceDeficit;
    }

    /**
     * Gets the skill ID associated with this result.
     *
     * @return the skill ID, or null if not specified
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * Checks if the skill was activated successfully.
     *
     * @return true if the status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Checks if the skill activation failed.
     *
     * @return true if the status is not SUCCESS
     */
    public boolean isFailure() {
        return status != Status.SUCCESS;
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates a successful skill activation result.
     *
     * @return a SUCCESS result
     */
    public static SkillResult success() {
        return new SkillResult(Status.SUCCESS, null, 0, 0, null);
    }

    /**
     * Creates a successful skill activation result with a custom message.
     *
     * @param message the success message
     * @return a SUCCESS result with the specified message
     */
    public static SkillResult success(String message) {
        return new SkillResult(Status.SUCCESS, message, 0, 0, null);
    }

    /**
     * Creates a successful skill activation result for a specific skill.
     *
     * @param skillId the ID of the skill that was activated
     * @return a SUCCESS result
     */
    public static SkillResult success(String skillId, String message) {
        return new SkillResult(Status.SUCCESS, message, 0, 0, skillId);
    }

    /**
     * Creates an "on cooldown" result.
     *
     * @param remainingMillis the remaining cooldown time in milliseconds
     * @return an ON_COOLDOWN result
     */
    public static SkillResult onCooldown(long remainingMillis) {
        String message = String.format("Skill is on cooldown (%.1fs remaining)", remainingMillis / 1000.0);
        return new SkillResult(Status.ON_COOLDOWN, message, remainingMillis, 0, null);
    }

    /**
     * Creates an "on cooldown" result for a specific skill.
     *
     * @param skillId         the ID of the skill
     * @param remainingMillis the remaining cooldown time in milliseconds
     * @return an ON_COOLDOWN result
     */
    public static SkillResult onCooldown(String skillId, long remainingMillis) {
        String message = String.format("Skill is on cooldown (%.1fs remaining)", remainingMillis / 1000.0);
        return new SkillResult(Status.ON_COOLDOWN, message, remainingMillis, 0, skillId);
    }

    /**
     * Creates an "insufficient mana" result.
     *
     * @param deficit the amount of mana the player is missing
     * @return an INSUFFICIENT_MANA result
     */
    public static SkillResult insufficientMana(double deficit) {
        String message = String.format("Not enough mana (need %.1f more)", deficit);
        return new SkillResult(Status.INSUFFICIENT_MANA, message, 0, deficit, null);
    }

    /**
     * Creates an "insufficient mana" result for a specific skill.
     *
     * @param skillId the ID of the skill
     * @param deficit the amount of mana the player is missing
     * @return an INSUFFICIENT_MANA result
     */
    public static SkillResult insufficientMana(String skillId, double deficit) {
        String message = String.format("Not enough mana (need %.1f more)", deficit);
        return new SkillResult(Status.INSUFFICIENT_MANA, message, 0, deficit, skillId);
    }

    /**
     * Creates an "insufficient stamina" result.
     *
     * @param deficit the amount of stamina the player is missing
     * @return an INSUFFICIENT_STAMINA result
     */
    public static SkillResult insufficientStamina(double deficit) {
        String message = String.format("Not enough stamina (need %.1f more)", deficit);
        return new SkillResult(Status.INSUFFICIENT_STAMINA, message, 0, deficit, null);
    }

    /**
     * Creates an "insufficient stamina" result for a specific skill.
     *
     * @param skillId the ID of the skill
     * @param deficit the amount of stamina the player is missing
     * @return an INSUFFICIENT_STAMINA result
     */
    public static SkillResult insufficientStamina(String skillId, double deficit) {
        String message = String.format("Not enough stamina (need %.1f more)", deficit);
        return new SkillResult(Status.INSUFFICIENT_STAMINA, message, 0, deficit, skillId);
    }

    /**
     * Creates a "missing permission" result.
     *
     * @return a MISSING_PERMISSION result
     */
    public static SkillResult missingPermission() {
        return new SkillResult(Status.MISSING_PERMISSION, null, 0, 0, null);
    }

    /**
     * Creates a "missing permission" result with a custom message.
     *
     * @param message the custom message
     * @return a MISSING_PERMISSION result
     */
    public static SkillResult missingPermission(String message) {
        return new SkillResult(Status.MISSING_PERMISSION, message, 0, 0, null);
    }

    /**
     * Creates a "level too low" result.
     *
     * @param requiredLevel the required level
     * @param currentLevel  the player's current level
     * @return a LEVEL_TOO_LOW result
     */
    public static SkillResult levelTooLow(int requiredLevel, int currentLevel) {
        String message = String.format("Level too low (need level %d, you are level %d)", requiredLevel, currentLevel);
        return new SkillResult(Status.LEVEL_TOO_LOW, message, 0, 0, null);
    }

    /**
     * Creates a "skill not unlocked" result.
     *
     * @return a SKILL_NOT_UNLOCKED result
     */
    public static SkillResult notUnlocked() {
        return new SkillResult(Status.SKILL_NOT_UNLOCKED, null, 0, 0, null);
    }

    /**
     * Creates a "skill not unlocked" result for a specific skill.
     *
     * @param skillId the ID of the skill
     * @return a SKILL_NOT_UNLOCKED result
     */
    public static SkillResult notUnlocked(String skillId) {
        String message = String.format("Skill '%s' has not been unlocked", skillId);
        return new SkillResult(Status.SKILL_NOT_UNLOCKED, message, 0, 0, skillId);
    }

    /**
     * Creates an "invalid target" result.
     *
     * @return an INVALID_TARGET result
     */
    public static SkillResult invalidTarget() {
        return new SkillResult(Status.INVALID_TARGET, null, 0, 0, null);
    }

    /**
     * Creates an "invalid target" result with a custom message.
     *
     * @param message the custom message
     * @return an INVALID_TARGET result
     */
    public static SkillResult invalidTarget(String message) {
        return new SkillResult(Status.INVALID_TARGET, message, 0, 0, null);
    }

    /**
     * Creates an "out of range" result.
     *
     * @return an OUT_OF_RANGE result
     */
    public static SkillResult outOfRange() {
        return new SkillResult(Status.OUT_OF_RANGE, null, 0, 0, null);
    }

    /**
     * Creates an "out of range" result with range information.
     *
     * @param maxRange     the maximum range of the skill
     * @param actualRange  the actual distance to target
     * @return an OUT_OF_RANGE result
     */
    public static SkillResult outOfRange(double maxRange, double actualRange) {
        String message = String.format("Target is out of range (max: %.1f, actual: %.1f)", maxRange, actualRange);
        return new SkillResult(Status.OUT_OF_RANGE, message, 0, 0, null);
    }

    /**
     * Creates an "interrupted" result.
     *
     * @return an INTERRUPTED result
     */
    public static SkillResult interrupted() {
        return new SkillResult(Status.INTERRUPTED, null, 0, 0, null);
    }

    /**
     * Creates an "interrupted" result with a custom message.
     *
     * @param message the custom message
     * @return an INTERRUPTED result
     */
    public static SkillResult interrupted(String message) {
        return new SkillResult(Status.INTERRUPTED, message, 0, 0, null);
    }

    /**
     * Creates a "skill not found" result.
     *
     * @param skillId the ID of the skill that was not found
     * @return a SKILL_NOT_FOUND result
     */
    public static SkillResult notFound(String skillId) {
        String message = String.format("Skill '%s' not found", skillId);
        return new SkillResult(Status.SKILL_NOT_FOUND, message, 0, 0, skillId);
    }

    /**
     * Creates a generic failure result.
     *
     * @param message the failure message
     * @return a FAILED result
     */
    public static SkillResult failed(String message) {
        return new SkillResult(Status.FAILED, message, 0, 0, null);
    }

    /**
     * Creates a result from a specific status.
     *
     * @param status the status
     * @return a result with the specified status and default message
     */
    public static SkillResult of(Status status) {
        return new SkillResult(status, null, 0, 0, null);
    }

    /**
     * Creates a result from a specific status with a custom message.
     *
     * @param status  the status
     * @param message the custom message
     * @return a result with the specified status and message
     */
    public static SkillResult of(Status status, String message) {
        return new SkillResult(status, message, 0, 0, null);
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SkillResult{status=").append(status);
        sb.append(", message='").append(message).append("'");
        if (cooldownRemaining > 0) {
            sb.append(", cooldownRemaining=").append(cooldownRemaining).append("ms");
        }
        if (resourceDeficit > 0) {
            sb.append(", resourceDeficit=").append(resourceDeficit);
        }
        if (skillId != null) {
            sb.append(", skillId='").append(skillId).append("'");
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillResult that = (SkillResult) o;
        return cooldownRemaining == that.cooldownRemaining &&
                Double.compare(that.resourceDeficit, resourceDeficit) == 0 &&
                status == that.status &&
                Objects.equals(message, that.message) &&
                Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message, cooldownRemaining, resourceDeficit, skillId);
    }
}
