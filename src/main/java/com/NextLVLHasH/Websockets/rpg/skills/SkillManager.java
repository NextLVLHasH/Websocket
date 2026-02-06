package com.NextLVLHasH.Websockets.rpg.skills;

import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.combat.CombatManager;
import com.NextLVLHasH.Websockets.rpg.combat.Damage;
import com.NextLVLHasH.Websockets.rpg.combat.DamageType;
import com.NextLVLHasH.Websockets.rpg.combat.EffectInstance;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for all skills and skill trees in the RPG system.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Loading skill and skill tree definitions from configuration files</li>
 *   <li>Managing player skill unlocks</li>
 *   <li>Tracking skill cooldowns per player</li>
 *   <li>Validating and executing skill activations</li>
 * </ul>
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SkillManager manager = SkillManager.getInstance();
 * manager.loadSkills();
 * manager.loadSkillTrees();
 * 
 * // Unlock a skill for a player
 * manager.unlockSkill(playerId, "fireball", playerData);
 * 
 * // Activate a skill
 * SkillResult result = manager.activateSkill(playerId, "fireball", playerData, target);
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
@SuppressWarnings("unused")
public class SkillManager {

    private static final Logger LOGGER = Logger.getLogger(SkillManager.class.getName());

    // ==================== Singleton ====================

    private static volatile SkillManager instance;
    private static final Object LOCK = new Object();

