package com.NextLVLHasH.Websockets.rpg.progression;

import java.util.Objects;

/**
 * A polynomial experience curve implementation.
 * <p>
 * Formula: XPRequired(level) = a * Math.pow(level, power) + b * level + c
 * </p>
 * <p>
 * This creates a highly customizable progression curve that can model
 * quadratic, cubic, or higher-order polynomial growth patterns.
 * The polynomial form allows fine-tuning of early, mid, and late-game
 * progression rates.
 * </p>
 * <p>
 * Example with a=10, power=2, b=50, c=0 (quadratic):
 * <ul>
 *   <li>Level 1→2: 10*1^2 + 50*1 + 0 = 60 XP</li>
 *   <li>Level 2→3: 10*2^2 + 50*2 + 0 = 140 XP</li>
 *   <li>Level 3→4: 10*3^2 + 50*3 + 0 = 240 XP</li>
 *   <li>Level 4→5: 10*4^2 + 50*4 + 0 = 360 XP</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class PolynomialCurve implements ExperienceCurve {

    /**
     * Coefficient for the power term.
     */
    private final double a;

    /**
     * The power/exponent for the polynomial term.
     */
    private final double power;

    /**
     * Coefficient for the linear term.
     */
    private final double b;

    /**
     * Constant term.
     */
    private final double c;

    /**
     * Creates a new PolynomialCurve with the specified parameters.
     *
     * @param a the coefficient for the power term (must result in positive XP)
     * @param power the exponent (must be >= 1)
     * @param b the coefficient for the linear term
     * @param c the constant term
     * @throws IllegalArgumentException if power < 1 or if the curve would produce negative XP
     */
    public PolynomialCurve(double a, double power, double b, double c) {
        if (power < 1) {
            throw new IllegalArgumentException("Power must be at least 1");
        }
        
        // Validate that level 1 produces positive XP
        double xpAtLevelOne = a * Math.pow(1, power) + b * 1 + c;
        if (xpAtLevelOne < 1) {
            throw new IllegalArgumentException(
                "Curve parameters must produce at least 1 XP at level 1. " +
                "Current parameters produce: " + xpAtLevelOne);
        }

        this.a = a;
        this.power = power;
        this.b = b;
        this.c = c;
    }

    /**
     * Creates a PolynomialCurve with default quadratic values.
     * Default: 10*level^2 + 50*level + 40
     *
     * @return a new PolynomialCurve with default settings
     */
    public static PolynomialCurve createDefault() {
        return new PolynomialCurve(10, 2, 50, 40);
    }

    /**
     * Creates a quadratic curve with simplified parameters.
     * Formula: a*level^2 + b*level
     *
     * @param a quadratic coefficient
     * @param b linear coefficient
     * @return a new quadratic PolynomialCurve
     */
    public static PolynomialCurve quadratic(double a, double b) {
        return new PolynomialCurve(a, 2, b, 0);
    }

    /**
     * Creates a cubic curve with simplified parameters.
     * Formula: a*level^3 + b*level
     *
     * @param a cubic coefficient
     * @param b linear coefficient
     * @return a new cubic PolynomialCurve
     */
    public static PolynomialCurve cubic(double a, double b) {
        return new PolynomialCurve(a, 3, b, 0);
    }

    @Override
    public long calculateXPForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        if (level == 1) {
            return 0L; // Level 1 requires no XP
        }

        // Calculate cumulative XP by summing XP required for each level
        // This is more accurate than trying to derive a closed form
        long totalXP = 0;
        for (int l = 1; l < level; l++) {
            totalXP += getXPRequiredForLevel(l);
        }
        return totalXP;
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
            if (high > 10000) break; // Safety limit
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
        double xp = a * Math.pow(level, power) + b * level + c;
        return Math.max(1, Math.round(xp));
    }

    @Override
    public String getCurveType() {
        return "polynomial";
    }

    /**
     * Gets the coefficient for the power term.
     *
     * @return coefficient a
     */
    public double getA() {
        return a;
    }

    /**
     * Gets the power/exponent.
     *
     * @return the power
     */
    public double getPower() {
        return power;
    }

    /**
     * Gets the coefficient for the linear term.
     *
     * @return coefficient b
     */
    public double getB() {
        return b;
    }

    /**
     * Gets the constant term.
     *
     * @return constant c
     */
    public double getC() {
        return c;
    }

    @Override
    public String toString() {
        return "PolynomialCurve{" +
                "formula=" + a + "*level^" + power + " + " + b + "*level + " + c +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolynomialCurve that = (PolynomialCurve) o;
        return Double.compare(that.a, a) == 0 &&
               Double.compare(that.power, power) == 0 &&
               Double.compare(that.b, b) == 0 &&
               Double.compare(that.c, c) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, power, b, c);
    }
}
