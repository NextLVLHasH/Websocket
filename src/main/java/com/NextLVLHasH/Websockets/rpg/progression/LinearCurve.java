package com.NextLVLHasH.Websockets.rpg.progression;

import java.util.Objects;

/**
 * A linear experience curve implementation.
 * <p>
 * Formula: XPRequired(level) = baseXP + (level * increment)
 * </p>
 * <p>
 * This creates a straight-line progression where each level requires
 * a fixed amount more XP than the previous level.
 * </p>
 * <p>
 * Example with baseXP=100 and increment=50:
 * <ul>
 *   <li>Level 1→2: 100 + (1 * 50) = 150 XP</li>
 *   <li>Level 2→3: 100 + (2 * 50) = 200 XP</li>
 *   <li>Level 3→4: 100 + (3 * 50) = 250 XP</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class LinearCurve implements ExperienceCurve {

    /**
     * The base XP requirement (constant component).
     */
    private final long baseXP;

    /**
     * The XP increment per level (linear component).
     */
    private final long increment;

    /**
     * Creates a new LinearCurve with the specified parameters.
     *
     * @param baseXP the base XP requirement (must be > 0)
     * @param increment the XP increment per level (must be >= 0)
     * @throws IllegalArgumentException if baseXP <= 0 or increment < 0
     */
    public LinearCurve(long baseXP, long increment) {
        if (baseXP <= 0) {
            throw new IllegalArgumentException("Base XP must be greater than 0");
        }
        if (increment < 0) {
            throw new IllegalArgumentException("Increment cannot be negative");
        }
        this.baseXP = baseXP;
        this.increment = increment;
    }

    /**
     * Creates a LinearCurve with default values (baseXP=100, increment=50).
     *
     * @return a new LinearCurve with default settings
     */
    public static LinearCurve createDefault() {
        return new LinearCurve(100, 50);
    }

    @Override
    public long calculateXPForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        if (level == 1) {
            return 0L; // Level 1 requires no XP
        }

        // Sum of XP required for levels 1 to (level-1)
        // XP(n) = baseXP + n * increment
        // Total = sum from n=1 to (level-1) of (baseXP + n * increment)
        // Total = (level-1) * baseXP + increment * sum(1 to level-1)
        // Total = (level-1) * baseXP + increment * (level-1) * level / 2
        long levelsCompleted = level - 1;
        long baseTotal = levelsCompleted * baseXP;
        long incrementTotal = increment * levelsCompleted * level / 2;
        return baseTotal + incrementTotal;
    }

    @Override
    public int calculateLevelFromXP(long totalXP) {
        if (totalXP < 0) {
            return 1;
        }
        if (totalXP == 0) {
            return 1;
        }

        // Binary search for the correct level
        int low = 1;
        int high = 1000; // Reasonable max level assumption

        // Expand high if needed
        while (calculateXPForLevel(high) <= totalXP) {
            high *= 2;
        }

        // Binary search
        while (low < high) {
            int mid = (low + high + 1) / 2;
            long xpRequired = calculateXPForLevel(mid);
            if (xpRequired <= totalXP) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    @Override
    public long getXPToNextLevel(int currentLevel, long currentXP) {
        if (currentLevel < 1) {
            throw new IllegalArgumentException("Current level must be at least 1");
        }

        long xpForNextLevel = calculateXPForLevel(currentLevel + 1);
        long remaining = xpForNextLevel - currentXP;
        return Math.max(0, remaining);
    }

    @Override
    public long getXPRequiredForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        return baseXP + (level * increment);
    }

    @Override
    public String getCurveType() {
        return "linear";
    }

    /**
     * Gets the base XP requirement.
     *
     * @return the base XP
     */
    public long getBaseXP() {
        return baseXP;
    }

    /**
     * Gets the XP increment per level.
     *
     * @return the increment
     */
    public long getIncrement() {
        return increment;
    }

    @Override
    public String toString() {
        return "LinearCurve{" +
                "baseXP=" + baseXP +
                ", increment=" + increment +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinearCurve that = (LinearCurve) o;
        return baseXP == that.baseXP && increment == that.increment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseXP, increment);
    }
}
