package com.NextLVLHasH.Websockets.rpg.progression;

/**
 * Interface defining an experience curve for level progression.
 * <p>
 * An experience curve determines how much XP is required to reach each level.
 * Different implementations can provide linear, exponential, polynomial, or
 * custom progression curves to suit different game balance needs.
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public interface ExperienceCurve {

    /**
     * Calculates the total XP required to reach the specified level from level 1.
     * <p>
     * For example, if level 2 requires 100 XP and level 3 requires 200 XP,
     * then calculateXPForLevel(3) would return 300 (100 + 200).
     * </p>
     *
     * @param level the target level (must be >= 1)
     * @return the total accumulated XP required to reach this level
     * @throws IllegalArgumentException if level is less than 1
     */
    long calculateXPForLevel(int level);

    /**
     * Calculates the level achieved given a total accumulated XP amount.
     * <p>
     * This is the inverse of {@link #calculateXPForLevel(int)}.
     * For example, if 300 total XP reaches level 3, then
     * calculateLevelFromXP(350) would return 3.
     * </p>
     *
     * @param totalXP the total accumulated XP (must be >= 0)
     * @return the level achieved with this XP amount (always >= 1)
     */
    int calculateLevelFromXP(long totalXP);

    /**
     * Calculates the remaining XP needed to reach the next level.
     * <p>
     * This takes into account the current level and the XP already accumulated
     * within that level.
     * </p>
     *
     * @param currentLevel the player's current level (must be >= 1)
     * @param currentXP the player's current total accumulated XP
     * @return the XP still needed to level up, or 0 if already past the threshold
     */
    long getXPToNextLevel(int currentLevel, long currentXP);

    /**
     * Gets the XP required to go from a specific level to the next level.
     * <p>
     * Unlike {@link #calculateXPForLevel(int)} which returns cumulative XP,
     * this returns just the XP needed for a single level transition.
     * </p>
     *
     * @param level the level to calculate XP requirement for
     * @return the XP needed to go from this level to the next
     */
    default long getXPRequiredForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        return calculateXPForLevel(level + 1) - calculateXPForLevel(level);
    }

    /**
     * Gets a human-readable name for this curve type.
     *
     * @return the curve type name (e.g., "linear", "exponential")
     */
    String getCurveType();
}