    /**
     * Gets the singleton instance of the SkillManager.
     *
     * @return the SkillManager instance
     */
    public static SkillManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SkillManager();
                }
            }
        }
        return instance;
    }

    // ==================== Configuration ====================

    private static final String DEFAULT_SKILLS_PATH = "config/skills/";
    private static final String DEFAULT_SKILL_TREES_PATH = "config/skill_trees/";

    private String skillsPath = DEFAULT_SKILLS_PATH;
    private String skillTreesPath = DEFAULT_SKILL_TREES_PATH;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // ==================== Data Storage ====================

    /**
     * Map of skill ID to Skill definition.
     */
    private final Map<String, Skill> skills;

    /**
     * Map of skill tree ID to SkillTree definition.
     */
    private final Map<String, SkillTree> skillTrees;

    /**
     * Map of associated class name to SkillTree for fast lookup by class.
     */
    private final Map<String, SkillTree> skillTreesByClass;

    /**
     * Map of player UUID to their cooldown tracker.
     */
    private final Map<UUID, SkillCooldown> cooldowns;

    /**
     * Map of player UUID to their set of unlocked skill IDs.
     */
    private final Map<UUID, Set<String>> unlockedSkills;

    // ==================== Constructor ====================

    /**
     * Private constructor for singleton pattern.
     */
    private SkillManager() {
        this.skills = new ConcurrentHashMap<>();
        this.skillTrees = new ConcurrentHashMap<>();
        this.skillTreesByClass = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.unlockedSkills = new ConcurrentHashMap<>();
    }
    
    /**
     * Initializes the SkillManager with the given data path.
     * Sets the config paths and loads all skills and skill trees.
     *
     * @param dataPath the base data directory
     */
    public void initialize(Path dataPath) {
        this.skillsPath = dataPath.resolve("config/skills/").toString();
        this.skillTreesPath = dataPath.resolve("config/skill_trees/").toString();
        
        try {
            loadSkills();
            loadSkillTrees();
            LOGGER.info("SkillManager initialized with " + skills.size() + " skills and " + skillTrees.size() + " skill trees");
        } catch (Exception e) {
            LOGGER.warning("Failed to load skills: " + e.getMessage());
        }
    }

    // ==================== Configuration Methods ====================

    /**
     * Sets the path to load skills from.
     *
     * @param path the skills configuration path
     */
    public void setSkillsPath(String path) {
        this.skillsPath = path != null ? path : DEFAULT_SKILLS_PATH;
    }

    /**
     * Sets the path to load skill trees from.
     *
     * @param path the skill trees configuration path
     */
    public void setSkillTreesPath(String path) {
        this.skillTreesPath = path != null ? path : DEFAULT_SKILL_TREES_PATH;
    }

    // ==================== Loading Methods ====================

    /**
     * Loads all skill definitions from the configured skills directory.
     * <p>
     * This method scans the skills directory for JSON files and loads each
     * as a Skill definition. Existing skills are cleared before loading.
     * </p>
     *
     * @return the number of skills loaded
     */
    public int loadSkills() {
        skills.clear();
        Path skillsDir = Paths.get(skillsPath);

        // Add potential runtime mod config directory in AppData (Hytale saves mods path)
        Path appdata = null;
        try {
            String app = System.getenv("APPDATA");
            if (app != null && !app.isEmpty()) appdata = Paths.get(app);
        } catch (Exception ignored) {}

        Path fallbackSkills = null;
        if (appdata != null) {
            fallbackSkills = appdata.resolve(Paths.get("Hytale", "UserData", "Saves", "my world", "mods", "com.NextLVLHasH_WebsocketNotifications", "config", "skills"));
        }

        // Ensure primary directory exists
        if (!Files.exists(skillsDir)) {
            LOGGER.warning("Skills directory does not exist: " + skillsDir.toAbsolutePath());
            try {
                Files.createDirectories(skillsDir);
                LOGGER.info("Created skills directory: " + skillsDir.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create skills directory", e);
            }
        }

        // Copy defaults if primary dir is empty
        if (isDirectoryEmpty(skillsDir)) {
            copyDefaultSkills(skillsDir);
        }

        int loaded = 0;
        List<Path> dirsToScan = new ArrayList<>();
        dirsToScan.add(skillsDir);
        if (fallbackSkills != null && !fallbackSkills.equals(skillsDir)) dirsToScan.add(fallbackSkills);

        for (Path dir : dirsToScan) {
            LOGGER.info("Scanning skills directory: " + dir.toAbsolutePath());
            if (!Files.exists(dir)) {
                LOGGER.fine("Directory does not exist, skipping: " + dir.toAbsolutePath());
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : stream) {
                    try {
                        Skill skill = loadSkillFromFile(file);
                        if (skill != null) {
                            skills.put(skill.getId().toLowerCase(), skill);
                            loaded++;
                            LOGGER.info("Loaded skill: " + skill.getId() + " from " + file.toAbsolutePath());
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to load skill from: " + file, e);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read skills directory: " + dir.toAbsolutePath(), e);
            }
        }

        LOGGER.info("Loaded " + loaded + " skills (scanned " + dirsToScan.size() + " dirs)");
        return loaded;
    }

    /**
     * Loads a single skill from a JSON file.
     *
     * @param file the path to the skill JSON file
     * @return the loaded Skill, or null if loading failed
     */
    private Skill loadSkillFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return Skill.fromJson(reader);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse skill file: " + file, e);
            return null;
        }
    }

    /**
     * Loads all skill tree definitions from the configured skill trees directory.
     * <p>
     * This method scans the skill trees directory for JSON files and loads each
     * as a SkillTree definition. Existing skill trees are cleared before loading.
     * </p>
     *
     * @return the number of skill trees loaded
     */
    public int loadSkillTrees() {
        skillTrees.clear();
        skillTreesByClass.clear();
        Path treesDir = Paths.get(skillTreesPath);

        if (!Files.exists(treesDir)) {
            LOGGER.warning("Skill trees directory does not exist: " + treesDir.toAbsolutePath());
            try {
                Files.createDirectories(treesDir);
                LOGGER.info("Created skill trees directory: " + treesDir.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create skill trees directory", e);
            }
        }
        
        // Check if directory is empty and copy defaults
        if (isDirectoryEmpty(treesDir)) {
            copyDefaultSkillTrees(treesDir);
        }

        int loaded = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(treesDir, "*.json")) {
            for (Path file : stream) {
                try {
                    SkillTree tree = loadSkillTreeFromFile(file);
                    if (tree != null) {
                        String treeKey = tree.getId() != null ? tree.getId().toLowerCase() : null;
                        if (treeKey != null) skillTrees.put(treeKey, tree);
                        // also index by associated class for easy lookup
                        String assoc = tree.getAssociatedClass();
                        if (assoc != null && !assoc.isBlank()) {
                            skillTreesByClass.put(assoc.toLowerCase(), tree);
                        }
                        loaded++;
                        LOGGER.info("Loaded skill tree: " + tree.getId() + " (class: " + tree.getAssociatedClass() + ", nodes: " + tree.getNodeCount() + ")");
                    } else {
                        LOGGER.warning("Skill tree was null after parsing: " + file);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load skill tree from: " + file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read skill trees directory", e);
        }

        LOGGER.info("Loaded " + loaded + " skill trees from " + treesDir.toAbsolutePath());
        return loaded;
    }

    /**\n     * Loads a single skill tree from a JSON file.\n     *\n     * @param file the path to the skill tree JSON file\n     * @return the loaded SkillTree, or null if loading failed\n     */
    private SkillTree loadSkillTreeFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            LOGGER.info("Parsing skill tree file: " + file.toAbsolutePath());
            SkillTree tree = SkillTree.fromJson(reader);
            if (tree == null) {
                LOGGER.warning("SkillTree.fromJson returned null for: " + file);
            }
            return tree;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse skill tree file: " + file + " - " + e.getMessage(), e);
            return null;
        }
    }
    
    // ==================== Default Config Methods ====================
    
    /**
     * Checks if a directory is empty or doesn't contain any JSON files.
     */
    private boolean isDirectoryEmpty(Path dir) {
        if (!Files.exists(dir)) return true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return true;
        }
    }
    
    /**
     * Copy default skill configs from resources to the target directory.
     */
    private void copyDefaultSkills(Path targetDir) {
        String[] defaultSkills = {
            "warrior_power_strike.json",
            "warrior_shield_bash.json",
            "mage_fireball.json",
            "mage_frost_bolt.json",
            "rogue_shadowstrike.json",
            "rogue_vanish.json",
            "builder_quick_build.json",
            "builder_repair.json",
            "miner_prospecting.json",
            "miner_earthquake.json",
            "gatherer_harvest_touch.json",
            "gatherer_nature_blessing.json"
        };
        
        for (String skillFile : defaultSkills) {
            copyResourceFile("config/skills/" + skillFile, targetDir.resolve(skillFile));
        }
        LOGGER.info("Copied " + defaultSkills.length + " default skill configs");
    }
    
    /**
     * Copy default skill tree configs from resources to the target directory.
     */
    private void copyDefaultSkillTrees(Path targetDir) {
        String[] defaultTrees = {
            "warrior_combat_tree.json",
            "mage_elemental_tree.json",
            "rogue_shadow_tree.json",
            "builder_construction_tree.json",
            "miner_excavation_tree.json",
            "gatherer_harvest_tree.json"
        };
        
        for (String treeFile : defaultTrees) {
            copyResourceFile("config/skill_trees/" + treeFile, targetDir.resolve(treeFile));
        }
        LOGGER.info("Copied " + defaultTrees.length + " default skill tree configs");
    }
    
    /**
     * Copy a file from resources to the filesystem.
     */
    private void copyResourceFile(String resourcePath, Path targetPath) {
        LOGGER.info("Attempting to copy resource: " + resourcePath + " to " + targetPath.toAbsolutePath());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Copied default config: " + targetPath.getFileName());
            } else {
                LOGGER.warning("Resource not found in classpath: " + resourcePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to copy default config: " + resourcePath, e);
        }
    }

    /**
     * Reloads all skills and skill trees from configuration.
     *
     * @return an array of [skillsLoaded, treesLoaded]
     */
    public int[] reload() {
        int skillsLoaded = loadSkills();
        int treesLoaded = loadSkillTrees();
        return new int[]{skillsLoaded, treesLoaded};
    }

    // ==================== Skill Registry Methods ====================

    /**
     * Gets a skill by its ID.
     *
     * @param skillId the unique identifier of the skill
     * @return the Skill, or null if not found
     */
    public Skill getSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return null;
        }
        return skills.get(skillId.toLowerCase());
    }

    /**
     * Gets a skill tree by its ID.
     *
     * @param treeId the unique identifier of the skill tree
     * @return the SkillTree, or null if not found
     */
    public SkillTree getSkillTree(String treeId) {
        if (treeId == null || treeId.isEmpty()) {
            return null;
        }
        return skillTrees.get(treeId.toLowerCase());
    }

    /**
     * Gets a skill tree by its associated class.
     * Searches all skill trees for one with matching associated_class.
     *
     * @param className the class name to find a tree for
     * @return the SkillTree, or null if not found
     */
    public SkillTree getSkillTreeByClass(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        String classLower = className.toLowerCase();
        // Fast path: check class->tree index
        SkillTree byClass = skillTreesByClass.get(classLower);
        if (byClass != null) return byClass;
        // Fallback: search trees for matching associated_class
        for (SkillTree tree : skillTrees.values()) {
            String assoc = tree.getAssociatedClass();
            if (assoc != null && classLower.equals(assoc.toLowerCase())) {
                // populate index for future lookups
                skillTreesByClass.put(classLower, tree);
                return tree;
            }
        }
        // Final fallback: try direct lookup by class name as tree ID
        return skillTrees.get(classLower);
    }

    /**
     * Checks if a skill exists.
     *
     * @param skillId the skill ID to check
     * @return true if the skill exists
     */
    public boolean skillExists(String skillId) {
        return getSkill(skillId) != null;
    }

    /**
     * Gets all registered skills.
     *
     * @return an unmodifiable collection of all skills
     */
    public Collection<Skill> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }

    /**
     * Gets all registered skill trees.
     *
     * @return an unmodifiable collection of all skill trees
     */
    public Collection<SkillTree> getAllSkillTrees() {
        return Collections.unmodifiableCollection(skillTrees.values());
    }

    /**
     * Registers a skill programmatically.
     *
     * @param skill the skill to register
     */
    public void registerSkill(Skill skill) {
        if (skill != null && skill.getId() != null) {
            skills.put(skill.getId().toLowerCase(), skill);
        }
    }

    /**
     * Registers a skill tree programmatically.
     *
     * @param tree the skill tree to register
     */
    public void registerSkillTree(SkillTree tree) {
        if (tree != null && tree.getId() != null) {
            skillTrees.put(tree.getId().toLowerCase(), tree);
            if (tree.getAssociatedClass() != null && !tree.getAssociatedClass().isBlank()) {
                skillTreesByClass.put(tree.getAssociatedClass().toLowerCase(), tree);
            }
        }
    }

    /**
     * Unregisters a skill.
     *
     * @param skillId the ID of the skill to unregister
     */
    public void unregisterSkill(String skillId) {
        if (skillId != null) {
            skills.remove(skillId.toLowerCase());
        }
    }

    /**
     * Unregisters a skill tree.
     *
     * @param treeId the ID of the skill tree to unregister
     */
    public void unregisterSkillTree(String treeId) {
        if (treeId != null) {
            SkillTree removed = skillTrees.remove(treeId.toLowerCase());
            if (removed != null && removed.getAssociatedClass() != null) {
                skillTreesByClass.remove(removed.getAssociatedClass().toLowerCase(), removed);
            }
        }
    }

    // ==================== Skill Unlock Methods ====================

    /**
     * Attempts to unlock a skill for a player.
     * <p>
     * This method checks:
     * <ul>
     *   <li>The skill exists</li>
     *   <li>Player has sufficient level</li>
     *   <li>Player has sufficient skill points</li>
     *   <li>Player has all prerequisite skills</li>
     * </ul>
     * If all checks pass, the skill is unlocked and skill points are deducted.
     * </p>
     *
     * @param playerId   the UUID of the player
     * @param skillId    the ID of the skill to unlock
     * @param playerData the player's RPG data
     * @return true if the skill was successfully unlocked
     */
    public boolean unlockSkill(UUID playerId, String skillId, PlayerRPGData playerData) {
        if (playerId == null || skillId == null || playerData == null) {
            return false;
        }

        // Check if skill exists
        Skill skill = getSkill(skillId);
        if (skill == null) {
            LOGGER.fine("Skill not found: " + skillId);
            return false;
        }

        // Check if already unlocked
        if (hasUnlockedSkill(playerId, skillId)) {
            LOGGER.fine("Skill already unlocked: " + skillId);
            return false;
        }

        // Check level requirement
        if (playerData.getLevel() < skill.getRequiredLevel()) {
            LOGGER.fine("Level too low for skill: " + skillId);
            return false;
        }

        // Check skill point cost
        if (playerData.getSkillPoints() < skill.getSkillPointCost()) {
            LOGGER.fine("Not enough skill points for: " + skillId);
            return false;
        }

        // Check prerequisites
        for (String prereq : skill.getPrerequisiteSkills()) {
            if (!hasUnlockedSkill(playerId, prereq)) {
                LOGGER.fine("Missing prerequisite skill: " + prereq);
                return false;
            }
        }

        // Deduct skill points
        if (skill.getSkillPointCost() > 0) {
            playerData.setSkillPoints(playerData.getSkillPoints() - skill.getSkillPointCost());
        }

        // Unlock the skill
        unlockedSkills.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(skillId.toLowerCase());

        LOGGER.info("Player " + playerId + " unlocked skill: " + skillId);
        return true;
    }

    /**
     * Force unlocks a skill for a player without checking requirements.
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill to unlock
     */
    public void forceUnlockSkill(UUID playerId, String skillId) {
        if (playerId != null && skillId != null && !skillId.isEmpty()) {
            unlockedSkills.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                    .add(skillId.toLowerCase());
        }
    }

    /**
     * Locks (removes) a skill from a player.
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill to lock
     */
    public void lockSkill(UUID playerId, String skillId) {
        if (playerId != null && skillId != null) {
            Set<String> playerSkills = unlockedSkills.get(playerId);
            if (playerSkills != null) {
                playerSkills.remove(skillId.toLowerCase());
            }
        }
    }

    /**
     * Checks if a player has unlocked a specific skill.
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill
     * @return true if the skill is unlocked
     */
    public boolean hasUnlockedSkill(UUID playerId, String skillId) {
        if (playerId == null || skillId == null) {
            return false;
        }
        Set<String> playerSkills = unlockedSkills.get(playerId);
        return playerSkills != null && playerSkills.contains(skillId.toLowerCase());
    }

    /**
     * Gets all skills unlocked by a player.
     *
     * @param playerId the UUID of the player
     * @return an unmodifiable set of unlocked skill IDs
     */
    public Set<String> getUnlockedSkills(UUID playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }
        Set<String> playerSkills = unlockedSkills.get(playerId);
        if (playerSkills == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(playerSkills));
    }

    // ==================== Cooldown Methods ====================

    /**
     * Gets the cooldown tracker for a player, creating one if it doesn't exist.
     *
     * @param playerId the UUID of the player
     * @return the player's SkillCooldown tracker
     */
    public SkillCooldown getCooldowns(UUID playerId) {
        if (playerId == null) {
            return new SkillCooldown();
        }
        return cooldowns.computeIfAbsent(playerId, k -> new SkillCooldown());
    }

    /**
     * Checks if a skill is on cooldown for a player.
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill
     * @return true if the skill is on cooldown
     */
    public boolean isSkillOnCooldown(UUID playerId, String skillId) {
        return getCooldowns(playerId).isOnCooldown(skillId);
    }

    /**
     * Gets the remaining cooldown for a skill.
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill
     * @return the remaining cooldown in milliseconds
     */
    public long getRemainingCooldown(UUID playerId, String skillId) {
        return getCooldowns(playerId).getRemainingCooldown(skillId);
    }

    // ==================== Skill Activation Methods ====================

    /**
     * Checks if a player can activate a skill.
     * <p>
     * This performs all validation checks without actually activating the skill.
     * </p>
     *
     * @param playerId   the UUID of the player
     * @param skillId    the ID of the skill
     * @param playerData the player's RPG data
     * @return a SkillResult indicating if activation is possible
     */
    public SkillResult canActivateSkill(UUID playerId, String skillId, PlayerRPGData playerData) {
        if (playerId == null || skillId == null || playerData == null) {
            return SkillResult.failed("Invalid parameters");
        }

        // Check if skill exists
        Skill skill = getSkill(skillId);
        if (skill == null) {
            return SkillResult.notFound(skillId);
        }

        // Passive skills cannot be activated
        if (skill.getType() == SkillType.PASSIVE) {
            return SkillResult.failed("Passive skills cannot be activated");
        }

        // Check if skill is unlocked
        if (!hasUnlockedSkill(playerId, skillId)) {
            return SkillResult.notUnlocked(skillId);
        }

        // Check level requirement
        if (playerData.getLevel() < skill.getRequiredLevel()) {
            return SkillResult.levelTooLow(skill.getRequiredLevel(), playerData.getLevel());
        }

        // Check permissions
        if (!skill.getRequiredPermissions().isEmpty()) {
            if (!SkillPermission.hasAllPermissions(playerId, skill.getRequiredPermissions())) {
                return SkillResult.missingPermission();
            }
        }

        // Check cooldown
        SkillCooldown playerCooldowns = getCooldowns(playerId);
        if (playerCooldowns.isOnCooldown(skillId)) {
            return SkillResult.onCooldown(skillId, playerCooldowns.getRemainingCooldown(skillId));
        }

        // Check mana cost
        if (skill.getManaCost() > 0 && playerData.getCurrentMana() < skill.getManaCost()) {
            double deficit = skill.getManaCost() - playerData.getCurrentMana();
            return SkillResult.insufficientMana(skillId, deficit);
        }

        // Check stamina cost
        if (skill.getStaminaCost() > 0 && playerData.getCurrentStamina() < skill.getStaminaCost()) {
            double deficit = skill.getStaminaCost() - playerData.getCurrentStamina();
            return SkillResult.insufficientStamina(skillId, deficit);
        }

        return SkillResult.success();
    }

    /**
     * Attempts to activate a skill for a player.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates all requirements</li>
     *   <li>Deducts resource costs (mana/stamina)</li>
     *   <li>Starts the cooldown</li>
     *   <li>Executes the skill effect</li>
     * </ul>
     * </p>
     *
     * @param playerId   the UUID of the player
     * @param skillId    the ID of the skill to activate
     * @param playerData the player's RPG data
     * @param target     the target of the skill (can be null for SELF/NONE targets)
     * @return a SkillResult indicating the outcome
     */
    public SkillResult activateSkill(UUID playerId, String skillId, PlayerRPGData playerData, Object target) {
        // First check if we can activate
        SkillResult canActivate = canActivateSkill(playerId, skillId, playerData);
        if (!canActivate.isSuccess()) {
            return canActivate;
        }

        Skill skill = getSkill(skillId);

        // Validate target based on skill's target type
        SkillResult targetValidation = validateTarget(skill, target);
        if (!targetValidation.isSuccess()) {
            return targetValidation;
        }

        // Deduct mana cost
        if (skill.getManaCost() > 0) {
            playerData.setCurrentMana(playerData.getCurrentMana() - skill.getManaCost());
        }

        // Deduct stamina cost
        if (skill.getStaminaCost() > 0) {
            playerData.setCurrentStamina(playerData.getCurrentStamina() - skill.getStaminaCost());
        }

        // Start cooldown
        if (skill.getCooldownMillis() > 0) {
            getCooldowns(playerId).setCooldown(skillId, skill.getCooldownMillis());
        }

        // Execute skill effect
        executeSkillEffect(playerId, skill, playerData, target);

        LOGGER.fine("Player " + playerId + " activated skill: " + skillId);
        return SkillResult.success(skillId, "Skill activated: " + skill.getDisplayName());
    }

    /**
     * Validates the target for a skill.
     *
     * @param skill  the skill being used
     * @param target the target object
     * @return a SkillResult indicating if the target is valid
     */
    private SkillResult validateTarget(Skill skill, Object target) {
        TargetType targetType = skill.getTargetType();

        switch (targetType) {
            case SELF:
            case NONE:
                // No target validation needed
                return SkillResult.success();

            case ENEMY:
            case ALLY:
                if (target == null) {
                    return SkillResult.invalidTarget("This skill requires a target");
                }
                // Additional target type validation would go here
                // (checking if target is actually an enemy/ally)
                return SkillResult.success();

            case GROUND_POINT:
                if (target == null) {
                    return SkillResult.invalidTarget("This skill requires a target location");
                }
                // Validate target is a valid position
                return SkillResult.success();

            case DIRECTION:
                // Direction skills typically don't require a specific target
                return SkillResult.success();

            default:
                return SkillResult.invalidTarget("Unknown target type");
        }
    }

    /**
     * Executes the effect of a skill.
     * <p>
     * Processes the skill's effect_data and applies damage, healing, buffs, and debuffs.
     * </p>
     *
     * @param playerId   the UUID of the player
     * @param skill      the skill being executed
     * @param playerData the player's RPG data
     * @param target     the target of the skill
     */
    private void executeSkillEffect(UUID playerId, Skill skill, PlayerRPGData playerData, Object target) {
        LOGGER.fine("Executing skill effect for: " + skill.getId() +
                " (type: " + skill.getType() + ", target: " + skill.getTargetType() + ")");

        Map<String, Object> effectData = skill.getEffectData();
        if (effectData == null || effectData.isEmpty()) {
            LOGGER.fine("No effect data for skill: " + skill.getId());
            return;
        }

        // Get the combat manager for damage and effect handling
        CombatManager combatManager = RPGManager.getInstance().getCombatManager();
        
        // Determine target UUID
        UUID targetId = extractTargetId(target, playerId, skill.getTargetType());
        
        // Extract common effect parameters
        int duration = getEffectDataInt(effectData, "duration", 0);
        int stacks = getEffectDataInt(effectData, "stacks", 1);
        
        // Process damage effects
        Double damageAmount = getEffectDataDouble(effectData, "damage");
        if (damageAmount != null && damageAmount > 0 && targetId != null) {
            processDamageEffect(playerId, targetId, skill, effectData, damageAmount, combatManager);
        }
        
        // Process healing effects
        Double healAmount = getEffectDataDouble(effectData, "heal");
        if (healAmount != null && healAmount > 0) {
            processHealEffect(playerId, skill, playerData, target, healAmount);
        }
        
        // Process buff effects (applied to self or ally)
        String buffId = getEffectDataString(effectData, "buff_id");
        if (buffId != null && !buffId.isEmpty() && combatManager != null) {
            UUID buffTargetId = (skill.getTargetType() == TargetType.SELF || skill.getTargetType() == TargetType.NONE) 
                    ? playerId : (targetId != null ? targetId : playerId);
            applyEffectWithStacks(combatManager, buffTargetId, playerId, buffId, duration, stacks);
            LOGGER.fine("Applied buff " + buffId + " to " + buffTargetId);
        }
        
        // Process debuff effects (applied to enemy target)
        String debuffId = getEffectDataString(effectData, "debuff_id");
        if (debuffId != null && !debuffId.isEmpty() && targetId != null && combatManager != null) {
            applyEffectWithStacks(combatManager, targetId, playerId, debuffId, duration, stacks);
            LOGGER.fine("Applied debuff " + debuffId + " to " + targetId);
        }
        
        // Log additional parameters for future use
        Double aoeRadius = getEffectDataDouble(effectData, "aoe_radius");
        Double projectileSpeed = getEffectDataDouble(effectData, "projectile_speed");
        if (aoeRadius != null) {
            LOGGER.fine("AOE skill with radius: " + aoeRadius);
        }
        if (projectileSpeed != null) {
            LOGGER.fine("Projectile skill with speed: " + projectileSpeed);
        }
    }

    /**
     * Processes damage effect from skill effect_data.
     */
    private void processDamageEffect(UUID attackerId, UUID targetId, Skill skill, 
            Map<String, Object> effectData, double damageAmount, CombatManager combatManager) {
        
        // Parse damage type from effect_data
        DamageType damageType = DamageType.PHYSICAL;
        String damageTypeStr = getEffectDataString(effectData, "damage_type");
        if (damageTypeStr != null && !damageTypeStr.isEmpty()) {
            try {
                damageType = DamageType.valueOf(damageTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown damage type '" + damageTypeStr + "' for skill: " + skill.getId());
            }
        }

        // Build and deal the damage
        Damage damage = Damage.builder()
                .attackerId(attackerId)
                .targetId(targetId)
                .type(damageType)
                .rawAmount(damageAmount)
                .sourceSkill(skill.getId())
                .build();

        if (combatManager != null) {
            combatManager.dealDamage(damage);
            LOGGER.fine("Dealt " + damageAmount + " " + damageType + " damage from skill: " + skill.getId());
        } else {
            LOGGER.warning("CombatManager not available, cannot deal damage for skill: " + skill.getId());
        }
    }

    /**
     * Processes healing effect from skill effect_data.
     */
    private void processHealEffect(UUID playerId, Skill skill, PlayerRPGData playerData, 
            Object target, double healAmount) {
        
        // Determine who to heal based on target type
        PlayerRPGData healTarget = playerData; // Default to self
        
        if (skill.getTargetType() == TargetType.ALLY && target instanceof PlayerRPGData) {
            healTarget = (PlayerRPGData) target;
        }
        
        double actualHealed = healTarget.modifyCurrentHealth(healAmount);
        LOGGER.fine("Healed " + actualHealed + " HP (requested: " + healAmount + ") from skill: " + skill.getId());
    }

    /**
     * Applies an effect with the specified duration and stacks.
     */
    private void applyEffectWithStacks(CombatManager combatManager, UUID targetId, UUID sourceId, 
            String effectId, int duration, int stacks) {
        
        // Apply the effect (use -1 for default duration if not specified)
        int effectDuration = duration > 0 ? duration : -1;
        EffectInstance instance = combatManager.applyEffect(targetId, sourceId, effectId, effectDuration);
        
        if (instance != null && stacks > 1) {
            // Add additional stacks beyond the first
            instance.addStacks(stacks - 1);
        }
    }

    /**
     * Extracts a target UUID from the target object.
     */
    private UUID extractTargetId(Object target, UUID playerId, TargetType targetType) {
        if (target == null) {
            // For SELF/NONE skills, target might be null - use player's own ID
            if (targetType == TargetType.SELF || targetType == TargetType.NONE) {
                return playerId;
            }
            return null;
        }
        
        if (target instanceof UUID) {
            return (UUID) target;
        }
        
        if (target instanceof PlayerRPGData) {
            return ((PlayerRPGData) target).getPlayerId();
        }
        
        // For other object types, try to extract UUID via reflection or toString
        // This is a fallback for custom entity types
        try {
            // Try getUUID() method
            java.lang.reflect.Method getUUID = target.getClass().getMethod("getUUID");
            Object result = getUUID.invoke(target);
            if (result instanceof UUID) {
                return (UUID) result;
            }
        } catch (Exception ignored) {
            // Method not found or failed
        }
        
        try {
            // Try getPlayerId() method
            java.lang.reflect.Method getPlayerId = target.getClass().getMethod("getPlayerId");
            Object result = getPlayerId.invoke(target);
            if (result instanceof UUID) {
                return (UUID) result;
            }
        } catch (Exception ignored) {
            // Method not found or failed
        }
        
        try {
            // Try getId() method
            java.lang.reflect.Method getId = target.getClass().getMethod("getId");
            Object result = getId.invoke(target);
            if (result instanceof UUID) {
                return (UUID) result;
            }
        } catch (Exception ignored) {
            // Method not found or failed
        }
        
        LOGGER.warning("Could not extract UUID from target of type: " + target.getClass().getName());
        return null;
    }

    /**
     * Safely gets a double value from effect data.
     */
    private Double getEffectDataDouble(Map<String, Object> effectData, String key) {
        Object value = effectData.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely gets an integer value from effect data.
     */
    private int getEffectDataInt(Map<String, Object> effectData, String key, int defaultValue) {
        Object value = effectData.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Safely gets a string value from effect data.
     */
    private String getEffectDataString(Map<String, Object> effectData, String key) {
        Object value = effectData.get(key);
        return value != null ? value.toString() : null;
    }

    // ==================== Player Data Management ====================

    /**
     * Clears all data for a player.
     * <p>
     * This removes unlocked skills and cooldowns for the player.
     * </p>
     *
     * @param playerId the UUID of the player
     */
    public void clearPlayerData(UUID playerId) {
        if (playerId != null) {
            unlockedSkills.remove(playerId);
            cooldowns.remove(playerId);
        }
    }

    /**
     * Loads unlocked skills for a player from a set.
     *
     * @param playerId the UUID of the player
     * @param skills   the set of skill IDs to load
     */
    public void loadPlayerSkills(UUID playerId, Set<String> skills) {
        if (playerId != null && skills != null) {
            Set<String> playerSkills = unlockedSkills.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
            playerSkills.clear();
            for (String skillId : skills) {
                playerSkills.add(skillId.toLowerCase());
            }
        }
    }

    /**
     * Loads cooldowns for a player from a map.
     *
     * @param playerId      the UUID of the player
     * @param cooldownData  map of skill ID to expiration timestamp
     */
    public void loadPlayerCooldowns(UUID playerId, Map<String, Long> cooldownData) {
        if (playerId != null && cooldownData != null) {
            cooldowns.put(playerId, new SkillCooldown(cooldownData));
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Performs a tick operation for all cooldowns.
     * <p>
     * This should be called periodically to clean up expired cooldowns.
     * </p>
     */
    public void tick() {
        for (SkillCooldown cooldown : cooldowns.values()) {
            cooldown.tick();
        }
    }

    /**
     * Gets the total number of registered skills.
     *
     * @return the skill count
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Gets the total number of registered skill trees.
     *
     * @return the skill tree count
     */
    public int getSkillTreeCount() {
        return skillTrees.size();
    }

    /**
     * Clears all registered skills and skill trees.
     */
    public void clearAll() {
        skills.clear();
        skillTrees.clear();
        unlockedSkills.clear();
        cooldowns.clear();
    }

    /**
     * Resets the singleton instance (for testing purposes).
     */
    public static void resetInstance() {
        synchronized (LOCK) {
            if (instance != null) {
                instance.clearAll();
                instance = null;
            }
        }
    }
}
