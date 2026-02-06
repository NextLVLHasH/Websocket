package com.NextLVLHasH.Websockets.rpg.persistence;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository for persisting and loading PlayerRPGData to/from the file system.
 * Data is stored in the format: player_data/&lt;uuid&gt;/rpg_data.json
 */
public class RPGDataRepository {

    private static final Logger LOGGER = Logger.getLogger(RPGDataRepository.class.getName());
    private static final String DATA_FOLDER = "player_data";
    private static final String DATA_FILE = "rpg_data.json";

    private final Path basePath;
    private final RPGDataSerializer serializer;

    /**
     * Creates a new RPGDataRepository with the default base path (current working directory).
     */
    public RPGDataRepository() {
        this(Paths.get("."));
    }

    /**
     * Creates a new RPGDataRepository with a custom base path.
     *
     * @param basePath the base directory for storing player data
     */
    public RPGDataRepository(Path basePath) {
        this.basePath = Objects.requireNonNull(basePath, "Base path cannot be null");
        this.serializer = new RPGDataSerializer();
    }

    /**
     * Gets the path to the data file for a specific player.
     *
     * @param playerId the player's UUID
     * @return the path to the player's data file
     */
    public Path getPlayerDataPath(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return basePath.resolve(DATA_FOLDER).resolve(playerId.toString()).resolve(DATA_FILE);
    }

    /**
     * Gets the path to the player's data directory.
     *
     * @param playerId the player's UUID
     * @return the path to the player's data directory
     */
    public Path getPlayerDataDirectory(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return basePath.resolve(DATA_FOLDER).resolve(playerId.toString());
    }

    /**
     * Saves player RPG data to the file system.
     *
     * @param data the player data to save
     * @throws IOException              if there's an error writing the file
     * @throws IllegalArgumentException if data is null
     */
    public void save(PlayerRPGData data) throws IOException {
        Objects.requireNonNull(data, "PlayerRPGData cannot be null");

        Path filePath = getPlayerDataPath(data.getPlayerId());
        Path directory = filePath.getParent();

        // Ensure directory exists
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
            LOGGER.fine("Created player data directory: " + directory);
        }

        // Serialize and write
        String json = serializer.serialize(data);
        
        // Write to temp file first, then move atomically for data safety
        Path tempFile = filePath.resolveSibling(DATA_FILE + ".tmp");
        try {
            Files.writeString(tempFile, json, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
            
            // Atomic move (replace if exists)
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            LOGGER.fine("Saved RPG data for player: " + data.getPlayerId());
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback to non-atomic move if atomic not supported
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.fine("Saved RPG data for player (non-atomic): " + data.getPlayerId());
        } finally {
            // Clean up temp file if it still exists
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Loads player RPG data from the file system.
     *
     * @param playerId the UUID of the player to load
     * @return the loaded PlayerRPGData, or null if not found or error occurs
     */
    public PlayerRPGData load(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Path filePath = getPlayerDataPath(playerId);

        if (!Files.exists(filePath)) {
            LOGGER.fine("No data file found for player: " + playerId);
            return null;
        }

        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            PlayerRPGData data = serializer.deserialize(json);
            LOGGER.fine("Loaded RPG data for player: " + playerId);
            return data;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read RPG data for player: " + playerId, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse RPG data for player: " + playerId, e);
            return null;
        }
    }

    /**
     * Loads player RPG data, returning an Optional.
     *
     * @param playerId the UUID of the player to load
     * @return Optional containing the data if found and loaded successfully
     */
    public Optional<PlayerRPGData> loadOptional(UUID playerId) {
        return Optional.ofNullable(load(playerId));
    }

    /**
     * Checks if player data exists in the file system.
     *
     * @param playerId the UUID of the player to check
     * @return true if the player's data file exists
     */
    public boolean exists(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return Files.exists(getPlayerDataPath(playerId));
    }

    /**
     * Deletes player RPG data from the file system.
     *
     * @param playerId the UUID of the player to delete
     * @throws IOException if there's an error deleting the file
     */
    public void delete(UUID playerId) throws IOException {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Path filePath = getPlayerDataPath(playerId);
        Path directory = getPlayerDataDirectory(playerId);

        // Delete the data file
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            LOGGER.fine("Deleted RPG data file for player: " + playerId);
        }

        // Delete the directory if empty
        if (Files.exists(directory)) {
            try (var stream = Files.list(directory)) {
                if (stream.findAny().isEmpty()) {
                    Files.delete(directory);
                    LOGGER.fine("Deleted empty player directory for player: " + playerId);
                }
            }
        }
    }

    /**
     * Attempts to delete player data, returning success status instead of throwing.
     *
     * @param playerId the UUID of the player to delete
     * @return true if deletion was successful or file didn't exist, false on error
     */
    public boolean tryDelete(UUID playerId) {
        try {
            delete(playerId);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete RPG data for player: " + playerId, e);
            return false;
        }
    }

    /**
     * Creates a backup of player data.
     *
     * @param playerId the UUID of the player
     * @return the path to the backup file, or null if backup failed
     */
    public Path backup(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Path source = getPlayerDataPath(playerId);
        if (!Files.exists(source)) {
            LOGGER.fine("No data to backup for player: " + playerId);
            return null;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        Path backupPath = source.resolveSibling("rpg_data_backup_" + timestamp + ".json");

        try {
            Files.copy(source, backupPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.fine("Created backup for player " + playerId + " at: " + backupPath);
            return backupPath;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create backup for player: " + playerId, e);
            return null;
        }
    }

    /**
     * Restores player data from a backup file.
     *
     * @param playerId   the UUID of the player
     * @param backupPath the path to the backup file
     * @return true if restore was successful
     */
    public boolean restore(UUID playerId, Path backupPath) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(backupPath, "Backup path cannot be null");

        if (!Files.exists(backupPath)) {
            LOGGER.warning("Backup file does not exist: " + backupPath);
            return false;
        }

        Path targetPath = getPlayerDataPath(playerId);

        try {
            // Ensure directory exists
            Path directory = targetPath.getParent();
            if (directory != null && !Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.fine("Restored backup for player " + playerId + " from: " + backupPath);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to restore backup for player: " + playerId, e);
            return false;
        }
    }

    /**
     * Gets the total size of the player data directory.
     *
     * @return the total size in bytes, or -1 on error
     */
    public long getTotalDataSize() {
        Path dataDir = basePath.resolve(DATA_FOLDER);
        if (!Files.exists(dataDir)) {
            return 0;
        }

        try (var walk = Files.walk(dataDir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to calculate total data size", e);
            return -1;
        }
    }

    /**
     * Counts the number of player data files.
     *
     * @return the number of player data files
     */
    public long getPlayerDataCount() {
        Path dataDir = basePath.resolve(DATA_FOLDER);
        if (!Files.exists(dataDir)) {
            return 0;
        }

        try (var stream = Files.list(dataDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve(DATA_FILE)))
                    .count();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to count player data files", e);
            return 0;
        }
    }

    /**
     * Gets the base path for this repository.
     *
     * @return the base path
     */
    public Path getBasePath() {
        return basePath;
    }
}
