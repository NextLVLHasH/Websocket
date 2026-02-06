package com.NextLVLHasH.Websockets.rpg.attributes;

/**
 * Enumeration of modifier types that can be applied to attributes.
 * Defines how modifiers are calculated and the order in which they are applied.
 * 
 * <p>Application order:
 * <ol>
 *   <li>FLAT - Added directly to the base value</li>
 *   <li>PERCENTAGE - Calculated as a percentage of the base value and added</li>
 *   <li>MULTIPLICATIVE - Multiplies the final value after all other modifiers</li>
 * </ol>
 * 
 * <p>Example calculation for base value 100 with +20 FLAT, +30% PERCENTAGE, and x1.5 MULTIPLICATIVE:
 * <pre>
 *   Step 1 (FLAT): 100 + 20 = 120
 *   Step 2 (PERCENTAGE): 120 + (100 * 0.30) = 150
 *   Step 3 (MULTIPLICATIVE): 150 * 1.5 = 225
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public enum ModifierType {
    
    /**
     * Adds a fixed value to the attribute.
     * Applied first in the calculation order.
     * 
     * <p>Example: +10 Strength buff adds 10 to the base strength value.
     */
    FLAT(1, "Flat", "Adds a fixed value to the attribute"),
    
    /**
     * Adds a percentage of the base value to the attribute.
     * Applied second, after FLAT modifiers.
     * 
     * <p>Example: +20% bonus adds 20% of the base value.
     * The percentage is calculated from the original base value, not the modified value.
     */
    PERCENTAGE(2, "Percentage", "Adds a percentage of the base value"),
    
    /**
     * Multiplies the final value after all other modifiers.
     * Applied last in the calculation order.
     * 
     * <p>Example: x1.5 multiplier increases the final value by 50%.
     */
    MULTIPLICATIVE(3, "Multiplicative", "Multiplies the final calculated value");

    private final int order;
    private final String displayName;
    private final String description;

    /**
     * Constructs a ModifierType with the specified properties.
     *
     * @param order the application order (lower numbers applied first)
     * @param displayName the human-readable name
     * @param description a brief description of how this modifier works
     */
    ModifierType(int order, String displayName, String description) {
        this.order = order;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the application order for this modifier type.
     * Modifiers with lower order values are applied first.
     *
     * @return the application order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of how this modifier type works.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Formats a value according to this modifier type for display purposes.
     *
     * @param value the modifier value
     * @return formatted string representation (e.g., "+10", "+20%", "x1.5")
     */
    public String formatValue(double value) {
        return switch (this) {
            case FLAT -> {
                if (value >= 0) {
                    yield "+" + (int) value;
                } else {
                    yield String.valueOf((int) value);
                }
            }
            case PERCENTAGE -> {
                int percent = (int) (value * 100);
                if (percent >= 0) {
                    yield "+" + percent + "%";
                } else {
                    yield percent + "%";
                }
            }
            case MULTIPLICATIVE -> "x" + String.format("%.2f", value);
        };
    }
}
