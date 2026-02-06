package com.NextLVLHasH.Websockets.rpg.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.*;

/**
 * Represents a passive perk/bonus in the RPG system.
 * Perks are permanent bonuses that modify player stats, abilities, or gameplay mechanics.
 * They integrate with Hytale's EntityStatMap modifier system.
 * 
 * Perks use PASSIVE SkillType - they are always active once unlocked.
 * 
 * Example perks:
 * - Mining: +10% XP from mining, faster break speed
 * - Combat: +5% damage dealt, +10% critical chance
 * - Health: +20 max health, +5% health regen
 * - Mana: +30 max mana, -10% mana cost
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class Perk {
    
    // ==================== Identity ====================
    
    @SerializedName("id")
    private final String id;
    
    @SerializedName("display_name")
    private final String displayName;
    
    @SerializedName("description")
    private final String description;
    
    @SerializedName("icon_path")
    private final String iconPath;
    
    // ==================== Classification ====================
    
    @SerializedName("category")
    private final SkillCategory category;
    
    @SerializedName("tier")
    private final int tier;
    
    // ==================== Requirements ====================
    
    @SerializedName("required_level")
    private final int requiredLevel;
    
    @SerializedName("skill_point_cost")
    private final int skillPointCost;
    
    @SerializedName("prerequisite_perks")
    private final List<String> prerequisitePerks;
    
    // ==================== Class/Subclass Restrictions ====================
    
    @SerializedName("required_class")
    private final String requiredClass;
    
    @SerializedName("required_subclass")
    private final String requiredSubclass;
    
    // ==================== Effects ====================
    
    /**
     * Stat modifiers to apply via Hytale's EntityStatMap.
     * Keys are stat names (Health, Mana, Stamina, etc.)
     * Values are PerkModifier definitions.
     */
    @SerializedName("stat_modifiers")
    private final Map<String, PerkModifier> statModifiers;
    
    /**
     * Custom gameplay modifiers (not Hytale stats).
     * Examples: mining_xp_bonus, damage_bonus, mining_speed, etc.
     */
    @SerializedName("gameplay_modifiers")
    private final Map<String, Double> gameplayModifiers;
    
    // ==================== Modifier Definition ====================
    
    /**
     * Defines a stat modifier for Hytale's EntityStatMap system.
     */
    public static class PerkModifier {
        @SerializedName("target")
        private final ModifierTarget target;
        
        @SerializedName("calculation_type")
        private final CalculationType calculationType;
        
        @SerializedName("amount")
        private final float amount;
        
        public PerkModifier(ModifierTarget target, CalculationType calculationType, float amount) {
            this.target = target;
            this.calculationType = calculationType;
            this.amount = amount;
        }
        
        public ModifierTarget getTarget() { return target; }
        public CalculationType getCalculationType() { return calculationType; }
        public float getAmount() { return amount; }
        
        @Override
        public String toString() {
            String sign = amount >= 0 ? "+" : "";
            if (calculationType == CalculationType.MULTIPLICATIVE) {
                return sign + (int)(amount * 100) + "% " + target.getDisplayName();
            } else {
                return sign + (int)amount + " " + target.getDisplayName();
            }
        }
    }
    
    /**
     * Target for the modifier - matches Hytale's Modifier.ModifierTarget
     */
    public enum ModifierTarget {
        MIN("Min"),
        MAX("Max"),
        VALUE("Value");
        
        private final String displayName;
        ModifierTarget(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Calculation type - matches Hytale's StaticModifier.CalculationType
     */
    public enum CalculationType {
        ADDITIVE,       // Adds flat value
        MULTIPLICATIVE  // Multiplies by (1 + amount)
    }
    
    // ==================== GSON ====================
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    // ==================== Constructor ====================
    
    private Perk(String id, String displayName, String description, String iconPath,
                 SkillCategory category, int tier, int requiredLevel, int skillPointCost,
                 List<String> prerequisitePerks, String requiredClass, String requiredSubclass,
                 Map<String, PerkModifier> statModifiers, Map<String, Double> gameplayModifiers) {
        this.id = Objects.requireNonNull(id, "Perk id cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";
        this.category = category != null ? category : SkillCategory.UTILITIES;
        this.tier = Math.max(0, tier);
        this.requiredLevel = Math.max(1, requiredLevel);
        this.skillPointCost = Math.max(1, skillPointCost);
        this.prerequisitePerks = prerequisitePerks != null 
                ? Collections.unmodifiableList(new ArrayList<>(prerequisitePerks))
                : Collections.emptyList();
        this.requiredClass = requiredClass;
        this.requiredSubclass = requiredSubclass;
        this.statModifiers = statModifiers != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(statModifiers))
                : Collections.emptyMap();
        this.gameplayModifiers = gameplayModifiers != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(gameplayModifiers))
                : Collections.emptyMap();
    }
    
    // ==================== Getters ====================
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
    public SkillCategory getCategory() { return category; }
    public int getTier() { return tier; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getSkillPointCost() { return skillPointCost; }
    public List<String> getPrerequisitePerks() { return prerequisitePerks; }
    public String getRequiredClass() { return requiredClass; }
    public String getRequiredSubclass() { return requiredSubclass; }
    public Map<String, PerkModifier> getStatModifiers() { return statModifiers; }
    public Map<String, Double> getGameplayModifiers() { return gameplayModifiers; }
    
    /**
     * Gets a specific gameplay modifier value.
     * @param key the modifier key
     * @return the modifier value, or 0 if not present
     */
    public double getGameplayModifier(String key) {
        return gameplayModifiers.getOrDefault(key, 0.0);
    }
    
    /**
     * Generates a formatted description of all effects.
     */
    public String getEffectsDescription() {
        StringBuilder sb = new StringBuilder();
        
        // Stat modifiers
        for (Map.Entry<String, PerkModifier> entry : statModifiers.entrySet()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        
        // Gameplay modifiers
        for (Map.Entry<String, Double> entry : gameplayModifiers.entrySet()) {
            if (sb.length() > 0) sb.append("\n");
            String key = entry.getKey().replace("_", " ");
            double value = entry.getValue();
            String sign = value >= 0 ? "+" : "";
            if (Math.abs(value) < 1) {
                sb.append(key).append(": ").append(sign).append((int)(value * 100)).append("%");
            } else {
                sb.append(key).append(": ").append(sign).append((int)value);
            }
        }
        
        return sb.toString();
    }
    
    // ==================== Serialization ====================
    
    public String toJson() {
        return GSON.toJson(this);
    }
    
    public static Perk fromJson(String json) {
        return GSON.fromJson(json, Perk.class);
    }
    
    // ==================== Builder ====================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String displayName;
        private String description;
        private String iconPath;
        private SkillCategory category = SkillCategory.UTILITIES;
        private int tier = 0;
        private int requiredLevel = 1;
        private int skillPointCost = 1;
        private final List<String> prerequisitePerks = new ArrayList<>();
        private String requiredClass = null;
        private String requiredSubclass = null;
        private final Map<String, PerkModifier> statModifiers = new LinkedHashMap<>();
        private final Map<String, Double> gameplayModifiers = new LinkedHashMap<>();
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder iconPath(String iconPath) { this.iconPath = iconPath; return this; }
        public Builder category(SkillCategory category) { this.category = category; return this; }
        public Builder tier(int tier) { this.tier = tier; return this; }
        public Builder requiredLevel(int level) { this.requiredLevel = level; return this; }
        public Builder skillPointCost(int cost) { this.skillPointCost = cost; return this; }
        public Builder prerequisite(String perkId) { this.prerequisitePerks.add(perkId); return this; }
        public Builder requiredClass(String classId) { this.requiredClass = classId; return this; }
        public Builder requiredSubclass(String subclassId) { this.requiredSubclass = subclassId; return this; }
        
        /**
         * Adds a Hytale stat modifier (Health, Mana, Stamina, etc.)
         */
        public Builder statModifier(String statName, ModifierTarget target, 
                                    CalculationType calcType, float amount) {
            this.statModifiers.put(statName, new PerkModifier(target, calcType, amount));
            return this;
        }
        
        /**
         * Adds a flat stat increase (e.g., +20 max health)
         */
        public Builder additiveStat(String statName, float amount) {
            return statModifier(statName, ModifierTarget.MAX, CalculationType.ADDITIVE, amount);
        }
        
        /**
         * Adds a percentage stat increase (e.g., +10% max health)
         */
        public Builder percentStat(String statName, float percent) {
            return statModifier(statName, ModifierTarget.MAX, CalculationType.MULTIPLICATIVE, percent);
        }
        
        /**
         * Adds a gameplay modifier (mining_xp_bonus, damage_bonus, etc.)
         */
        public Builder gameplayModifier(String key, double value) {
            this.gameplayModifiers.put(key, value);
            return this;
        }
        
        public Perk build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Perk id is required");
            }
            return new Perk(id, displayName, description, iconPath, category, tier,
                    requiredLevel, skillPointCost, prerequisitePerks, requiredClass, requiredSubclass,
                    statModifiers, gameplayModifiers);
        }
    }
    
    @Override
    public String toString() {
        return "Perk{id='" + id + "', displayName='" + displayName + "', category=" + category + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Perk perk = (Perk) o;
        return Objects.equals(id, perk.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
