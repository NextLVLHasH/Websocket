package com.NextLVLHasH.Websockets.rpg.character;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.*;

/**
 * Defines equipment restrictions and requirements for a character class.
 * <p>
 * This class specifies which weapon types and armor types a class can use,
 * as well as minimum attribute requirements for certain equipment.
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class EquipmentRestriction {

    /**
     * List of weapon types this class is allowed to use.
     * An empty list means all weapon types are allowed.
     */
    @SerializedName("allowed_weapon_types")
    private final List<String> allowedWeaponTypes;

    /**
     * List of armor types this class is allowed to use.
     * An empty list means all armor types are allowed.
     */
    @SerializedName("allowed_armor_types")
    private final List<String> allowedArmorTypes;

    /**
     * Minimum attribute requirements for using specific equipment.
     * Maps AttributeType to the minimum value required.
     */
    @SerializedName("minimum_attributes")
    private final Map<AttributeType, Integer> minimumAttributes;

    // Gson instance for JSON operations
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Creates a new EquipmentRestriction with the specified parameters.
     *
     * @param allowedWeaponTypes list of allowed weapon types
     * @param allowedArmorTypes  list of allowed armor types
     * @param minimumAttributes  minimum attribute requirements
     */
    public EquipmentRestriction(
            List<String> allowedWeaponTypes,
            List<String> allowedArmorTypes,
            Map<AttributeType, Integer> minimumAttributes) {
        this.allowedWeaponTypes = allowedWeaponTypes != null 
                ? new ArrayList<>(allowedWeaponTypes) 
                : new ArrayList<>();
        this.allowedArmorTypes = allowedArmorTypes != null 
                ? new ArrayList<>(allowedArmorTypes) 
                : new ArrayList<>();
        this.minimumAttributes = minimumAttributes != null 
                ? new EnumMap<>(minimumAttributes) 
                : new EnumMap<>(AttributeType.class);
    }

    /**
     * Creates an empty EquipmentRestriction with no restrictions.
     */
    public EquipmentRestriction() {
        this(null, null, null);
    }

    // ==================== Weapon Checks ====================

    /**
     * Checks if the specified weapon type can be used by this class.
     *
     * @param weaponType the weapon type to check (e.g., "sword", "bow", "staff")
     * @return true if the weapon type is allowed, false otherwise
     */
    public boolean canUseWeapon(String weaponType) {
        if (weaponType == null || weaponType.isBlank()) {
            return false;
        }
        // Empty list means all weapons are allowed
        if (allowedWeaponTypes.isEmpty()) {
            return true;
        }
        return allowedWeaponTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(weaponType.trim()));
    }

    /**
     * Gets the list of allowed weapon types.
     *
     * @return unmodifiable list of allowed weapon types
     */
    public List<String> getAllowedWeaponTypes() {
        return Collections.unmodifiableList(allowedWeaponTypes);
    }

    /**
     * Checks if any weapon restrictions exist.
     *
     * @return true if there are weapon restrictions, false if all weapons are allowed
     */
    public boolean hasWeaponRestrictions() {
        return !allowedWeaponTypes.isEmpty();
    }

    // ==================== Armor Checks ====================

    /**
     * Checks if the specified armor type can be used by this class.
     *
     * @param armorType the armor type to check (e.g., "light", "medium", "heavy")
     * @return true if the armor type is allowed, false otherwise
     */
    public boolean canUseArmor(String armorType) {
        if (armorType == null || armorType.isBlank()) {
            return false;
        }
        // Empty list means all armor types are allowed
        if (allowedArmorTypes.isEmpty()) {
            return true;
        }
        return allowedArmorTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(armorType.trim()));
    }

    /**
     * Gets the list of allowed armor types.
     *
     * @return unmodifiable list of allowed armor types
     */
    public List<String> getAllowedArmorTypes() {
        return Collections.unmodifiableList(allowedArmorTypes);
    }

    /**
     * Checks if any armor restrictions exist.
     *
     * @return true if there are armor restrictions, false if all armor types are allowed
     */
    public boolean hasArmorRestrictions() {
        return !allowedArmorTypes.isEmpty();
    }

    // ==================== Attribute Requirement Checks ====================

    /**
     * Checks if the player's attributes meet all minimum requirements.
     *
     * @param playerAttributes map of the player's current attribute values
     * @return true if all minimum attribute requirements are met
     */
    public boolean meetsAttributeRequirements(Map<AttributeType, Integer> playerAttributes) {
        if (playerAttributes == null) {
            return minimumAttributes.isEmpty();
        }
        
        for (Map.Entry<AttributeType, Integer> requirement : minimumAttributes.entrySet()) {
            AttributeType type = requirement.getKey();
            int requiredValue = requirement.getValue();
            int playerValue = playerAttributes.getOrDefault(type, 0);
            
            if (playerValue < requiredValue) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the minimum attribute requirements.
     *
     * @return unmodifiable map of minimum attribute requirements
     */
    public Map<AttributeType, Integer> getMinimumAttributes() {
        return Collections.unmodifiableMap(minimumAttributes);
    }

    /**
     * Gets the minimum value required for a specific attribute.
     *
     * @param type the attribute type
     * @return the minimum required value, or 0 if no requirement exists
     */
    public int getMinimumAttribute(AttributeType type) {
        return minimumAttributes.getOrDefault(type, 0);
    }

    /**
     * Gets a list of unmet attribute requirements for the given player attributes.
     *
     * @param playerAttributes the player's current attributes
     * @return list of attribute types that don't meet requirements
     */
    public List<AttributeType> getUnmetRequirements(Map<AttributeType, Integer> playerAttributes) {
        List<AttributeType> unmet = new ArrayList<>();
        if (playerAttributes == null) {
            unmet.addAll(minimumAttributes.keySet());
            return unmet;
        }
        
        for (Map.Entry<AttributeType, Integer> requirement : minimumAttributes.entrySet()) {
            AttributeType type = requirement.getKey();
            int requiredValue = requirement.getValue();
            int playerValue = playerAttributes.getOrDefault(type, 0);
            
            if (playerValue < requiredValue) {
                unmet.add(type);
            }
        }
        return unmet;
    }

    // ==================== JSON Serialization ====================

    /**
     * Serializes this EquipmentRestriction to a JSON string.
     *
     * @return JSON string representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserializes an EquipmentRestriction from a JSON string.
     *
     * @param json the JSON string
     * @return the deserialized EquipmentRestriction
     */
    public static EquipmentRestriction fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new EquipmentRestriction();
        }
        return GSON.fromJson(json, EquipmentRestriction.class);
    }

    // ==================== Builder Pattern ====================

    /**
     * Creates a new Builder for constructing EquipmentRestriction instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing EquipmentRestriction instances.
     */
    public static class Builder {
        private final List<String> allowedWeaponTypes = new ArrayList<>();
        private final List<String> allowedArmorTypes = new ArrayList<>();
        private final Map<AttributeType, Integer> minimumAttributes = new EnumMap<>(AttributeType.class);

        /**
         * Adds an allowed weapon type.
         *
         * @param weaponType the weapon type to allow
         * @return this builder
         */
        public Builder allowWeapon(String weaponType) {
            if (weaponType != null && !weaponType.isBlank()) {
                allowedWeaponTypes.add(weaponType.trim().toLowerCase());
            }
            return this;
        }

        /**
         * Adds multiple allowed weapon types.
         *
         * @param weaponTypes the weapon types to allow
         * @return this builder
         */
        public Builder allowWeapons(String... weaponTypes) {
            for (String type : weaponTypes) {
                allowWeapon(type);
            }
            return this;
        }

        /**
         * Adds multiple allowed weapon types from a collection.
         *
         * @param weaponTypes the weapon types to allow
         * @return this builder
         */
        public Builder allowWeapons(Collection<String> weaponTypes) {
            if (weaponTypes != null) {
                weaponTypes.forEach(this::allowWeapon);
            }
            return this;
        }

        /**
         * Adds an allowed armor type.
         *
         * @param armorType the armor type to allow
         * @return this builder
         */
        public Builder allowArmor(String armorType) {
            if (armorType != null && !armorType.isBlank()) {
                allowedArmorTypes.add(armorType.trim().toLowerCase());
            }
            return this;
        }

        /**
         * Adds multiple allowed armor types.
         *
         * @param armorTypes the armor types to allow
         * @return this builder
         */
        public Builder allowArmors(String... armorTypes) {
            for (String type : armorTypes) {
                allowArmor(type);
            }
            return this;
        }

        /**
         * Adds multiple allowed armor types from a collection.
         *
         * @param armorTypes the armor types to allow
         * @return this builder
         */
        public Builder allowArmors(Collection<String> armorTypes) {
            if (armorTypes != null) {
                armorTypes.forEach(this::allowArmor);
            }
            return this;
        }

        /**
         * Sets a minimum attribute requirement.
         *
         * @param type     the attribute type
         * @param minValue the minimum value required
         * @return this builder
         */
        public Builder requireAttribute(AttributeType type, int minValue) {
            if (type != null && minValue > 0) {
                minimumAttributes.put(type, minValue);
            }
            return this;
        }

        /**
         * Sets minimum attribute requirements from a map.
         *
         * @param requirements map of attribute requirements
         * @return this builder
         */
        public Builder requireAttributes(Map<AttributeType, Integer> requirements) {
            if (requirements != null) {
                requirements.forEach(this::requireAttribute);
            }
            return this;
        }

        /**
         * Builds the EquipmentRestriction instance.
         *
         * @return the constructed EquipmentRestriction
         */
        public EquipmentRestriction build() {
            return new EquipmentRestriction(allowedWeaponTypes, allowedArmorTypes, minimumAttributes);
        }
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        return "EquipmentRestriction{" +
                "allowedWeaponTypes=" + allowedWeaponTypes +
                ", allowedArmorTypes=" + allowedArmorTypes +
                ", minimumAttributes=" + minimumAttributes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquipmentRestriction that = (EquipmentRestriction) o;
        return Objects.equals(allowedWeaponTypes, that.allowedWeaponTypes) &&
                Objects.equals(allowedArmorTypes, that.allowedArmorTypes) &&
                Objects.equals(minimumAttributes, that.minimumAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedWeaponTypes, allowedArmorTypes, minimumAttributes);
    }
}
