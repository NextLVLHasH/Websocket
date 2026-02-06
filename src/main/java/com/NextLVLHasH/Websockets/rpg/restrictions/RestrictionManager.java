package com.NextLVLHasH.Websockets.rpg.restrictions;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.character.CharacterManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for all restriction checks in the RPG system.
 * Handles equipment restrictions, inventory calculations, and requirement validation.
 */
@SuppressWarnings("unused")
public final class RestrictionManager {

    private static final Logger LOGGER = Logger.getLogger(RestrictionManager.class.getName());

    // Singleton instance
    private static volatile RestrictionManager instance;

    // Configuration
    private InventoryRestriction inventoryRules;
    private Path configPath;

    // Class-specific weapon restrictions (classId -> allowed weapon types)
    private final Map<String, Set<WeaponType>> classWeaponRestrictions;

    // Class-specific armor restrictions (classId -> allowed armor types)
    private final Map<String, Set<ArmorType>> classArmorRestrictions;

    // Loading state
    private volatile boolean loaded = false;

    // ==================== Singleton ====================

    /**
     * Private constructor for singleton pattern.
     */
    private RestrictionManager() {
        this.inventoryRules = new InventoryRestriction();
        this.configPath = Path.of("config", "restrictions.json");
        this.classWeaponRestrictions = new HashMap<>();
        this.classArmorRestrictions = new HashMap<>();
        initializeDefaultRestrictions();
    }

