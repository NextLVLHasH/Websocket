package com.NextLVLHasH.Websockets.rpg.attributes;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.*;

/**
 * Container for all attributes of a character.
 * Manages the six core attributes (STRENGTH, DEXTERITY, INTELLIGENCE,
 * CONSTITUTION, WISDOM, CHARISMA) and provides methods for applying
 * and removing modifiers across attributes.
 * 
 * <p>This class serves as the primary interface for managing a character's
 * attributes and their modifiers. All six attributes are initialized
 * automatically with configurable base values.
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class AttributeSet {

    private static final int DEFAULT_BASE_VALUE = 10;
    
    private final Map<AttributeType, AttributeInstance> attributes;
    private final List<AttributeModifier> allModifiers;

    /**
     * Creates a new AttributeSet with all attributes initialized to the default base value (10).
     */
    public AttributeSet() {
        this(DEFAULT_BASE_VALUE);
    }

    /**
     * Creates a new AttributeSet with all attributes initialized to the specified base value.
     *
     * @param defaultBaseValue the initial base value for all attributes
     */
    public AttributeSet(int defaultBaseValue) {
        this.attributes = new EnumMap<>(AttributeType.class);
        this.allModifiers = new ArrayList<>();
        
        // Initialize all six attributes
        for (AttributeType type : AttributeType.values()) {
            attributes.put(type, new AttributeInstance(type, defaultBaseValue));
        }
    }

    /**
     * Creates a new AttributeSet with specified base values for each attribute.
     *
     * @param baseValues map of attribute types to their base values
     */
    public AttributeSet(Map<AttributeType, Integer> baseValues) {
        this.attributes = new EnumMap<>(AttributeType.class);
        this.allModifiers = new ArrayList<>();
        
        // Initialize all attributes with provided or default values
        for (AttributeType type : AttributeType.values()) {
            int baseValue = baseValues.getOrDefault(type, DEFAULT_BASE_VALUE);
            attributes.put(type, new AttributeInstance(type, baseValue));
        }
    }

    /**
     * Gets the AttributeInstance for the specified type.
     *
     * @param type the attribute type
     * @return the AttributeInstance for this type
     */
    public AttributeInstance getAttribute(AttributeType type) {
        return attributes.get(type);
    }

    /**
     * Sets the base value for an attribute.
     *
     * @param type the attribute type
     * @param value the new base value
     */
    public void setBaseValue(AttributeType type, int value) {
        AttributeInstance instance = attributes.get(type);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /**
     * Gets the base value for an attribute (before modifiers).
     *
     * @param type the attribute type
     * @return the base value
     */
    public int getBaseValue(AttributeType type) {
        AttributeInstance instance = attributes.get(type);
        return instance != null ? instance.getBaseValue() : 0;
    }

    /**
     * Gets the calculated value for an attribute (with all modifiers applied).
     *
     * @param type the attribute type
     * @return the calculated value including all modifiers
     */
    public int getValue(AttributeType type) {
        AttributeInstance instance = attributes.get(type);
        return instance != null ? instance.getValue() : 0;
    }

    /**
     * Adds a modifier to the appropriate attribute.
     *
     * @param mod the modifier to add
     */
    public void addModifier(AttributeModifier mod) {
        Objects.requireNonNull(mod, "Modifier cannot be null");
        
        AttributeInstance instance = attributes.get(mod.getAttribute());
        if (instance != null) {
            // Remove existing modifier with same ID first
            allModifiers.removeIf(m -> m.getId().equals(mod.getId()));
            
            instance.addModifier(mod);
            allModifiers.add(mod);
        }
    }

    /**
     * Adds multiple modifiers at once.
     *
     * @param modifiers the collection of modifiers to add
     */
    public void addModifiers(Collection<AttributeModifier> modifiers) {
        for (AttributeModifier mod : modifiers) {
            addModifier(mod);
        }
    }

    /**
     * Removes a modifier by its ID from all attributes.
     *
     * @param modifierId the ID of the modifier to remove
     * @return true if the modifier was found and removed
     */
    public boolean removeModifier(String modifierId) {
        boolean removed = allModifiers.removeIf(m -> m.getId().equals(modifierId));
        
        for (AttributeInstance instance : attributes.values()) {
            instance.removeModifier(modifierId);
        }
        
        return removed;
    }

    /**
     * Removes all modifiers from a specific source.
     *
     * @param source the source to match (e.g., "Equipment: Iron Helm")
     * @return the total number of modifiers removed
     */
    public int removeModifiersBySource(String source) {
        int removed = 0;
        allModifiers.removeIf(m -> m.getSource().equals(source));
        
        for (AttributeInstance instance : attributes.values()) {
            removed += instance.removeModifiersBySource(source);
        }
        
        return removed;
    }

    /**
     * Clears all expired modifiers from all attributes.
     *
     * @return the total number of expired modifiers removed
     */
    public int clearExpiredModifiers() {
        int removed = 0;
        allModifiers.removeIf(AttributeModifier::isExpired);
        
        for (AttributeInstance instance : attributes.values()) {
            removed += instance.clearExpiredModifiers();
        }
        
        return removed;
    }

    /**
     * Clears all modifiers from all attributes.
     */
    public void clearAllModifiers() {
        allModifiers.clear();
        for (AttributeInstance instance : attributes.values()) {
            instance.clearAllModifiers();
        }
    }

    /**
     * Gets a map of all calculated attribute values.
     *
     * @return map of attribute types to their calculated values
     */
    public Map<AttributeType, Integer> getAllValues() {
        Map<AttributeType, Integer> values = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            values.put(type, getValue(type));
        }
        return values;
    }

    /**
     * Gets a map of all base attribute values.
     *
     * @return map of attribute types to their base values
     */
    public Map<AttributeType, Integer> getAllBaseValues() {
        Map<AttributeType, Integer> values = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            values.put(type, getBaseValue(type));
        }
        return values;
    }

    /**
     * Forces recalculation of all attributes on next access.
     */
    public void recalculateAll() {
        for (AttributeInstance instance : attributes.values()) {
            instance.invalidate();
        }
    }

    /**
     * Gets all active modifiers across all attributes.
     *
     * @return list of all active modifiers
     */
    public List<AttributeModifier> getAllActiveModifiers() {
        return allModifiers.stream()
                .filter(m -> !m.isExpired())
                .sorted()
                .toList();
    }

    /**
     * Gets all modifiers for a specific attribute type.
     *
     * @param type the attribute type
     * @return list of modifiers for this attribute
     */
    public List<AttributeModifier> getModifiersForAttribute(AttributeType type) {
        AttributeInstance instance = attributes.get(type);
        return instance != null ? instance.getActiveModifiers() : Collections.emptyList();
    }

    /**
     * Checks if there are any expired modifiers that need cleaning.
     *
     * @return true if there are expired modifiers
     */
    public boolean hasExpiredModifiers() {
        return allModifiers.stream().anyMatch(AttributeModifier::isExpired);
    }

    /**
     * Gets the total number of active modifiers across all attributes.
     *
     * @return the count of active modifiers
     */
    public int getActiveModifierCount() {
        return (int) allModifiers.stream().filter(m -> !m.isExpired()).count();
    }

    /**
     * Gets a modifier by its ID.
     *
     * @param modifierId the ID to search for
     * @return Optional containing the modifier if found
     */
    public Optional<AttributeModifier> getModifier(String modifierId) {
        return allModifiers.stream()
                .filter(m -> m.getId().equals(modifierId))
                .findFirst();
    }

    /**
     * Checks if a modifier with the given ID exists.
     *
     * @param modifierId the ID to check
     * @return true if the modifier exists
     */
    public boolean hasModifier(String modifierId) {
        return allModifiers.stream().anyMatch(m -> m.getId().equals(modifierId));
    }

    /**
     * Gets a summary of all attribute values.
     *
     * @return formatted string with all attribute values
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder("Attributes:\n");
        for (AttributeType type : AttributeType.values()) {
            AttributeInstance instance = attributes.get(type);
            sb.append("  ").append(type.getDisplayName()).append(": ");
            sb.append(instance.getValue());
            
            int base = instance.getBaseValue();
            int calculated = instance.getValue();
            if (calculated != base) {
                int diff = calculated - base;
                sb.append(" (").append(base);
                sb.append(diff >= 0 ? " +" : " ");
                sb.append(diff).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Gets a detailed breakdown of a specific attribute.
     *
     * @param type the attribute type
     * @return detailed breakdown string
     */
    public String getAttributeBreakdown(AttributeType type) {
        AttributeInstance instance = attributes.get(type);
        return instance != null ? instance.getValueBreakdown() : type + ": N/A";
    }

    /**
     * Creates a snapshot of current values for comparison or rollback.
     *
     * @return a new AttributeSet with the same values but no modifiers
     */
    public AttributeSet createSnapshot() {
        Map<AttributeType, Integer> currentValues = new EnumMap<>(AttributeType.class);
        for (AttributeType type : AttributeType.values()) {
            currentValues.put(type, getValue(type));
        }
        return new AttributeSet(currentValues);
    }

    /**
     * Copies base values from another AttributeSet.
     *
     * @param other the source AttributeSet
     */
    public void copyBaseValuesFrom(AttributeSet other) {
        for (AttributeType type : AttributeType.values()) {
            setBaseValue(type, other.getBaseValue(type));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AttributeSet{");
        for (AttributeType type : AttributeType.values()) {
            sb.append(type.name()).append("=").append(getValue(type));
            if (type != AttributeType.CHARISMA) {
                sb.append(", ");
            }
        }
        sb.append(", modifiers=").append(getActiveModifierCount());
        sb.append("}");
        return sb.toString();
    }
}
