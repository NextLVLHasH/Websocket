package com.NextLVLHasH.Websockets.rpg.progression;

import java.util.Objects;

/**
 * An exponential experience curve implementation.
 * <p>
 * Formula: XPRequired(level) = baseXP * Math.pow(multiplier, level - 1)
 * </p>
 * <p>
 * This creates accelerating progression where each level requires
 * exponentially more XP than the previous level, creating a scaling
 * challenge curve typical of many RPGs.
 * </p>
 * <p>
 * Example with baseXP=100 and multiplier=1.5:
 * <ul>
 *   <li>Level 1→2: 100 * 1.5^0 = 100 XP</li>
 *   <li>Level 2→3: 100 * 1.5^1 = 150 XP</li>
 *   <li>Level 3→4: 100 * 1.5^2 = 225 XP</li>
 *   <li>Level 4→5: 100 * 1.5^3 = 337 XP</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class ExponentialCurve implements ExperienceCurve {

    /**
     * The base XP requirement for the first level-up.
     */
    private final long baseXP;

    /**
     * The multiplier applied exponentially per level.
     */
    private final double multiplier;

    /**
     * Creates a new ExponentialCurve with the specified parameters.
     *
     * @param baseXP the base XP requirement (must be > 0)
     * @param multiplier the exponential multiplier (must be >= 1.0)
     * @throws IllegalArgumentException if baseXP <= 0 or multiplier < 1.0
     */
    public ExponentialCurve(long baseXP, double multiplier) {
        if (baseXP <= 0) {
            throw new IllegalArgumentException("Base XP must be greater than 0");
        }
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("Multiplier must be at least 1.0");
        }
        this.baseXP = baseXP;
        this.multiplier = multiplier;
    }

    /**
     * Creates an ExponentialCurve with default values (baseXP=100, multiplier=1.5).
     *
     * @return a new ExponentialCurve with default settings
     */
    public static ExponentialCurve createDefault() {
        return new ExponentialCurve(100, 1.5);
    }

    @Override
    public long calculateXPForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        if (level == 1) {
            return 0L; // Level 1 requires no XP
        }

        // Sum of geometric series: baseXP * (1 + r + r^2 + ... + r^(n-2))
        // where n = level and r = multiplier
        // = baseXP * (1 - r^(n-1)) / (1 - r) when r != 1
        // = baseXP * (n-1) when r = 1
        
        if (Math.abs(multiplier - 1.0) < 0.0001) {
            // Linear case when multiplier is essentially 1
            return baseXP * (level - 1);
        }

        double geometricSum = (1.0 - Math.pow(multiplier, level - 1)) / (1.0 - multiplier);
        return Math.round(baseXP * geometricSum);
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
            if (high > 10000) break; // Safety limit for exponential curves
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
        return Math.round(baseXP * Math.pow(multiplier, level - 1));
    }

    @Override
    public String getCurveType() {
        return "exponential";
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
     * Gets the exponential multiplier.
     *
     * @return the multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public String toString() {
        return "ExponentialCurve{" +
                "baseXP=" + baseXP +
                ", multiplier=" + multiplier +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExponentialCurve that = (ExponentialCurve) o;
        return baseXP == that.baseXP && Double.compare(that.multiplier, multiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseXP, multiplier);
    }
}
