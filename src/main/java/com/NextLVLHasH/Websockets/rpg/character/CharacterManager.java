package com.NextLVLHasH.Websockets.rpg.character;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for all character-related definitions in the RPG system.
 * <p>
 * This class manages the loading, storage, and retrieval of {@link CharacterClass},
 * {@link Race}, and {@link Profession} definitions. It loads data from JSON files
 * in the configuration directories and provides methods for validating character
 * combinations and applying bonuses to player data.
 * </p>
 *
 * <p>Configuration directories:</p>
 * <ul>
 *   <li>{@code config/classes/} - Character class definitions</li>
 *   <li>{@code config/races/} - Race definitions</li>
 *   <li>{@code config/professions/} - Profession definitions</li>
 * </ul>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public final class CharacterManager {

    private static final Logger LOGGER = Logger.getLogger(CharacterManager.class.getName());

    // Singleton instance
    private static volatile CharacterManager instance;

    // Data storage
    private final Map<String, CharacterClass> classes;
    private final Map<String, Race> races;
    private final Map<String, Profession> professions;

    // Configuration paths
    private Path configBasePath;
    private static final String CLASSES_DIR = "classes";
    private static final String RACES_DIR = "races";
    private static final String PROFESSIONS_DIR = "professions";
    private static final String JSON_EXTENSION = ".json";

    // Loading state
    private volatile boolean loaded = false;

    // ==================== Singleton ====================

    /**
     * Private constructor for singleton pattern.
     */
    private CharacterManager() {
        this.classes = new ConcurrentHashMap<>();
        this.races = new ConcurrentHashMap<>();
        this.professions = new ConcurrentHashMap<>();
        this.configBasePath = Path.of("config");
    }

    /**
     * Gets the singleton instance of CharacterManager.
     *
     * @return the CharacterManager instance
     */
    public static CharacterManager getInstance() {
        if (instance == null) {
            synchronized (CharacterManager.class) {
                if (instance == null) {
                    instance = new CharacterManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (primarily for testing).
     */
    public static void resetInstance() {
        synchronized (CharacterManager.class) {
            instance = null;
        }
    }
    
    /**
     * Initializes the CharacterManager with the given data path.
     * Sets the config path and loads all character definitions.
     *
     * @param dataPath the base data directory
     */
    public void initialize(Path dataPath) {
        if (loaded) {
            LOGGER.warning("CharacterManager already initialized");
            return;
        }
        
        this.configBasePath = dataPath.resolve("config");
        
        try {
            loadAll();
        } catch (IOException e) {
            LOGGER.warning("Failed to load character definitions: " + e.getMessage());
            // Create default data if not found
            LOGGER.info("Creating default character definitions...");
        }
    }

    // ==================== Configuration ====================

    /**
     * Sets the base path for configuration files.
     *
     * @param configBasePath the base path (e.g., "config" or "plugins/rpg/config")
     */
    public void setConfigBasePath(Path configBasePath) {
        this.configBasePath = Objects.requireNonNull(configBasePath, "Config base path cannot be null");
    }

    /**
     * Gets the current configuration base path.
     *
     * @return the config base path
     */
    public Path getConfigBasePath() {
        return configBasePath;
    }

    // ==================== Loading ====================

    /**
     * Loads all character definitions from configuration files.
     * <p>
     * This method loads classes, races, and professions from their respective
     * directories under the configuration base path. Files must be JSON format
     * with the {@code .json} extension.
     * </p>
     *
     * @throws IOException if there is an error reading configuration files
     */
    public void loadAll() throws IOException {
        LOGGER.info("Loading all character definitions from: " + configBasePath);

        // Clear existing data
        classes.clear();
        races.clear();
        professions.clear();

        // Load each type
        int classCount = loadClasses();
        int raceCount = loadRaces();
        int professionCount = loadProfessions();

        loaded = true;

        LOGGER.info(String.format("Loaded %d classes, %d races, %d professions",
                classCount, raceCount, professionCount));
    }

    /**
     * Loads character class definitions from the classes directory.
     *
     * @return the number of classes loaded
     * @throws IOException if there is an error reading files
     */
    private int loadClasses() throws IOException {
        Path classesPath = configBasePath.resolve(CLASSES_DIR);
        if (!Files.exists(classesPath)) {
            LOGGER.warning("Classes directory not found: " + classesPath);
            Files.createDirectories(classesPath);
        }
        
        // Copy defaults if empty
        if (isDirectoryEmpty(classesPath)) {
            copyDefaultClasses(classesPath);
        }

        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(classesPath, "*" + JSON_EXTENSION)) {
            for (Path file : stream) {
                try {
                    CharacterClass characterClass = CharacterClass.fromFile(file);
                    classes.put(characterClass.getId().toLowerCase(), characterClass);
                    count++;
                    LOGGER.fine("Loaded class: " + characterClass.getId());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load class from: " + file, e);
                }
            }
        }
        return count;
    }

    /**
     * Loads race definitions from the races directory.
     *
     * @return the number of races loaded
     * @throws IOException if there is an error reading files
     */
    private int loadRaces() throws IOException {
        Path racesPath = configBasePath.resolve(RACES_DIR);
        if (!Files.exists(racesPath)) {
            LOGGER.warning("Races directory not found: " + racesPath);
            Files.createDirectories(racesPath);
        }
        
        // Copy defaults if empty
        if (isDirectoryEmpty(racesPath)) {
            copyDefaultRaces(racesPath);
        }

        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(racesPath, "*" + JSON_EXTENSION)) {
            for (Path file : stream) {
                try {
                    Race race = Race.fromFile(file);
                    races.put(race.getId().toLowerCase(), race);
                    count++;
                    LOGGER.fine("Loaded race: " + race.getId());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load race from: " + file, e);
                }
            }
        }
        return count;
    }

    /**
     * Loads profession definitions from the professions directory.
     *
     * @return the number of professions loaded
     * @throws IOException if there is an error reading files
     */
    private int loadProfessions() throws IOException {
        Path professionsPath = configBasePath.resolve(PROFESSIONS_DIR);
        if (!Files.exists(professionsPath)) {
            LOGGER.warning("Professions directory not found: " + professionsPath);
            Files.createDirectories(professionsPath);
        }
        
        // Copy defaults if empty
        if (isDirectoryEmpty(professionsPath)) {
            copyDefaultProfessions(professionsPath);
        }

        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(professionsPath, "*" + JSON_EXTENSION)) {
            for (Path file : stream) {
                try {
                    Profession profession = Profession.fromFile(file);
                    professions.put(profession.getId().toLowerCase(), profession);
                    count++;
                    LOGGER.fine("Loaded profession: " + profession.getId());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load profession from: " + file, e);
                }
            }
        }
        return count;
    }

    // ==================== Default Config Methods ====================
    
    /**
     * Checks if a directory is empty or doesn't contain any JSON files.
     */
    private boolean isDirectoryEmpty(Path dir) {
        if (!Files.exists(dir)) return true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + JSON_EXTENSION)) {
            return !stream.iterator().hasNext();
        } catch (IOException e) {
            return true;
        }
    }
    
    /**
     * Copy default class configs from resources to the target directory.
     */
    private void copyDefaultClasses(Path targetDir) {
        String[] defaultClasses = {
            "warrior.json",
            "mage.json",
            "rogue.json",
            "builder.json",
            "miner.json",
            "gatherer.json"
        };
        
        for (String classFile : defaultClasses) {
            copyResourceFile("config/classes/" + classFile, targetDir.resolve(classFile));
        }
        LOGGER.info("Copied " + defaultClasses.length + " default class configs");
    }
    
    /**
     * Copy default race configs from resources to the target directory.
     */
    private void copyDefaultRaces(Path targetDir) {
        String[] defaultRaces = {
            "human.json",
            "elf.json",
            "dwarf.json",
            "orc.json"
        };
        
        for (String raceFile : defaultRaces) {
            copyResourceFile("config/races/" + raceFile, targetDir.resolve(raceFile));
        }
        LOGGER.info("Copied " + defaultRaces.length + " default race configs");
    }
    
    /**
     * Copy default profession configs from resources to the target directory.
     */
    private void copyDefaultProfessions(Path targetDir) {
        String[] defaultProfessions = {
            "blacksmith.json",
            "herbalist.json",
            "alchemist.json",
            "enchanter.json"
        };
        
        for (String profFile : defaultProfessions) {
            copyResourceFile("config/professions/" + profFile, targetDir.resolve(profFile));
        }
        LOGGER.info("Copied " + defaultProfessions.length + " default profession configs");
    }
    
    /**
     * Copy a file from resources to the filesystem.
     */
    private void copyResourceFile(String resourcePath, Path targetPath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.fine("Copied default config: " + targetPath.getFileName());
            } else {
                LOGGER.warning("Resource not found: " + resourcePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to copy default config: " + resourcePath, e);
        }
    }

    /**
     * Reloads all character definitions.
     *
     * @throws IOException if there is an error reading files
     */
    public void reload() throws IOException {
        loadAll();
    }

    /**
     * Checks if definitions have been loaded.
     *
     * @return true if data has been loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    // ==================== Class Access ====================

    /**
     * Gets a character class by ID.
     *
     * @param id the class ID (case-insensitive)
     * @return the CharacterClass, or null if not found
     */
    public CharacterClass getClass(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return classes.get(id.toLowerCase());
    }

    /**
     * Gets a character class by ID, throwing an exception if not found.
     *
     * @param id the class ID (case-insensitive)
     * @return the CharacterClass
     * @throws NoSuchElementException if the class is not found
     */
    public CharacterClass getClassOrThrow(String id) {
        CharacterClass characterClass = getClass(id);
        if (characterClass == null) {
            throw new NoSuchElementException("Character class not found: " + id);
        }
        return characterClass;
    }

    /**
     * Gets an Optional containing the character class.
     *
     * @param id the class ID (case-insensitive)
     * @return Optional containing the class, or empty if not found
     */
    public Optional<CharacterClass> getClassOptional(String id) {
        return Optional.ofNullable(getClass(id));
    }

    /**
     * Gets all loaded character classes.
     *
     * @return unmodifiable list of all classes
     */
    public List<CharacterClass> getAllClasses() {
        return Collections.unmodifiableList(new ArrayList<>(classes.values()));
    }

    /**
     * Checks if a class with the given ID exists.
     *
     * @param id the class ID
     * @return true if the class exists
     */
    public boolean hasClass(String id) {
        return getClass(id) != null;
    }

    /**
     * Gets the number of loaded classes.
     *
     * @return the class count
     */
    public int getClassCount() {
        return classes.size();
    }

    // ==================== Race Access ====================

    /**
     * Gets a race by ID.
     *
     * @param id the race ID (case-insensitive)
     * @return the Race, or null if not found
     */
    public Race getRace(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return races.get(id.toLowerCase());
    }

    /**
     * Gets a race by ID, throwing an exception if not found.
     *
     * @param id the race ID (case-insensitive)
     * @return the Race
     * @throws NoSuchElementException if the race is not found
     */
    public Race getRaceOrThrow(String id) {
        Race race = getRace(id);
        if (race == null) {
            throw new NoSuchElementException("Race not found: " + id);
        }
        return race;
    }

    /**
     * Gets an Optional containing the race.
     *
     * @param id the race ID (case-insensitive)
     * @return Optional containing the race, or empty if not found
     */
    public Optional<Race> getRaceOptional(String id) {
        return Optional.ofNullable(getRace(id));
    }

    /**
     * Gets all loaded races.
     *
     * @return unmodifiable list of all races
     */
    public List<Race> getAllRaces() {
        return Collections.unmodifiableList(new ArrayList<>(races.values()));
    }

    /**
     * Checks if a race with the given ID exists.
     *
     * @param id the race ID
     * @return true if the race exists
     */
    public boolean hasRace(String id) {
        return getRace(id) != null;
    }

    /**
     * Gets the number of loaded races.
     *
     * @return the race count
     */
    public int getRaceCount() {
        return races.size();
    }

    // ==================== Profession Access ====================

    /**
     * Gets a profession by ID.
     *
     * @param id the profession ID (case-insensitive)
     * @return the Profession, or null if not found
     */
    public Profession getProfession(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return professions.get(id.toLowerCase());
    }

    /**
     * Gets a profession by ID, throwing an exception if not found.
     *
     * @param id the profession ID (case-insensitive)
     * @return the Profession
     * @throws NoSuchElementException if the profession is not found
     */
    public Profession getProfessionOrThrow(String id) {
        Profession profession = getProfession(id);
        if (profession == null) {
            throw new NoSuchElementException("Profession not found: " + id);
        }
        return profession;
    }

    /**
     * Gets an Optional containing the profession.
     *
     * @param id the profession ID (case-insensitive)
     * @return Optional containing the profession, or empty if not found
     */
    public Optional<Profession> getProfessionOptional(String id) {
        return Optional.ofNullable(getProfession(id));
    }

    /**
     * Gets all loaded professions.
     *
     * @return unmodifiable list of all professions
     */
    public List<Profession> getAllProfessions() {
        return Collections.unmodifiableList(new ArrayList<>(professions.values()));
    }

    /**
     * Gets professions available for a specific class.
     *
     * @param classId the class ID (case-insensitive)
     * @return list of professions available to the class
     */
    public List<Profession> getAvailableProfessions(String classId) {
        if (classId == null || classId.isBlank()) {
            // Return professions with no class restrictions
            return professions.values().stream()
                    .filter(p -> !p.hasClassRestrictions())
                    .toList();
        }

        return professions.values().stream()
                .filter(p -> p.isClassAllowed(classId))
                .toList();
    }

    /**
     * Checks if a profession with the given ID exists.
     *
     * @param id the profession ID
     * @return true if the profession exists
     */
    public boolean hasProfession(String id) {
        return getProfession(id) != null;
    }

    /**
     * Gets the number of loaded professions.
     *
     * @return the profession count
     */
    public int getProfessionCount() {
        return professions.size();
    }

    // ==================== Validation ====================

    /**
     * Validates if a combination of class, race, and profession is valid.
     *
     * @param classId      the class ID
     * @param raceId       the race ID
     * @param professionId the profession ID (can be null)
     * @return true if the combination is valid
     */
    public boolean isValidCombination(String classId, String raceId, String professionId) {
        // Validate class exists
        if (!hasClass(classId)) {
            return false;
        }

        // Validate race exists
        if (!hasRace(raceId)) {
            return false;
        }

        // Profession is optional, but if specified it must exist and be allowed
        if (professionId != null && !professionId.isBlank()) {
            Profession profession = getProfession(professionId);
            if (profession == null) {
                return false;
            }
            // Check if the profession allows this class
            if (!profession.isClassAllowed(classId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets a detailed validation result for a character combination.
     *
     * @param classId      the class ID
     * @param raceId       the race ID
     * @param professionId the profession ID (can be null)
     * @return validation result with details
     */
    public ValidationResult validateCombination(String classId, String raceId, String professionId) {
        List<String> errors = new ArrayList<>();

        if (!hasClass(classId)) {
            errors.add("Invalid class: " + classId);
        }

        if (!hasRace(raceId)) {
            errors.add("Invalid race: " + raceId);
        }

        if (professionId != null && !professionId.isBlank()) {
            Profession profession = getProfession(professionId);
            if (profession == null) {
                errors.add("Invalid profession: " + professionId);
            } else if (!profession.isClassAllowed(classId)) {
                errors.add("Profession '" + professionId + "' is not available for class '" + classId + "'");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Result of character combination validation.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        /**
         * Gets the first error message, or empty string if valid.
         *
         * @return the first error message
         */
        public String getFirstError() {
            return errors.isEmpty() ? "" : errors.get(0);
        }
    }

    // ==================== Character Bonus Application ====================

    /**
     * Applies class and race bonuses to player RPG data.
     * <p>
     * This method updates the player's base attributes with class base values
     * plus racial bonuses, sets resource pools (health, mana, stamina), and
     * configures regeneration rates based on the selected class and race.
     * </p>
     *
     * @param data the player RPG data to modify
     * @throws IllegalArgumentException if data is null or class/race are not set
     * @throws NoSuchElementException   if class or race is not found
     */
    public void applyCharacterBonuses(PlayerRPGData data) {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        String classId = data.getSelectedClass();
        String raceId = data.getSelectedRace();

        if (classId == null || classId.isBlank()) {
            throw new IllegalArgumentException("Player has no class selected");
        }
        if (raceId == null || raceId.isBlank()) {
            throw new IllegalArgumentException("Player has no race selected");
        }

        CharacterClass characterClass = getClassOrThrow(classId);
        Race race = getRaceOrThrow(raceId);

        int level = data.getLevel();

        // Apply attributes: class base + class growth + racial bonus
        for (AttributeType type : AttributeType.values()) {
            int classBase = characterClass.getBaseAttribute(type);
            int classGrowth = (int) Math.floor(characterClass.getAttributeGrowthRate(type) * (level - 1));
            int racialBonus = race.getRacialBonus(type);
            int totalAttribute = classBase + classGrowth + racialBonus;

            data.setBaseAttribute(type, totalAttribute);
            data.setCurrentAttribute(type, totalAttribute);
        }

        // Apply resource pools based on level
        double maxHealth = characterClass.calculateHealthAtLevel(level);
        double maxMana = characterClass.calculateManaAtLevel(level);
        double maxStamina = characterClass.calculateStaminaAtLevel(level);

        data.setMaxHealth(maxHealth);
        data.setCurrentHealth(maxHealth);
        data.setMaxMana(maxMana);
        data.setCurrentMana(maxMana);
        data.setMaxStamina(maxStamina);
        data.setCurrentStamina(maxStamina);

        // Apply regeneration rates
        data.setHealthRegen(characterClass.getHealthRegen());
        data.setManaRegen(characterClass.getManaRegen());
        data.setStaminaRegen(characterClass.getStaminaRegen());

        // Unlock starting skills from class
        for (String skillId : characterClass.getStartingSkills()) {
            data.unlockSkill(skillId);
        }

        // Unlock racial abilities
        for (String abilityId : race.getRacialAbilities()) {
            data.unlockSkill(abilityId);
        }

        LOGGER.fine(String.format("Applied bonuses for player %s: class=%s, race=%s, level=%d",
                data.getPlayerId(), classId, raceId, level));
    }

    /**
     * Recalculates and applies bonuses when a player levels up.
     *
     * @param data the player RPG data
     */
    public void applyLevelUpBonuses(PlayerRPGData data) {
        // Re-apply all bonuses with the new level
        applyCharacterBonuses(data);
    }

    /**
     * Gets the calculated stats for a hypothetical character at a given level.
     * Useful for UI preview during character creation.
     *
     * @param classId the class ID
     * @param raceId  the race ID
     * @param level   the level to calculate for
     * @return map of calculated stat values
     */
    public Map<String, Object> calculatePreviewStats(String classId, String raceId, int level) {
        CharacterClass characterClass = getClassOrThrow(classId);
        Race race = getRaceOrThrow(raceId);

        Map<String, Object> stats = new LinkedHashMap<>();

        // Calculate attributes
        Map<String, Integer> attributes = new LinkedHashMap<>();
        for (AttributeType type : AttributeType.values()) {
            int classBase = characterClass.getBaseAttribute(type);
            int classGrowth = (int) Math.floor(characterClass.getAttributeGrowthRate(type) * (level - 1));
            int racialBonus = race.getRacialBonus(type);
            attributes.put(type.getDisplayName(), classBase + classGrowth + racialBonus);
        }
        stats.put("attributes", attributes);

        // Calculate resources
        stats.put("maxHealth", characterClass.calculateHealthAtLevel(level));
        stats.put("maxMana", characterClass.calculateManaAtLevel(level));
        stats.put("maxStamina", characterClass.calculateStaminaAtLevel(level));

        // Add regen rates
        stats.put("healthRegen", characterClass.getHealthRegen());
        stats.put("manaRegen", characterClass.getManaRegen());
        stats.put("staminaRegen", characterClass.getStaminaRegen());

        // Add racial modifiers
        stats.put("heightScale", race.getHeightScale());
        stats.put("speedModifier", race.getSpeedModifier());
        stats.put("resistances", race.getResistances());

        // Add class modifiers
        stats.put("modifiers", characterClass.getModifiers());

        return stats;
    }

    // ==================== Registration (for programmatic addition) ====================

    /**
     * Registers a character class programmatically.
     *
     * @param characterClass the class to register
     */
    public void registerClass(CharacterClass characterClass) {
        Objects.requireNonNull(characterClass, "CharacterClass cannot be null");
        classes.put(characterClass.getId().toLowerCase(), characterClass);
        LOGGER.fine("Registered class: " + characterClass.getId());
    }

    /**
     * Registers a race programmatically.
     *
     * @param race the race to register
     */
    public void registerRace(Race race) {
        Objects.requireNonNull(race, "Race cannot be null");
        races.put(race.getId().toLowerCase(), race);
        LOGGER.fine("Registered race: " + race.getId());
    }

    /**
     * Registers a profession programmatically.
     *
     * @param profession the profession to register
     */
    public void registerProfession(Profession profession) {
        Objects.requireNonNull(profession, "Profession cannot be null");
        professions.put(profession.getId().toLowerCase(), profession);
        LOGGER.fine("Registered profession: " + profession.getId());
    }

    // ==================== Unregistration ====================

    /**
     * Unregisters a character class.
     *
     * @param id the class ID
     * @return the removed class, or null if not found
     */
    public CharacterClass unregisterClass(String id) {
        if (id == null) return null;
        return classes.remove(id.toLowerCase());
    }

    /**
     * Unregisters a race.
     *
     * @param id the race ID
     * @return the removed race, or null if not found
     */
    public Race unregisterRace(String id) {
        if (id == null) return null;
        return races.remove(id.toLowerCase());
    }

    /**
     * Unregisters a profession.
     *
     * @param id the profession ID
     * @return the removed profession, or null if not found
     */
    public Profession unregisterProfession(String id) {
        if (id == null) return null;
        return professions.remove(id.toLowerCase());
    }

    // ==================== Save Methods ====================
    
    /**
     * Saves all classes to their JSON files.
     */
    public void saveClasses() {
        Path classesPath = configBasePath.resolve(CLASSES_DIR);
        try {
            Files.createDirectories(classesPath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create classes directory", e);
            return;
        }
        
        for (CharacterClass charClass : classes.values()) {
            Path filePath = classesPath.resolve(charClass.getId() + JSON_EXTENSION);
            try (java.io.Writer writer = Files.newBufferedWriter(filePath)) {
                writer.write(charClass.toJson());
                LOGGER.fine("Saved class: " + charClass.getId());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save class: " + charClass.getId(), e);
            }
        }
    }
    
    /**
     * Saves all races to their JSON files.
     */
    public void saveRaces() {
        Path racesPath = configBasePath.resolve(RACES_DIR);
        try {
            Files.createDirectories(racesPath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create races directory", e);
            return;
        }
        
        for (Race race : races.values()) {
            Path filePath = racesPath.resolve(race.getId() + JSON_EXTENSION);
            try (java.io.Writer writer = Files.newBufferedWriter(filePath)) {
                writer.write(race.toJson());
                LOGGER.fine("Saved race: " + race.getId());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save race: " + race.getId(), e);
            }
        }
    }
    
    /**
     * Removes and deletes a class file.
     */
    public void removeClass(String id) {
        if (id == null) return;
        String lowerId = id.toLowerCase();
        CharacterClass removed = classes.remove(lowerId);
        if (removed != null) {
            Path filePath = configBasePath.resolve(CLASSES_DIR).resolve(lowerId + JSON_EXTENSION);
            try {
                Files.deleteIfExists(filePath);
                LOGGER.info("Deleted class file: " + filePath);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete class file: " + filePath, e);
            }
        }
    }
    
    /**
     * Removes and deletes a race file.
     */
    public void removeRace(String id) {
        if (id == null) return;
        String lowerId = id.toLowerCase();
        Race removed = races.remove(lowerId);
        if (removed != null) {
            Path filePath = configBasePath.resolve(RACES_DIR).resolve(lowerId + JSON_EXTENSION);
            try {
                Files.deleteIfExists(filePath);
                LOGGER.info("Deleted race file: " + filePath);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete race file: " + filePath, e);
            }
        }
    }

    // ==================== Utility ====================

    /**
     * Clears all loaded data.
     */
    public void clear() {
        classes.clear();
        races.clear();
        professions.clear();
        loaded = false;
        LOGGER.info("Cleared all character definitions");
    }

    /**
     * Gets a summary of loaded definitions.
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format("CharacterManager: %d classes, %d races, %d professions (loaded=%b)",
                classes.size(), races.size(), professions.size(), loaded);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
