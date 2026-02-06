package com.NextLVLHasH.Websockets.rpg.restrictions;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.util.*;

/**
 * Defines rules for calculating inventory size based on player attributes.
 * Inventory slots can be increased through level, constitution, and professions.
 */
public class InventoryRestriction {

    private int baseSlots;
    private int slotsPerConstitution;
    private int slotsPerLevel;
    private int maxSlots;
    private final Map<String, Integer> professionBonuses;

    // Default values
    private static final int DEFAULT_BASE_SLOTS = 20;
    private static final int DEFAULT_SLOTS_PER_CONSTITUTION = 2;
    private static final int DEFAULT_SLOTS_PER_LEVEL = 1;
    private static final int DEFAULT_MAX_SLOTS = 60;

    /**
     * Creates an InventoryRestriction with default values.
     */
    public InventoryRestriction() {
        this.baseSlots = DEFAULT_BASE_SLOTS;
        this.slotsPerConstitution = DEFAULT_SLOTS_PER_CONSTITUTION;
        this.slotsPerLevel = DEFAULT_SLOTS_PER_LEVEL;
        this.maxSlots = DEFAULT_MAX_SLOTS;
        this.professionBonuses = new HashMap<>();
    }

    /**
     * Creates an InventoryRestriction with specified values.
     *
     * @param baseSlots            the base number of inventory slots
     * @param slotsPerConstitution extra slots per point of constitution above 10
     * @param slotsPerLevel        extra slots per level above 1
     * @param maxSlots             the maximum slots possible
     */
    public InventoryRestriction(int baseSlots, int slotsPerConstitution,
                                int slotsPerLevel, int maxSlots) {
        this.baseSlots = Math.max(1, baseSlots);
        this.slotsPerConstitution = Math.max(0, slotsPerConstitution);
        this.slotsPerLevel = Math.max(0, slotsPerLevel);
        this.maxSlots = Math.max(this.baseSlots, maxSlots);
        this.professionBonuses = new HashMap<>();
    }

    // ==================== Getters/Setters ====================

    /**
     * Gets the base number of inventory slots.
     *
     * @return the base slots
     */
    public int getBaseSlots() {
        return baseSlots;
    }

    /**
     * Sets the base number of inventory slots.
     *
     * @param baseSlots the base slots (minimum 1)
     */
    public void setBaseSlots(int baseSlots) {
        this.baseSlots = Math.max(1, baseSlots);
    }

    /**
     * Gets the slots gained per constitution point above base.
     *
     * @return slots per constitution point
     */
    public int getSlotsPerConstitution() {
        return slotsPerConstitution;
    }

    /**
     * Sets the slots gained per constitution point above base.
     *
     * @param slotsPerConstitution slots per constitution (minimum 0)
     */
    public void setSlotsPerConstitution(int slotsPerConstitution) {
        this.slotsPerConstitution = Math.max(0, slotsPerConstitution);
    }

    /**
     * Gets the slots gained per level above 1.
     *
     * @return slots per level
     */
    public int getSlotsPerLevel() {
        return slotsPerLevel;
    }

    /**
     * Sets the slots gained per level above 1.
     *
     * @param slotsPerLevel slots per level (minimum 0)
     */
    public void setSlotsPerLevel(int slotsPerLevel) {
        this.slotsPerLevel = Math.max(0, slotsPerLevel);
    }

    /**
     * Gets the maximum possible inventory slots.
     *
     * @return the max slots
     */
    public int getMaxSlots() {
        return maxSlots;
    }

