package com.NextLVLHasH.Websockets.rpg.attributes;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Represents a single attribute with its base value and all active modifiers.
 * Handles the calculation of the final attribute value by applying modifiers
 * in the correct order (FLAT, then PERCENTAGE, then MULTIPLICATIVE).
 * 
 * <p>The instance maintains a cached calculated value that is invalidated when
 * modifiers are added or removed. This optimization prevents recalculation on
 * every access while ensuring the value stays up-to-date.
 * 
 * <p>Thread-safe for concurrent modifier operations.
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class AttributeInstance {

    private final AttributeType type;
    private int baseValue;
    private final List<AttributeModifier> modifiers;
    private int cachedValue;
    private boolean dirty;

    /**
     * Creates a new AttributeInstance with the specified type and default base value of 10.
     *
     * @param type the attribute type
     */
    public AttributeInstance(AttributeType type) {
        this(type, 10);
    }

    /**
     * Creates a new AttributeInstance with the specified type and base value.
     *
     * @param type the attribute type
     * @param baseValue the initial base value
     */
    public AttributeInstance(AttributeType type, int baseValue) {
        this.type = Objects.requireNonNull(type, "Attribute type cannot be null");
        this.baseValue = baseValue;
        this.modifiers = new CopyOnWriteArrayList<>();
        this.cachedValue = baseValue;
        this.dirty = false;
    }

    /**
     * Gets the attribute type.
     *
     * @return the attribute type
     */
    public AttributeType getType() {
        return type;
    }

    /**
     * Gets the base value before any modifiers.
     *
     * @return the base value
     */
    public int getBaseValue() {
        return baseValue;
    }

    /**
     * Sets the base value and marks the cache as dirty.
     *
     * @param baseValue the new base value
     */
    public void setBaseValue(int baseValue) {
        this.baseValue = baseValue;
        this.dirty = true;
    }

    /**
     * Adds a modifier to this attribute.
     * The modifier must target this attribute type.
     *
     * @param mod the modifier to add
     * @throws IllegalArgumentException if the modifier targets a different attribute
     */
    public void addModifier(AttributeModifier mod) {
        Objects.requireNonNull(mod, "Modifier cannot be null");
        if (mod.getAttribute() != this.type) {
            throw new IllegalArgumentException(
                "Modifier targets " + mod.getAttribute() + " but this instance is for " + this.type);
        }
        
        // Remove any existing modifier with the same ID
        modifiers.removeIf(m -> m.getId().equals(mod.getId()));
        modifiers.add(mod);
        dirty = true;
    }

    /**
     * Removes a modifier by its unique ID.
     *
     * @param modifierId the ID of the modifier to remove
     * @return true if a modifier was removed, false otherwise
     */
    public boolean removeModifier(String modifierId) {
        boolean removed = modifiers.removeIf(m -> m.getId().equals(modifierId));
        if (removed) {
            dirty = true;
        }
        return removed;
    }

    /**
     * Removes all modifiers from the specified source.
     *
     * @param source the source to match (e.g., "Equipment: Iron Helm")
     * @return the number of modifiers removed
     */
    public int removeModifiersBySource(String source) {
        int sizeBefore = modifiers.size();
        modifiers.removeIf(m -> m.getSource().equals(source));
        int removed = sizeBefore - modifiers.size();
        if (removed > 0) {
            dirty = true;
        }
        return removed;
    }

    /**
     * Removes all expired modifiers.
     *
     * @return the number of modifiers removed
     */
    public int clearExpiredModifiers() {
        int sizeBefore = modifiers.size();
        modifiers.removeIf(AttributeModifier::isExpired);
        int removed = sizeBefore - modifiers.size();
        if (removed > 0) {
            dirty = true;
        }
        return removed;
    }

    /**
     * Checks if there are any expired modifiers that need cleaning.
     *
     * @return true if there are expired modifiers
     */
    public boolean hasExpiredModifiers() {
        return modifiers.stream().anyMatch(AttributeModifier::isExpired);
    }

    /**
     * Calculates the final attribute value by applying all modifiers.
     * Modifiers are applied in order: FLAT, then PERCENTAGE, then MULTIPLICATIVE.
     * 
     * <p>Calculation steps:
     * <ol>
     *   <li>Start with base value</li>
     *   <li>Add all FLAT modifier values</li>
     *   <li>Add (base * sum of PERCENTAGE values)</li>
     *   <li>Multiply by all MULTIPLICATIVE values</li>
     * </ol>
     *
     * @return the calculated final value
     */
    public int calculate() {
        // Start with base value
        double result = baseValue;
        
        // Sort modifiers by type order, priority, and creation time
        List<AttributeModifier> activeModifiers = modifiers.stream()
                .filter(m -> !m.isExpired())
                .sorted()
                .toList();
        
        // Calculate sums for each modifier type
        double flatSum = 0;
        double percentageSum = 0;
        double multiplicativeProduct = 1.0;
        
        for (AttributeModifier mod : activeModifiers) {
            switch (mod.getType()) {
                case FLAT -> flatSum += mod.getValue();
                case PERCENTAGE -> percentageSum += mod.getValue();
                case MULTIPLICATIVE -> multiplicativeProduct *= mod.getValue();
            }
        }
        
        // Apply in order: FLAT, then PERCENTAGE, then MULTIPLICATIVE
        result += flatSum;
        result += baseValue * percentageSum;
        result *= multiplicativeProduct;
        
        // Round to nearest integer and ensure non-negative
        cachedValue = Math.max(0, (int) Math.round(result));
        dirty = false;
        
        return cachedValue;
    }

    /**
     * Gets the current attribute value.
     * Returns the cached value if valid, otherwise recalculates.
     *
     * @return the current calculated value
     */
    public int getValue() {
        // Check for expired modifiers that would invalidate cache
        if (hasExpiredModifiers()) {
            dirty = true;
        }
        
        if (dirty) {
            return calculate();
        }
        return cachedValue;
    }

    /**
     * Forces a recalculation of the value on next access.
     */
    public void invalidate() {
        dirty = true;
    }

    /**
     * Gets all active (non-expired) modifiers.
     *
     * @return unmodifiable list of active modifiers, sorted by application order
     */
    public List<AttributeModifier> getActiveModifiers() {
        return modifiers.stream()
                .filter(m -> !m.isExpired())
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets all modifiers including expired ones.
     *
     * @return unmodifiable list of all modifiers
     */
    public List<AttributeModifier> getAllModifiers() {
        return Collections.unmodifiableList(new ArrayList<>(modifiers));
    }

    /**
     * Gets the total number of modifiers (including expired).
     *
     * @return the modifier count
     */
    public int getModifierCount() {
        return modifiers.size();
    }

    /**
     * Gets the number of active (non-expired) modifiers.
     *
     * @return the active modifier count
     */
    public int getActiveModifierCount() {
        return (int) modifiers.stream().filter(m -> !m.isExpired()).count();
    }

    /**
     * Clears all modifiers from this attribute.
     */
    public void clearAllModifiers() {
        modifiers.clear();
        dirty = true;
    }

    /**
     * Gets a modifier by its ID.
     *
     * @param modifierId the ID to search for
     * @return Optional containing the modifier if found
     */
    public Optional<AttributeModifier> getModifier(String modifierId) {
        return modifiers.stream()
                .filter(m -> m.getId().equals(modifierId))
                .findFirst();
    }

    /**
     * Checks if this attribute has any modifiers from the specified source.
     *
     * @param source the source to check for
     * @return true if there are modifiers from this source
     */
    public boolean hasModifiersFromSource(String source) {
        return modifiers.stream().anyMatch(m -> m.getSource().equals(source));
    }

    /**
     * Gets all modifiers from the specified source.
     *
     * @param source the source to filter by
     * @return list of modifiers from this source
     */
    public List<AttributeModifier> getModifiersBySource(String source) {
        return modifiers.stream()
                .filter(m -> m.getSource().equals(source))
                .collect(Collectors.toList());
    }

    /**
     * Gets the total flat bonus from all active modifiers.
     *
     * @return the sum of all FLAT modifier values
     */
    public double getTotalFlatBonus() {
        return modifiers.stream()
                .filter(m -> !m.isExpired() && m.getType() == ModifierType.FLAT)
                .mapToDouble(AttributeModifier::getValue)
                .sum();
    }

    /**
     * Gets the total percentage bonus from all active modifiers.
     *
     * @return the sum of all PERCENTAGE modifier values
     */
    public double getTotalPercentageBonus() {
        return modifiers.stream()
                .filter(m -> !m.isExpired() && m.getType() == ModifierType.PERCENTAGE)
                .mapToDouble(AttributeModifier::getValue)
                .sum();
    }

    /**
     * Gets the total multiplicative factor from all active modifiers.
     *
     * @return the product of all MULTIPLICATIVE modifier values
     */
    public double getTotalMultiplicativeFactor() {
        return modifiers.stream()
                .filter(m -> !m.isExpired() && m.getType() == ModifierType.MULTIPLICATIVE)
                .mapToDouble(AttributeModifier::getValue)
                .reduce(1.0, (a, b) -> a * b);
    }

    /**
     * Creates a summary of all bonuses for display.
     *
     * @return formatted string showing base value and all bonuses
     */
    public String getValueBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getDisplayName()).append(": ").append(baseValue).append(" (base)");
        
        double flat = getTotalFlatBonus();
        if (flat != 0) {
            sb.append(" + ").append((int) flat).append(" (flat)");
        }
        
        double percentage = getTotalPercentageBonus();
        if (percentage != 0) {
            sb.append(" + ").append((int) (baseValue * percentage)).append(" (").append((int) (percentage * 100)).append("%)");
        }
        
        double mult = getTotalMultiplicativeFactor();
        if (mult != 1.0) {
            sb.append(" × ").append(String.format("%.2f", mult));
        }
        
        sb.append(" = ").append(getValue());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AttributeInstance{" +
                "type=" + type +
                ", baseValue=" + baseValue +
                ", value=" + getValue() +
                ", modifiers=" + getActiveModifierCount() +
                '}';
    }
}
