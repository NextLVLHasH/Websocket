package com.NextLVLHasH.Websockets.rpg.skills;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the perk system - loading from JSON, tracking unlocked perks,
 * and applying perk effects to players via Hytale's EntityStatMap.
 * 
 * Perks are organized by:
 * - Category (COMBAT, UTILITIES, MAGIC)
 * - Tier (0 = basic, higher = more powerful, requires prerequisites)
 * 
 * JSON Storage Structure:
 * - perks.json: Perk definitions with costs, effects, tiers
 * - player_perks/{uuid}.json: Player's unlocked perks
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class PerkManager {
    
    private static final Logger LOGGER = Logger.getLogger(PerkManager.class.getName());
    
    // ==================== Singleton ====================
    
    private static volatile PerkManager instance;
    private static final Object LOCK = new Object();
    
    public static PerkManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PerkManager();
                }
            }
        }
        return instance;
    }
    
    // ==================== Configuration ====================
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    
    @SuppressWarnings("unused")
    private Path dataPath;
    private Path perksFile;
    private Path playerPerksDir;
    
    // ==================== Data Storage ====================
    
    /** All registered perks by ID */
    private final Map<String, Perk> perks = new ConcurrentHashMap<>();
    
    /** Perks organized by category */
    private final Map<SkillCategory, List<Perk>> perksByCategory = new ConcurrentHashMap<>();
    
    /** Perks organized by tier */
    private final Map<Integer, List<Perk>> perksByTier = new ConcurrentHashMap<>();
    
    /** Player unlocked perks: UUID -> Set of perk IDs */
    private final Map<UUID, Set<String>> playerPerks = new ConcurrentHashMap<>();
    
    /** Player perk points: UUID -> available points */
    private final Map<UUID, Integer> playerPerkPoints = new ConcurrentHashMap<>();
    
    // ==================== Constructor ====================
    
    private PerkManager() {
        // Private constructor for singleton
    }
    
    // ==================== Initialization ====================
    
    /**
     * Initialize the perk manager with the data directory.
     * Creates default perks if none exist.
     */
    public void initialize(Path dataPath) {
        this.dataPath = dataPath;
        this.perksFile = dataPath.resolve("config/perks.json");
        this.playerPerksDir = dataPath.resolve("player_perks");
        
        try {
            // Create directories
            Files.createDirectories(perksFile.getParent());
            Files.createDirectories(playerPerksDir);
            
            // Load or create default perks
            if (Files.exists(perksFile)) {
                loadPerks();
            } else {
                createDefaultPerks();
                savePerks();
            }
            
            LOGGER.info("PerkManager initialized with " + perks.size() + " perks");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize PerkManager", e);
        }
    }
    
    // ==================== Perk Loading/Saving ====================
    
    /**
     * Load all perk definitions from perks.json
     */
    public void loadPerks() {
        perks.clear();
        perksByCategory.clear();
        perksByTier.clear();
        
        try (Reader reader = Files.newBufferedReader(perksFile)) {
            Type listType = new TypeToken<List<Perk>>(){}.getType();
            List<Perk> perkList = GSON.fromJson(reader, listType);
            
            if (perkList != null) {
                for (Perk perk : perkList) {
                    registerPerk(perk);
                }
            }
            
            LOGGER.info("Loaded " + perks.size() + " perks from " + perksFile);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load perks, creating defaults", e);
            createDefaultPerks();
            savePerks();
        }
    }
    
    /**
     * Save all perk definitions to perks.json
     */
    public void savePerks() {
        try (Writer writer = Files.newBufferedWriter(perksFile)) {
            List<Perk> perkList = new ArrayList<>(perks.values());
            // Sort by category then tier then name
            perkList.sort(Comparator
                    .comparing(Perk::getCategory)
                    .thenComparing(Perk::getTier)
                    .thenComparing(Perk::getId));
            GSON.toJson(perkList, writer);
            LOGGER.info("Saved " + perkList.size() + " perks to " + perksFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save perks", e);
        }
    }
    
    /**
     * Register a perk in all lookup maps.
     * Can be used to add custom perks at runtime.
     */
    public void registerPerk(Perk perk) {
        perks.put(perk.getId().toLowerCase(), perk);
        
        perksByCategory.computeIfAbsent(perk.getCategory(), k -> new ArrayList<>())
                .add(perk);
        
        perksByTier.computeIfAbsent(perk.getTier(), k -> new ArrayList<>())
                .add(perk);
    }
    
    // ==================== Default Perks Creation ====================
    
    /**
     * Creates the default perk set organized by character class.
     * Classes: Warrior, Mage, Rogue, Builder, Miner, Gatherer
     * Each class has 2 perks per tier (0, 1, 2) = 6 perks per class = 36 total perks.
     */
    private void createDefaultPerks() {
        LOGGER.info("Creating default perks for all classes...");
        
        // =====================================================
        // WARRIOR CLASS - Combat focused melee fighter
        // =====================================================
        
        // --- TIER 0: Basic Warrior Perks ---
        registerPerk(Perk.builder()
                .id("warrior_combat_training")
                .displayName("Combat Training")
                .description("Basic combat training increases your melee damage.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("melee_damage_bonus", 0.08)
                .build());
        
        registerPerk(Perk.builder()
                .id("warrior_tough_skin")
                .displayName("Tough Skin")
                .description("Your skin hardens from battle experience.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("damage_reduction", 0.05)
                .additiveStat("Health", 10f)
                .build());
        
        // --- TIER 1: Intermediate Warrior Perks ---
        registerPerk(Perk.builder()
                .id("warrior_battle_fury")
                .displayName("Battle Fury")
                .description("Channel your rage into devastating attacks.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("warrior_combat_training")
                .gameplayModifier("melee_damage_bonus", 0.12)
                .gameplayModifier("critical_chance", 0.05)
                .gameplayModifier("attack_speed_bonus", 0.08)
                .build());
        
        registerPerk(Perk.builder()
                .id("warrior_iron_body")
                .displayName("Iron Body")
                .description("Your body becomes as tough as iron.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("warrior_tough_skin")
                .gameplayModifier("damage_reduction", 0.10)
                .additiveStat("Health", 25f)
                .gameplayModifier("knockback_resistance", 0.15)
                .build());
        
        // --- TIER 2: Advanced Warrior Perks ---
        registerPerk(Perk.builder()
                .id("warrior_berserker")
                .displayName("Berserker Rage")
                .description("Enter a berserker rage for massive damage at the cost of defense.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("warrior_battle_fury")
                .gameplayModifier("melee_damage_bonus", 0.25)
                .gameplayModifier("critical_chance", 0.15)
                .gameplayModifier("critical_damage", 0.50)
                .gameplayModifier("damage_reduction", -0.10)
                .build());
        
        registerPerk(Perk.builder()
                .id("warrior_juggernaut")
                .displayName("Juggernaut")
                .description("Become an unstoppable force on the battlefield.")
                .category(SkillCategory.COMBAT)
                .requiredClass("warrior")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("warrior_iron_body")
                .additiveStat("Health", 50f)
                .percentStat("Health", 0.15f)
                .gameplayModifier("damage_reduction", 0.20)
                .gameplayModifier("knockback_resistance", 0.40)
                .build());
        
        // =====================================================
        // MAGE CLASS - Magic and spell focused
        // =====================================================
        
        // --- TIER 0: Basic Mage Perks ---
        registerPerk(Perk.builder()
                .id("mage_arcane_initiate")
                .displayName("Arcane Initiate")
                .description("Begin to tap into arcane magical energies.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .additiveStat("Mana", 25f)
                .gameplayModifier("spell_damage_bonus", 0.05)
                .build());
        
        registerPerk(Perk.builder()
                .id("mage_mana_affinity")
                .displayName("Mana Affinity")
                .description("Your connection to mana grows stronger.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .additiveStat("Mana", 15f)
                .gameplayModifier("mana_regen_bonus", 0.10)
                .build());
        
        // --- TIER 1: Intermediate Mage Perks ---
        registerPerk(Perk.builder()
                .id("mage_elemental_mastery")
                .displayName("Elemental Mastery")
                .description("Master the elements for devastating spells.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("mage_arcane_initiate")
                .gameplayModifier("spell_damage_bonus", 0.15)
                .gameplayModifier("elemental_damage_bonus", 0.10)
                .additiveStat("Mana", 20f)
                .build());
        
        registerPerk(Perk.builder()
                .id("mage_mana_flow")
                .displayName("Mana Flow")
                .description("Improve your mana regeneration and efficiency.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("mage_mana_affinity")
                .additiveStat("Mana", 35f)
                .gameplayModifier("mana_regen_bonus", 0.20)
                .gameplayModifier("mana_cost_reduction", 0.10)
                .build());
        
        // --- TIER 2: Advanced Mage Perks ---
        registerPerk(Perk.builder()
                .id("mage_archmage")
                .displayName("Archmage")
                .description("Attain the rank of Archmage with incredible magical power.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("mage_elemental_mastery")
                .gameplayModifier("spell_damage_bonus", 0.30)
                .gameplayModifier("elemental_damage_bonus", 0.20)
                .gameplayModifier("spell_critical_chance", 0.15)
                .additiveStat("Mana", 30f)
                .build());
        
        registerPerk(Perk.builder()
                .id("mage_infinite_mana")
                .displayName("Infinite Mana")
                .description("Your mana pool becomes virtually limitless.")
                .category(SkillCategory.MAGIC)
                .requiredClass("mage")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("mage_mana_flow")
                .additiveStat("Mana", 75f)
                .percentStat("Mana", 0.25f)
                .gameplayModifier("mana_regen_bonus", 0.40)
                .gameplayModifier("mana_cost_reduction", 0.25)
                .build());
        
        // =====================================================
        // ROGUE CLASS - Stealth and agility focused
        // =====================================================
        
        // --- TIER 0: Basic Rogue Perks ---
        registerPerk(Perk.builder()
                .id("rogue_quick_reflexes")
                .displayName("Quick Reflexes")
                .description("Your reflexes sharpen, increasing dodge chance.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("dodge_chance", 0.08)
                .gameplayModifier("movement_speed_bonus", 0.05)
                .build());
        
        registerPerk(Perk.builder()
                .id("rogue_backstab")
                .displayName("Backstab")
                .description("Deal extra damage when attacking from behind.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("backstab_bonus", 0.15)
                .gameplayModifier("critical_chance", 0.05)
                .build());
        
        // --- TIER 1: Intermediate Rogue Perks ---
        registerPerk(Perk.builder()
                .id("rogue_shadow_step")
                .displayName("Shadow Step")
                .description("Move like a shadow, harder to detect.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("rogue_quick_reflexes")
                .gameplayModifier("dodge_chance", 0.12)
                .gameplayModifier("movement_speed_bonus", 0.10)
                .gameplayModifier("stealth_bonus", 0.15)
                .build());
        
        registerPerk(Perk.builder()
                .id("rogue_deadly_precision")
                .displayName("Deadly Precision")
                .description("Your strikes find weak points with deadly precision.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("rogue_backstab")
                .gameplayModifier("critical_chance", 0.12)
                .gameplayModifier("critical_damage", 0.25)
                .gameplayModifier("backstab_bonus", 0.10)
                .build());
        
        // --- TIER 2: Advanced Rogue Perks ---
        registerPerk(Perk.builder()
                .id("rogue_phantom")
                .displayName("Phantom")
                .description("Become nearly invisible, striking from the shadows.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("rogue_shadow_step")
                .gameplayModifier("dodge_chance", 0.20)
                .gameplayModifier("stealth_bonus", 0.30)
                .gameplayModifier("movement_speed_bonus", 0.15)
                .gameplayModifier("invisibility_duration", 0.25)
                .build());
        
        registerPerk(Perk.builder()
                .id("rogue_assassin")
                .displayName("Assassin")
                .description("Master the art of the lethal strike.")
                .category(SkillCategory.COMBAT)
                .requiredClass("rogue")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("rogue_deadly_precision")
                .gameplayModifier("critical_chance", 0.20)
                .gameplayModifier("critical_damage", 0.75)
                .gameplayModifier("backstab_bonus", 0.40)
                .gameplayModifier("execute_threshold", 0.15)
                .build());
        
        // =====================================================
        // BUILDER CLASS - Construction and crafting focused
        // =====================================================
        
        // --- TIER 0: Basic Builder Perks ---
        registerPerk(Perk.builder()
                .id("builder_foundation")
                .displayName("Foundation")
                .description("Learn the basics of efficient building.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("building_xp_bonus", 0.10)
                .gameplayModifier("build_speed_bonus", 0.05)
                .build());
        
        registerPerk(Perk.builder()
                .id("builder_craftsman")
                .displayName("Craftsman")
                .description("Your crafted items are of higher quality.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("crafting_xp_bonus", 0.10)
                .gameplayModifier("crafting_quality_bonus", 0.08)
                .build());
        
        // --- TIER 1: Intermediate Builder Perks ---
        registerPerk(Perk.builder()
                .id("builder_architect")
                .displayName("Architect")
                .description("Design and build structures with greater efficiency.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("builder_foundation")
                .gameplayModifier("building_xp_bonus", 0.20)
                .gameplayModifier("build_speed_bonus", 0.15)
                .gameplayModifier("block_placement_range", 0.10)
                .build());
        
        registerPerk(Perk.builder()
                .id("builder_mastersmith")
                .displayName("Master Smith")
                .description("Your smithing skills produce exceptional items.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("builder_craftsman")
                .gameplayModifier("crafting_xp_bonus", 0.15)
                .gameplayModifier("crafting_quality_bonus", 0.15)
                .gameplayModifier("material_efficiency", 0.10)
                .build());
        
        // --- TIER 2: Advanced Builder Perks ---
        registerPerk(Perk.builder()
                .id("builder_grand_architect")
                .displayName("Grand Architect")
                .description("Build magnificent structures with unparalleled speed.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("builder_architect")
                .gameplayModifier("building_xp_bonus", 0.35)
                .gameplayModifier("build_speed_bonus", 0.30)
                .gameplayModifier("block_placement_range", 0.25)
                .gameplayModifier("instant_build_chance", 0.10)
                .build());
        
        registerPerk(Perk.builder()
                .id("builder_legendary_crafter")
                .displayName("Legendary Crafter")
                .description("Create legendary quality items with ease.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("builder")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("builder_mastersmith")
                .gameplayModifier("crafting_xp_bonus", 0.30)
                .gameplayModifier("crafting_quality_bonus", 0.30)
                .gameplayModifier("material_efficiency", 0.25)
                .gameplayModifier("legendary_craft_chance", 0.08)
                .build());
        
        // =====================================================
        // MINER CLASS - Mining and excavation focused
        // =====================================================
        
        // --- TIER 0: Basic Miner Perks ---
        registerPerk(Perk.builder()
                .id("miner_apprentice")
                .displayName("Mining Apprentice")
                .description("Begin your journey as a miner.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("mining_xp_bonus", 0.10)
                .gameplayModifier("mining_speed_bonus", 0.05)
                .build());
        
        registerPerk(Perk.builder()
                .id("miner_ore_detector")
                .displayName("Ore Detector")
                .description("Develop a sense for finding valuable ores.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("ore_detection_range", 0.10)
                .gameplayModifier("rare_ore_chance", 0.05)
                .build());
        
        // --- TIER 1: Intermediate Miner Perks ---
        registerPerk(Perk.builder()
                .id("miner_efficient_extraction")
                .displayName("Efficient Extraction")
                .description("Extract resources more efficiently from the earth.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("miner_apprentice")
                .gameplayModifier("mining_xp_bonus", 0.18)
                .gameplayModifier("mining_speed_bonus", 0.12)
                .gameplayModifier("double_drop_chance", 0.08)
                .build());
        
        registerPerk(Perk.builder()
                .id("miner_prospector")
                .displayName("Prospector")
                .description("Find rare and valuable deposits with ease.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("miner_ore_detector")
                .gameplayModifier("ore_detection_range", 0.25)
                .gameplayModifier("rare_ore_chance", 0.15)
                .gameplayModifier("gem_find_bonus", 0.12)
                .build());
        
        // --- TIER 2: Advanced Miner Perks ---
        registerPerk(Perk.builder()
                .id("miner_master")
                .displayName("Master Miner")
                .description("Achieve mastery over the art of mining.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("miner_efficient_extraction")
                .gameplayModifier("mining_xp_bonus", 0.30)
                .gameplayModifier("mining_speed_bonus", 0.25)
                .gameplayModifier("double_drop_chance", 0.18)
                .gameplayModifier("fortune_bonus", 0.15)
                .additiveStat("Stamina", 15f)
                .build());
        
        registerPerk(Perk.builder()
                .id("miner_deep_delver")
                .displayName("Deep Delver")
                .description("Master of finding treasures in the deepest caves.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("miner")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("miner_prospector")
                .gameplayModifier("ore_detection_range", 0.50)
                .gameplayModifier("rare_ore_chance", 0.30)
                .gameplayModifier("gem_find_bonus", 0.25)
                .gameplayModifier("ancient_artifact_chance", 0.05)
                .build());
        
        // =====================================================
        // GATHERER CLASS - Resource gathering and farming
        // =====================================================
        
        // --- TIER 0: Basic Gatherer Perks ---
        registerPerk(Perk.builder()
                .id("gatherer_forager")
                .displayName("Forager")
                .description("Learn the basics of foraging for resources.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("gathering_xp_bonus", 0.10)
                .gameplayModifier("gather_speed_bonus", 0.05)
                .build());
        
        registerPerk(Perk.builder()
                .id("gatherer_green_thumb")
                .displayName("Green Thumb")
                .description("Plants grow faster and yield more for you.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(0)
                .requiredLevel(1)
                .skillPointCost(1)
                .gameplayModifier("farming_xp_bonus", 0.10)
                .gameplayModifier("crop_yield_bonus", 0.08)
                .build());
        
        // --- TIER 1: Intermediate Gatherer Perks ---
        registerPerk(Perk.builder()
                .id("gatherer_naturalist")
                .displayName("Naturalist")
                .description("Your connection to nature increases gathering efficiency.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("gatherer_forager")
                .gameplayModifier("gathering_xp_bonus", 0.18)
                .gameplayModifier("gather_speed_bonus", 0.12)
                .gameplayModifier("rare_resource_chance", 0.10)
                .build());
        
        registerPerk(Perk.builder()
                .id("gatherer_farmer")
                .displayName("Expert Farmer")
                .description("Your farming expertise produces bountiful harvests.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(1)
                .requiredLevel(5)
                .skillPointCost(2)
                .prerequisite("gatherer_green_thumb")
                .gameplayModifier("farming_xp_bonus", 0.18)
                .gameplayModifier("crop_yield_bonus", 0.20)
                .gameplayModifier("crop_growth_speed", 0.15)
                .build());
        
        // --- TIER 2: Advanced Gatherer Perks ---
        registerPerk(Perk.builder()
                .id("gatherer_nature_master")
                .displayName("Nature Master")
                .description("Command nature itself to provide abundant resources.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("gatherer_naturalist")
                .gameplayModifier("gathering_xp_bonus", 0.30)
                .gameplayModifier("gather_speed_bonus", 0.25)
                .gameplayModifier("rare_resource_chance", 0.25)
                .gameplayModifier("resource_respawn_bonus", 0.20)
                .build());
        
        registerPerk(Perk.builder()
                .id("gatherer_harvest_lord")
                .displayName("Harvest Lord")
                .description("Your harvests are legendary in their abundance.")
                .category(SkillCategory.UTILITIES)
                .requiredClass("gatherer")
                .tier(2)
                .requiredLevel(10)
                .skillPointCost(3)
                .prerequisite("gatherer_farmer")
                .gameplayModifier("farming_xp_bonus", 0.30)
                .gameplayModifier("crop_yield_bonus", 0.40)
                .gameplayModifier("crop_growth_speed", 0.30)
                .gameplayModifier("super_crop_chance", 0.10)
                .build());
        
        LOGGER.info("Created " + perks.size() + " default perks for 6 classes");
    }
    
    // ==================== Player Perk Data ====================
    
    /**
     * Load a player's unlocked perks from their JSON file.
     */
    public void loadPlayerPerks(UUID playerId) {
        Path playerFile = playerPerksDir.resolve(playerId.toString() + ".json");
        
        if (Files.exists(playerFile)) {
            try (Reader reader = Files.newBufferedReader(playerFile)) {
                PlayerPerkData data = GSON.fromJson(reader, PlayerPerkData.class);
                if (data != null) {
                    playerPerks.put(playerId, ConcurrentHashMap.newKeySet());
                    playerPerks.get(playerId).addAll(data.unlockedPerks);
                    playerPerkPoints.put(playerId, data.perkPoints);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load perks for player " + playerId, e);
            }
        } else {
            // Initialize new player
            playerPerks.put(playerId, ConcurrentHashMap.newKeySet());
            playerPerkPoints.put(playerId, 0);
        }
    }
    
    /**
     * Save a player's unlocked perks to their JSON file.
     */
    public void savePlayerPerks(UUID playerId) {
        Path playerFile = playerPerksDir.resolve(playerId.toString() + ".json");
        
        try (Writer writer = Files.newBufferedWriter(playerFile)) {
            PlayerPerkData data = new PlayerPerkData();
            data.playerId = playerId.toString();
            data.unlockedPerks = new ArrayList<>(getUnlockedPerks(playerId));
            data.perkPoints = getPerkPoints(playerId);
            data.lastUpdated = System.currentTimeMillis();
            GSON.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save perks for player " + playerId, e);
        }
    }
    
    /**
     * Player perk data structure for JSON serialization.
     */
    private static class PlayerPerkData {
        @SuppressWarnings("unused")
        String playerId;
        List<String> unlockedPerks = new ArrayList<>();
        int perkPoints = 0;
        @SuppressWarnings("unused")
        long lastUpdated;
    }
    
    // ==================== Perk Points ====================
    
    public int getPerkPoints(UUID playerId) {
        return playerPerkPoints.getOrDefault(playerId, 0);
    }
    
    public void setPerkPoints(UUID playerId, int points) {
        playerPerkPoints.put(playerId, Math.max(0, points));
    }
    
    public void addPerkPoints(UUID playerId, int points) {
        setPerkPoints(playerId, getPerkPoints(playerId) + points);
    }
    
    public boolean spendPerkPoints(UUID playerId, int points) {
        int current = getPerkPoints(playerId);
        if (current >= points) {
            setPerkPoints(playerId, current - points);
            return true;
        }
        return false;
    }
    
    // ==================== Perk Unlocking ====================
    
    /**
     * Attempts to unlock a perk for a player.
     * 
     * @return true if successful, false otherwise
     */
    public boolean unlockPerk(UUID playerId, String perkId, PlayerRPGData playerData) {
        Perk perk = getPerk(perkId);
        if (perk == null) {
            LOGGER.fine("Perk not found: " + perkId);
            return false;
        }
        
        // Check if already unlocked
        if (hasUnlockedPerk(playerId, perkId)) {
            LOGGER.fine("Perk already unlocked: " + perkId);
            return false;
        }
        
        // Check level requirement
        if (playerData != null && playerData.getLevel() < perk.getRequiredLevel()) {
            LOGGER.fine("Level too low for perk: " + perkId);
            return false;
        }
        
        // Check perk points
        if (getPerkPoints(playerId) < perk.getSkillPointCost()) {
            LOGGER.fine("Not enough perk points for: " + perkId);
            return false;
        }
        
        // Check prerequisites
        for (String prereq : perk.getPrerequisitePerks()) {
            if (!hasUnlockedPerk(playerId, prereq)) {
                LOGGER.fine("Missing prerequisite perk: " + prereq);
                return false;
            }
        }
        
        // Deduct points and unlock
        spendPerkPoints(playerId, perk.getSkillPointCost());
        playerPerks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(perkId.toLowerCase());
        
        LOGGER.info("Player " + playerId + " unlocked perk: " + perkId);
        return true;
    }
    
    /**
     * Force unlock a perk without checking requirements.
     */
    public void forceUnlockPerk(UUID playerId, String perkId) {
        playerPerks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(perkId.toLowerCase());
    }
    
    /**
     * Check if a player has unlocked a perk.
     */
    public boolean hasUnlockedPerk(UUID playerId, String perkId) {
        Set<String> unlocked = playerPerks.get(playerId);
        return unlocked != null && unlocked.contains(perkId.toLowerCase());
    }
    
    /**
     * Get all unlocked perk IDs for a player.
     */
    public Set<String> getUnlockedPerks(UUID playerId) {
        return playerPerks.getOrDefault(playerId, Collections.emptySet());
    }
    
    /**
     * Get all unlocked Perk objects for a player.
     */
    public List<Perk> getUnlockedPerkObjects(UUID playerId) {
        return getUnlockedPerks(playerId).stream()
                .map(this::getPerk)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    // ==================== Perk Effect Calculation ====================
    
    /**
     * Calculate total gameplay modifier value from all unlocked perks.
     * 
     * @param playerId the player
     * @param modifierKey the gameplay modifier key (e.g., "damage_bonus")
     * @return total modifier value (additive)
     */
    public double getTotalGameplayModifier(UUID playerId, String modifierKey) {
        return getUnlockedPerkObjects(playerId).stream()
                .mapToDouble(p -> p.getGameplayModifier(modifierKey))
                .sum();
    }
    
    /**
     * Get all stat modifiers from unlocked perks for applying to Hytale's EntityStatMap.
     * 
     * @param playerId the player
     * @return map of stat name -> list of modifiers
     */
    public Map<String, List<Perk.PerkModifier>> getAllStatModifiers(UUID playerId) {
        Map<String, List<Perk.PerkModifier>> result = new HashMap<>();
        
        for (Perk perk : getUnlockedPerkObjects(playerId)) {
            for (Map.Entry<String, Perk.PerkModifier> entry : perk.getStatModifiers().entrySet()) {
                result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }
        
        return result;
    }
    
    // ==================== Perk Registry Queries ====================
    
    public Perk getPerk(String perkId) {
        return perkId != null ? perks.get(perkId.toLowerCase()) : null;
    }
    
    public Collection<Perk> getAllPerks() {
        return Collections.unmodifiableCollection(perks.values());
    }
    
    public List<Perk> getPerksByCategory(SkillCategory category) {
        return perksByCategory.getOrDefault(category, Collections.emptyList());
    }
    
    public List<Perk> getPerksByTier(int tier) {
        return perksByTier.getOrDefault(tier, Collections.emptyList());
    }
    
    /**
     * Get perks available for a player to unlock (meets requirements).
     */
    public List<Perk> getAvailablePerks(UUID playerId, PlayerRPGData playerData) {
        int playerLevel = playerData != null ? playerData.getLevel() : 1;
        int perkPoints = getPerkPoints(playerId);
        Set<String> unlocked = getUnlockedPerks(playerId);
        
        return perks.values().stream()
                .filter(p -> !unlocked.contains(p.getId().toLowerCase()))
                .filter(p -> playerLevel >= p.getRequiredLevel())
                .filter(p -> perkPoints >= p.getSkillPointCost())
                .filter(p -> p.getPrerequisitePerks().stream()
                        .allMatch(prereq -> unlocked.contains(prereq.toLowerCase())))
                .collect(Collectors.toList());
    }
    
    // ==================== Reload ====================
    
    public void reload() {
        loadPerks();
        LOGGER.info("PerkManager reloaded");
    }
}