    /**
     * Sets the maximum possible inventory slots.
     *
     * @param maxSlots the max slots (minimum equals base slots)
     */
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(this.baseSlots, maxSlots);
    }

    /**
     * Gets the profession bonuses map.
     *
     * @return unmodifiable map of profession ID to bonus slots
     */
    public Map<String, Integer> getProfessionBonuses() {
        return Collections.unmodifiableMap(professionBonuses);
    }

    // ==================== Profession Bonus Management ====================

    /**
     * Adds or updates a profession bonus.
     *
     * @param professionId the profession ID
     * @param bonusSlots   the number of bonus slots
     */
    public void setProfessionBonus(String professionId, int bonusSlots) {
        if (professionId != null && !professionId.isBlank()) {
            if (bonusSlots > 0) {
                professionBonuses.put(professionId.trim().toLowerCase(), bonusSlots);
            } else {
                professionBonuses.remove(professionId.trim().toLowerCase());
            }
        }
    }

    /**
     * Removes a profession bonus.
     *
     * @param professionId the profession ID
     */
    public void removeProfessionBonus(String professionId) {
        if (professionId != null) {
            professionBonuses.remove(professionId.trim().toLowerCase());
        }
    }

    /**
     * Gets the bonus slots for a specific profession.
     *
     * @param professionId the profession ID
     * @return the bonus slots, or 0 if no bonus
     */
    public int getProfessionBonus(String professionId) {
        if (professionId == null || professionId.isBlank()) {
            return 0;
        }
        return professionBonuses.getOrDefault(professionId.trim().toLowerCase(), 0);
    }

    /**
     * Clears all profession bonuses.
     */
    public void clearProfessionBonuses() {
        professionBonuses.clear();
    }

    // ==================== Calculation Methods ====================

    /**
     * Calculates the total inventory slots for a player.
     *
     * @param data the player's RPG data
     * @return the calculated number of inventory slots
     */
    public int calculateSlots(PlayerRPGData data) {
        if (data == null) {
            return baseSlots;
        }

        int totalSlots = baseSlots;

        // Add slots from constitution (using current attribute, above base of 10)
        int constitution = data.getCurrentAttribute(AttributeType.CONSTITUTION);
        int constitutionBonus = Math.max(0, constitution - 10);
        totalSlots += constitutionBonus * slotsPerConstitution;

        // Add slots from level (above level 1)
        int level = data.getLevel();
        int levelBonus = Math.max(0, level - 1);
        totalSlots += levelBonus * slotsPerLevel;

        // Add profession bonus if applicable
        String profession = data.getSelectedProfession();
        if (profession != null) {
            totalSlots += getProfessionBonus(profession);
        }

        // Cap at maximum slots
        return Math.min(totalSlots, maxSlots);
    }

    /**
     * Calculates slots with detailed breakdown.
     *
     * @param data the player's RPG data
     * @return a map containing slot breakdown by source
     */
    public Map<String, Integer> calculateSlotsDetailed(PlayerRPGData data) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        
        breakdown.put("base", baseSlots);
        
        if (data == null) {
            breakdown.put("total", baseSlots);
            return breakdown;
        }

        int constitution = data.getCurrentAttribute(AttributeType.CONSTITUTION);
        int constitutionBonus = Math.max(0, constitution - 10) * slotsPerConstitution;
        breakdown.put("constitution", constitutionBonus);

        int level = data.getLevel();
        int levelBonus = Math.max(0, level - 1) * slotsPerLevel;
        breakdown.put("level", levelBonus);

        String profession = data.getSelectedProfession();
        int profBonus = (profession != null) ? getProfessionBonus(profession) : 0;
        breakdown.put("profession", profBonus);

        int rawTotal = baseSlots + constitutionBonus + levelBonus + profBonus;
        int cappedTotal = Math.min(rawTotal, maxSlots);
        
        breakdown.put("rawTotal", rawTotal);
        breakdown.put("total", cappedTotal);
        
        if (rawTotal > maxSlots) {
            breakdown.put("cappedAmount", rawTotal - maxSlots);
        }

        return breakdown;
    }

    /**
     * Checks if the player has reached maximum inventory capacity.
     *
     * @param data the player's RPG data
     * @return true if at max slots
     */
    public boolean isAtMaxCapacity(PlayerRPGData data) {
        return calculateSlots(data) >= maxSlots;
    }

    /**
     * Calculates how many more slots can be gained.
     *
     * @param data the player's RPG data
     * @return the number of potential additional slots
     */
    public int getRemainingCapacity(PlayerRPGData data) {
        return maxSlots - calculateSlots(data);
    }

    /**
     * Estimates slots at a specific level with current constitution.
     *
     * @param data        the player's RPG data
     * @param targetLevel the target level to calculate for
     * @return estimated slots at that level
     */
    public int estimateSlotsAtLevel(PlayerRPGData data, int targetLevel) {
        if (data == null || targetLevel < 1) {
            return baseSlots;
        }

        int totalSlots = baseSlots;

        int constitution = data.getCurrentAttribute(AttributeType.CONSTITUTION);
        int constitutionBonus = Math.max(0, constitution - 10);
        totalSlots += constitutionBonus * slotsPerConstitution;

        int levelBonus = Math.max(0, targetLevel - 1);
        totalSlots += levelBonus * slotsPerLevel;

        String profession = data.getSelectedProfession();
        if (profession != null) {
            totalSlots += getProfessionBonus(profession);
        }

        return Math.min(totalSlots, maxSlots);
    }

    /**
     * Creates a copy of this InventoryRestriction.
     *
     * @return a new copy with the same values
     */
    public InventoryRestriction copy() {
        InventoryRestriction copy = new InventoryRestriction(
                baseSlots, slotsPerConstitution, slotsPerLevel, maxSlots);
        copy.professionBonuses.putAll(this.professionBonuses);
        return copy;
    }

    @Override
    public String toString() {
        return String.format("InventoryRestriction{base=%d, perCon=%d, perLevel=%d, max=%d, professions=%s}",
                baseSlots, slotsPerConstitution, slotsPerLevel, maxSlots, professionBonuses);
    }
}
