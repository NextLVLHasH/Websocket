package com.NextLVLHasH.Websockets.rpg.restrictions;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.*;

/**
 * Represents the requirements that must be met to equip or use an item.
 * Uses the builder pattern for flexible construction.
 */
public class ItemRequirement {

    private final int requiredLevel;
    private final Map<AttributeType, Integer> requiredAttributes;
    private final List<String> allowedClasses;  // Empty = all classes allowed
    private final List<String> allowedRaces;    // Empty = all races allowed
    private final List<String> requiredSkills;
    private final WeaponType weaponType;        // null if not a weapon
    private final ArmorType armorType;          // null if not armor
    private final String itemId;
    private final String itemName;

    /**
     * Private constructor - use Builder to create instances.
     */
    private ItemRequirement(Builder builder) {
        this.requiredLevel = builder.requiredLevel;
        this.requiredAttributes = Collections.unmodifiableMap(new EnumMap<>(builder.requiredAttributes));
        this.allowedClasses = Collections.unmodifiableList(new ArrayList<>(builder.allowedClasses));
        this.allowedRaces = Collections.unmodifiableList(new ArrayList<>(builder.allowedRaces));
        this.requiredSkills = Collections.unmodifiableList(new ArrayList<>(builder.requiredSkills));
        this.weaponType = builder.weaponType;
        this.armorType = builder.armorType;
        this.itemId = builder.itemId;
        this.itemName = builder.itemName;
    }

    // ==================== Getters ====================

    /**
     * Gets the minimum level required to use this item.
     *
     * @return the required level (0 means no level requirement)
     */
    public int getRequiredLevel() {
        return requiredLevel;
    }

    /**
     * Gets the attribute requirements for this item.
     *
     * @return unmodifiable map of required attributes
     */
    public Map<AttributeType, Integer> getRequiredAttributes() {
        return requiredAttributes;
    }

    /**
     * Gets the list of class IDs allowed to use this item.
     * An empty list means all classes are allowed.
     *
     * @return unmodifiable list of allowed class IDs
     */
    public List<String> getAllowedClasses() {
        return allowedClasses;
    }

    /**
     * Gets the list of race IDs allowed to use this item.
     * An empty list means all races are allowed.
     *
     * @return unmodifiable list of allowed race IDs
     */
    public List<String> getAllowedRaces() {
        return allowedRaces;
    }

    /**
     * Gets the list of skill IDs required to use this item.
     *
     * @return unmodifiable list of required skill IDs
     */
    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    /**
     * Gets the weapon type if this is a weapon.
     *
     * @return the WeaponType, or null if not a weapon
     */
    public WeaponType getWeaponType() {
        return weaponType;
    }

    /**
     * Gets the armor type if this is armor.
     *
     * @return the ArmorType, or null if not armor
     */
    public ArmorType getArmorType() {
        return armorType;
    }

    /**
     * Gets the item identifier.
     *
     * @return the item ID
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Gets the item display name.
     *
     * @return the item name
     */
    public String getItemName() {
        return itemName;
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if this item is a weapon.
     *
     * @return true if this is a weapon
     */
    public boolean isWeapon() {
        return weaponType != null;
    }

    /**
     * Checks if this item is armor.
     *
     * @return true if this is armor
     */
    public boolean isArmor() {
        return armorType != null;
    }

    /**
     * Checks if this item has any class restrictions.
     *
     * @return true if class-restricted
     */
    public boolean hasClassRestriction() {
        return !allowedClasses.isEmpty();
    }

    /**
     * Checks if this item has any race restrictions.
     *
     * @return true if race-restricted
     */
    public boolean hasRaceRestriction() {
        return !allowedRaces.isEmpty();
    }

    /**
     * Checks if this item requires specific skills.
     *
     * @return true if skill requirements exist
     */
    public boolean hasSkillRequirements() {
        return !requiredSkills.isEmpty();
    }

    /**
     * Checks if the specified class ID is allowed to use this item.
     *
     * @param classId the class ID to check
     * @return true if allowed (or if no class restriction exists)
     */
    public boolean isClassAllowed(String classId) {
        if (allowedClasses.isEmpty()) {
            return true;
        }
        return classId != null && allowedClasses.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(classId));
    }