    /**
     * Gets the singleton instance of RestrictionManager.
     *
     * @return the RestrictionManager instance
     */
    public static RestrictionManager getInstance() {
        if (instance == null) {
            synchronized (RestrictionManager.class) {
                if (instance == null) {
                    instance = new RestrictionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (primarily for testing).
     */
    public static void resetInstance() {
        synchronized (RestrictionManager.class) {
            instance = null;
        }
    }

    // ==================== Initialization ====================

    /**
     * Initializes default class restrictions.
     */
    private void initializeDefaultRestrictions() {
        // Warrior: melee weapons and heavy/medium armor
        classWeaponRestrictions.put("warrior", EnumSet.of(
                WeaponType.SWORD, WeaponType.AXE, WeaponType.MACE,
                WeaponType.SPEAR, WeaponType.SHIELD, WeaponType.FIST
        ));
        classArmorRestrictions.put("warrior", EnumSet.of(
                ArmorType.HEAVY, ArmorType.MEDIUM, ArmorType.LIGHT
        ));

        // Mage: magical weapons and cloth armor
        classWeaponRestrictions.put("mage", EnumSet.of(
                WeaponType.STAFF, WeaponType.WAND, WeaponType.DAGGER
        ));
        classArmorRestrictions.put("mage", EnumSet.of(
                ArmorType.CLOTH, ArmorType.NONE
        ));

        // Rogue: agility weapons and light armor
        classWeaponRestrictions.put("rogue", EnumSet.of(
                WeaponType.DAGGER, WeaponType.SWORD, WeaponType.BOW,
                WeaponType.CROSSBOW, WeaponType.FIST
        ));
        classArmorRestrictions.put("rogue", EnumSet.of(
                ArmorType.LIGHT, ArmorType.MEDIUM
        ));

        // Ranger: ranged and some melee
        classWeaponRestrictions.put("ranger", EnumSet.of(
                WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.SWORD,
                WeaponType.DAGGER, WeaponType.SPEAR
        ));
        classArmorRestrictions.put("ranger", EnumSet.of(
                ArmorType.LIGHT, ArmorType.MEDIUM
        ));

        // Priest: healing focus
        classWeaponRestrictions.put("priest", EnumSet.of(
                WeaponType.STAFF, WeaponType.WAND, WeaponType.MACE
        ));
        classArmorRestrictions.put("priest", EnumSet.of(
                ArmorType.CLOTH, ArmorType.LIGHT
        ));

        // Default profession bonuses
        inventoryRules.setProfessionBonus("merchant", 10);
        inventoryRules.setProfessionBonus("blacksmith", 5);
        inventoryRules.setProfessionBonus("alchemist", 5);
        inventoryRules.setProfessionBonus("miner", 8);
    }

    // ==================== Configuration ====================

    /**
     * Sets the configuration file path.
     *
     * @param configPath the path to the restrictions config file
     */
    public void setConfigPath(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "Config path cannot be null");
    }

    /**
     * Gets the inventory restriction rules.
     *
     * @return the inventory restriction rules
     */
    public InventoryRestriction getInventoryRules() {
        return inventoryRules;
    }

    /**
     * Sets the inventory restriction rules.
     *
     * @param inventoryRules the new inventory rules
     */
    public void setInventoryRules(InventoryRestriction inventoryRules) {
        this.inventoryRules = Objects.requireNonNull(inventoryRules, "Inventory rules cannot be null");
    }

    /**
     * Loads restriction configuration from file.
     */
    public void loadConfig() {
        LOGGER.info("Loading restriction configuration from: " + configPath);

        if (!Files.exists(configPath)) {
            LOGGER.info("Restriction config not found, using defaults");
            loaded = true;
            return;
        }

        try {
            String content = Files.readString(configPath);
            parseConfig(content);
            LOGGER.info("Restriction configuration loaded successfully");
            loaded = true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load restriction config, using defaults", e);
            loaded = true;
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "Invalid JSON in restriction config, using defaults", e);
            loaded = true;
        }
    }

    /**
     * Parses the JSON configuration string and updates internal state.
     *
     * @param content the JSON content to parse
     */
    private void parseConfig(String content) {
        Gson gson = new Gson();
        RestrictionsConfig config = gson.fromJson(content, RestrictionsConfig.class);

        if (config == null) {
            LOGGER.warning("Parsed config is null, using defaults");
            return;
        }

        // Update inventory rules if present
        if (config.inventory != null) {
            InventoryConfig inv = config.inventory;
            if (inv.baseSlots > 0) {
                inventoryRules.setBaseSlots(inv.baseSlots);
                LOGGER.fine("Loaded baseSlots: " + inv.baseSlots);
            }
            if (inv.slotsPerConstitution >= 0) {
                inventoryRules.setSlotsPerConstitution(inv.slotsPerConstitution);
                LOGGER.fine("Loaded slotsPerConstitution: " + inv.slotsPerConstitution);
            }
            if (inv.slotsPerLevel >= 0) {
                inventoryRules.setSlotsPerLevel(inv.slotsPerLevel);
                LOGGER.fine("Loaded slotsPerLevel: " + inv.slotsPerLevel);
            }
            if (inv.maxSlots > 0) {
                inventoryRules.setMaxSlots(inv.maxSlots);
                LOGGER.fine("Loaded maxSlots: " + inv.maxSlots);
            }
            if (inv.professionBonuses != null) {
                for (Map.Entry<String, Integer> entry : inv.professionBonuses.entrySet()) {
                    inventoryRules.setProfessionBonus(entry.getKey(), entry.getValue());
                    LOGGER.fine("Loaded profession bonus: " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }

        // Update class restrictions if present
        if (config.classRestrictions != null) {
            for (Map.Entry<String, ClassRestrictionConfig> entry : config.classRestrictions.entrySet()) {
                String classId = entry.getKey().toLowerCase().trim();
                ClassRestrictionConfig classConfig = entry.getValue();

                // Parse weapon restrictions
                if (classConfig.weapons != null && !classConfig.weapons.isEmpty()) {
                    EnumSet<WeaponType> weapons = EnumSet.noneOf(WeaponType.class);
                    for (String weaponName : classConfig.weapons) {
                        try {
                            WeaponType weapon = WeaponType.valueOf(weaponName.toUpperCase().trim());
                            weapons.add(weapon);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning("Unknown weapon type in config: " + weaponName);
                        }
                    }
                    if (!weapons.isEmpty()) {
                        classWeaponRestrictions.put(classId, weapons);
                        LOGGER.fine("Loaded weapon restrictions for " + classId + ": " + weapons);
                    }
                }

                // Parse armor restrictions
                if (classConfig.armor != null && !classConfig.armor.isEmpty()) {
                    EnumSet<ArmorType> armors = EnumSet.noneOf(ArmorType.class);
                    for (String armorName : classConfig.armor) {
                        try {
                            ArmorType armor = ArmorType.valueOf(armorName.toUpperCase().trim());
                            armors.add(armor);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning("Unknown armor type in config: " + armorName);
                        }
                    }
                    if (!armors.isEmpty()) {
                        classArmorRestrictions.put(classId, armors);
                        LOGGER.fine("Loaded armor restrictions for " + classId + ": " + armors);
                    }
                }
            }
        }

        LOGGER.info("Parsed restriction config: " + classWeaponRestrictions.size() + " class weapon restrictions, " +
                classArmorRestrictions.size() + " class armor restrictions");
    }

    // ==================== Config Inner Classes ====================

    /**
     * Top-level configuration structure for restrictions.
     */
    private static class RestrictionsConfig {
        InventoryConfig inventory;
        Map<String, ClassRestrictionConfig> classRestrictions;
    }

    /**
     * Configuration for inventory slot calculations.
     */
    private static class InventoryConfig {
        int baseSlots;
        int slotsPerConstitution;
        int slotsPerLevel;
        int maxSlots;
        Map<String, Integer> professionBonuses;
    }

    /**
     * Configuration for per-class equipment restrictions.
     */
    private static class ClassRestrictionConfig {
        List<String> weapons;
        List<String> armor;
    }

    /**
     * Checks if configuration has been loaded.
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    // ==================== Equipment Checks ====================

    /**
     * Checks if a player can equip an item based on all requirements.
     *
     * @param data the player's RPG data
     * @param item the item requirements to check
     * @return the restriction result
     */
    public RestrictionResult canEquipItem(PlayerRPGData data, ItemRequirement item) {
        if (data == null) {
            return RestrictionResult.denied("Player data is null");
        }
        if (item == null) {
            return RestrictionResult.denied("Item requirements are null");
        }

        List<String> failureReasons = new ArrayList<>();

        // Check level requirement
        if (!meetsLevelRequirement(data, item.getRequiredLevel())) {
            failureReasons.add(String.format("Requires level %d (you are level %d)",
                    item.getRequiredLevel(), data.getLevel()));
        }

        // Check attribute requirements
        if (!meetsAttributeRequirements(data, item.getRequiredAttributes())) {
            for (Map.Entry<AttributeType, Integer> entry : item.getRequiredAttributes().entrySet()) {
                int playerValue = data.getCurrentAttribute(entry.getKey());
                if (playerValue < entry.getValue()) {
                    failureReasons.add(String.format("Requires %d %s (you have %d)",
                            entry.getValue(), entry.getKey().getDisplayName(), playerValue));
                }
            }
        }

        // Check class restriction
        if (item.hasClassRestriction() && !item.isClassAllowed(data.getSelectedClass())) {
            failureReasons.add(String.format("Your class (%s) cannot use this item",
                    data.getSelectedClass() != null ? data.getSelectedClass() : "none"));
        }

        // Check race restriction
        if (item.hasRaceRestriction() && !item.isRaceAllowed(data.getSelectedRace())) {
            failureReasons.add(String.format("Your race (%s) cannot use this item",
                    data.getSelectedRace() != null ? data.getSelectedRace() : "none"));
        }

        // Check skill requirements
        if (item.hasSkillRequirements()) {
            for (String skillId : item.getRequiredSkills()) {
                if (!data.getUnlockedSkills().contains(skillId)) {
                    failureReasons.add(String.format("Requires skill: %s", skillId));
                }
            }
        }

        // Check weapon type restriction
        if (item.isWeapon()) {
            RestrictionResult weaponResult = canUseWeapon(data, item.getWeaponType());
            if (weaponResult.isDenied()) {
                failureReasons.addAll(weaponResult.getFailureReasons());
            }
        }

        // Check armor type restriction
        if (item.isArmor()) {
            RestrictionResult armorResult = canWearArmor(data, item.getArmorType());
            if (armorResult.isDenied()) {
                failureReasons.addAll(armorResult.getFailureReasons());
            }
        }

        return failureReasons.isEmpty()
                ? RestrictionResult.allowed()
                : RestrictionResult.denied(failureReasons);
    }

    /**
     * Checks if a player can use a specific weapon type based on their class.
     *
     * @param data   the player's RPG data
     * @param weapon the weapon type to check
     * @return the restriction result
     */
    public RestrictionResult canUseWeapon(PlayerRPGData data, WeaponType weapon) {
        if (data == null) {
            return RestrictionResult.denied("Player data is null");
        }
        if (weapon == null) {
            return RestrictionResult.denied("Weapon type is null");
        }

        String classId = data.getSelectedClass();
        if (classId == null) {
            // No class selected, allow all weapons
            return RestrictionResult.allowed();
        }

        String normalizedClassId = classId.toLowerCase().trim();
        Set<WeaponType> allowedWeapons = classWeaponRestrictions.get(normalizedClassId);

        if (allowedWeapons == null) {
            // Unknown class, allow all weapons
            return RestrictionResult.allowed();
        }

        if (allowedWeapons.contains(weapon)) {
            return RestrictionResult.allowed();
        }

        return RestrictionResult.denied(String.format(
                "Your class (%s) cannot use %s weapons",
                classId, weapon.getDisplayName()));
    }

    /**
     * Checks if a player can wear a specific armor type based on their class.
     *
     * @param data  the player's RPG data
     * @param armor the armor type to check
     * @return the restriction result
     */
    public RestrictionResult canWearArmor(PlayerRPGData data, ArmorType armor) {
        if (data == null) {
            return RestrictionResult.denied("Player data is null");
        }
        if (armor == null) {
            return RestrictionResult.denied("Armor type is null");
        }

        // NONE armor is always allowed
        if (armor == ArmorType.NONE) {
            return RestrictionResult.allowed();
        }

        String classId = data.getSelectedClass();
        if (classId == null) {
            // No class selected, allow all armor
            return RestrictionResult.allowed();
        }

        String normalizedClassId = classId.toLowerCase().trim();
        Set<ArmorType> allowedArmor = classArmorRestrictions.get(normalizedClassId);

        if (allowedArmor == null) {
            // Unknown class, allow all armor
            return RestrictionResult.allowed();
        }

        if (allowedArmor.contains(armor)) {
            return RestrictionResult.allowed();
        }

        return RestrictionResult.denied(String.format(
                "Your class (%s) cannot wear %s",
                classId, armor.getDisplayName()));
    }

    // ==================== Inventory ====================

    /**
     * Gets the calculated inventory size for a player.
     *
     * @param data the player's RPG data
     * @return the number of inventory slots
     */
    public int getInventorySize(PlayerRPGData data) {
        return inventoryRules.calculateSlots(data);
    }

    /**
     * Gets detailed inventory breakdown for a player.
     *
     * @param data the player's RPG data
     * @return map with slot breakdown by source
     */
    public Map<String, Integer> getInventorySizeDetailed(PlayerRPGData data) {
        return inventoryRules.calculateSlotsDetailed(data);
    }

    // ==================== Requirement Checks ====================

    /**
     * Checks if a player meets a level requirement.
     *
     * @param data     the player's RPG data
     * @param required the required level
     * @return true if requirement is met
     */
    public boolean meetsLevelRequirement(PlayerRPGData data, int required) {
        if (data == null) {
            return required <= 1;
        }
        return data.getLevel() >= required;
    }

    /**
     * Checks if a player meets all attribute requirements.
     *
     * @param data     the player's RPG data
     * @param required map of required attribute values
     * @return true if all requirements are met
     */
    public boolean meetsAttributeRequirements(PlayerRPGData data, Map<AttributeType, Integer> required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        if (data == null) {
            return false;
        }

        for (Map.Entry<AttributeType, Integer> entry : required.entrySet()) {
            int playerValue = data.getCurrentAttribute(entry.getKey());
            if (playerValue < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the list of unmet attribute requirements.
     *
     * @param data     the player's RPG data
     * @param required map of required attribute values
     * @return list of unmet requirements as formatted strings
     */
    public List<String> getUnmetAttributeRequirements(PlayerRPGData data, Map<AttributeType, Integer> required) {
        List<String> unmet = new ArrayList<>();
        if (required == null || required.isEmpty() || data == null) {
            return unmet;
        }

        for (Map.Entry<AttributeType, Integer> entry : required.entrySet()) {
            int playerValue = data.getCurrentAttribute(entry.getKey());
            if (playerValue < entry.getValue()) {
                unmet.add(String.format("%s: %d/%d",
                        entry.getKey().getDisplayName(), playerValue, entry.getValue()));
            }
        }
        return unmet;
    }

    // ==================== Class Restriction Management ====================

    /**
     * Sets the allowed weapon types for a class.
     *
     * @param classId        the class ID
     * @param allowedWeapons the set of allowed weapon types
     */
    public void setClassWeaponRestrictions(String classId, Set<WeaponType> allowedWeapons) {
        if (classId != null && !classId.isBlank()) {
            String normalized = classId.toLowerCase().trim();
            if (allowedWeapons != null && !allowedWeapons.isEmpty()) {
                classWeaponRestrictions.put(normalized, EnumSet.copyOf(allowedWeapons));
            } else {
                classWeaponRestrictions.remove(normalized);
            }
        }
    }

    /**
     * Sets the allowed armor types for a class.
     *
     * @param classId       the class ID
     * @param allowedArmor the set of allowed armor types
     */
    public void setClassArmorRestrictions(String classId, Set<ArmorType> allowedArmor) {
        if (classId != null && !classId.isBlank()) {
            String normalized = classId.toLowerCase().trim();
            if (allowedArmor != null && !allowedArmor.isEmpty()) {
                classArmorRestrictions.put(normalized, EnumSet.copyOf(allowedArmor));
            } else {
                classArmorRestrictions.remove(normalized);
            }
        }
    }

    /**
     * Gets the allowed weapon types for a class.
     *
     * @param classId the class ID
     * @return unmodifiable set of allowed weapon types, or all types if unrestricted
     */
    public Set<WeaponType> getAllowedWeapons(String classId) {
        if (classId == null || classId.isBlank()) {
            return EnumSet.allOf(WeaponType.class);
        }
        Set<WeaponType> allowed = classWeaponRestrictions.get(classId.toLowerCase().trim());
        return allowed != null ? Collections.unmodifiableSet(allowed) : EnumSet.allOf(WeaponType.class);
    }

    /**
     * Gets the allowed armor types for a class.
     *
     * @param classId the class ID
     * @return unmodifiable set of allowed armor types, or all types if unrestricted
     */
    public Set<ArmorType> getAllowedArmor(String classId) {
        if (classId == null || classId.isBlank()) {
            return EnumSet.allOf(ArmorType.class);
        }
        Set<ArmorType> allowed = classArmorRestrictions.get(classId.toLowerCase().trim());
        return allowed != null ? Collections.unmodifiableSet(allowed) : EnumSet.allOf(ArmorType.class);
    }

    /**
     * Clears all class-specific restrictions.
     */
    public void clearClassRestrictions() {
        classWeaponRestrictions.clear();
        classArmorRestrictions.clear();
    }
}