    /**
     * Checks if the specified race ID is allowed to use this item.
     *
     * @param raceId the race ID to check
     * @return true if allowed (or if no race restriction exists)
     */
    public boolean isRaceAllowed(String raceId) {
        if (allowedRaces.isEmpty()) {
            return true;
        }
        return raceId != null && allowedRaces.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(raceId));
    }

    /**
     * Creates a new Builder for constructing ItemRequirement instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy of this requirement with a new builder pre-populated.
     *
     * @return a Builder with this requirement's values
     */
    public Builder toBuilder() {
        return new Builder()
                .itemId(this.itemId)
                .itemName(this.itemName)
                .requiredLevel(this.requiredLevel)
                .requiredAttributes(this.requiredAttributes)
                .allowedClasses(this.allowedClasses)
                .allowedRaces(this.allowedRaces)
                .requiredSkills(this.requiredSkills)
                .weaponType(this.weaponType)
                .armorType(this.armorType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ItemRequirement{");
        if (itemId != null) sb.append("id='").append(itemId).append("', ");
        if (itemName != null) sb.append("name='").append(itemName).append("', ");
        if (requiredLevel > 0) sb.append("level=").append(requiredLevel).append(", ");
        if (!requiredAttributes.isEmpty()) sb.append("attributes=").append(requiredAttributes).append(", ");
        if (!allowedClasses.isEmpty()) sb.append("classes=").append(allowedClasses).append(", ");
        if (!allowedRaces.isEmpty()) sb.append("races=").append(allowedRaces).append(", ");
        if (!requiredSkills.isEmpty()) sb.append("skills=").append(requiredSkills).append(", ");
        if (weaponType != null) sb.append("weapon=").append(weaponType).append(", ");
        if (armorType != null) sb.append("armor=").append(armorType).append(", ");
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    // ==================== Builder ====================

    /**
     * Builder class for constructing ItemRequirement instances.
     */
    public static class Builder {
        private int requiredLevel = 0;
        private final Map<AttributeType, Integer> requiredAttributes = new EnumMap<>(AttributeType.class);
        private final List<String> allowedClasses = new ArrayList<>();
        private final List<String> allowedRaces = new ArrayList<>();
        private final List<String> requiredSkills = new ArrayList<>();
        private WeaponType weaponType = null;
        private ArmorType armorType = null;
        private String itemId = null;
        private String itemName = null;

        /**
         * Sets the item identifier.
         *
         * @param itemId the item ID
         * @return this builder
         */
        public Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        /**
         * Sets the item display name.
         *
         * @param itemName the item name
         * @return this builder
         */
        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        /**
         * Sets the required level.
         *
         * @param level the minimum level required
         * @return this builder
         */
        public Builder requiredLevel(int level) {
            this.requiredLevel = Math.max(0, level);
            return this;
        }

        /**
         * Adds an attribute requirement.
         *
         * @param attribute the attribute type
         * @param value     the minimum value required
         * @return this builder
         */
        public Builder requireAttribute(AttributeType attribute, int value) {
            if (attribute != null && value > 0) {
                this.requiredAttributes.put(attribute, value);
            }
            return this;
        }

        /**
         * Sets all attribute requirements from a map.
         *
         * @param attributes map of attribute requirements
         * @return this builder
         */
        public Builder requiredAttributes(Map<AttributeType, Integer> attributes) {
            this.requiredAttributes.clear();
            if (attributes != null) {
                attributes.forEach((k, v) -> {
                    if (k != null && v > 0) {
                        this.requiredAttributes.put(k, v);
                    }
                });
            }
            return this;
        }

        /**
         * Adds an allowed class.
         *
         * @param classId the class ID to allow
         * @return this builder
         */
        public Builder allowClass(String classId) {
            if (classId != null && !classId.isBlank()) {
                this.allowedClasses.add(classId.trim());
            }
            return this;
        }

        /**
         * Sets all allowed classes from a collection.
         *
         * @param classes collection of allowed class IDs
         * @return this builder
         */
        public Builder allowedClasses(Collection<String> classes) {
            this.allowedClasses.clear();
            if (classes != null) {
                classes.stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(String::trim)
                        .forEach(this.allowedClasses::add);
            }
            return this;
        }

        /**
         * Adds an allowed race.
         *
         * @param raceId the race ID to allow
         * @return this builder
         */
        public Builder allowRace(String raceId) {
            if (raceId != null && !raceId.isBlank()) {
                this.allowedRaces.add(raceId.trim());
            }
            return this;
        }

        /**
         * Sets all allowed races from a collection.
         *
         * @param races collection of allowed race IDs
         * @return this builder
         */
        public Builder allowedRaces(Collection<String> races) {
            this.allowedRaces.clear();
            if (races != null) {
                races.stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(String::trim)
                        .forEach(this.allowedRaces::add);
            }
            return this;
        }

        /**
         * Adds a required skill.
         *
         * @param skillId the skill ID required
         * @return this builder
         */
        public Builder requireSkill(String skillId) {
            if (skillId != null && !skillId.isBlank()) {
                this.requiredSkills.add(skillId.trim());
            }
            return this;
        }

        /**
         * Sets all required skills from a collection.
         *
         * @param skills collection of required skill IDs
         * @return this builder
         */
        public Builder requiredSkills(Collection<String> skills) {
            this.requiredSkills.clear();
            if (skills != null) {
                skills.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim)
                        .forEach(this.requiredSkills::add);
            }
            return this;
        }

        /**
         * Sets the weapon type (marks this as a weapon).
         *
         * @param weaponType the weapon type
         * @return this builder
         */
        public Builder weaponType(WeaponType weaponType) {
            this.weaponType = weaponType;
            return this;
        }

        /**
         * Sets the armor type (marks this as armor).
         *
         * @param armorType the armor type
         * @return this builder
         */
        public Builder armorType(ArmorType armorType) {
            this.armorType = armorType;
            return this;
        }

        /**
         * Builds the ItemRequirement instance.
         *
         * @return a new ItemRequirement with the configured values
         */
        public ItemRequirement build() {
            return new ItemRequirement(this);
        }
    }
}
