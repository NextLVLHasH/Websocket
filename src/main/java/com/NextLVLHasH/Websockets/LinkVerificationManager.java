package com.NextLVLHasH.Websockets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages Discord <-> Hytale account linking verification
 * Uses auth codes for secure verification via game chat
 * Persists verified links to JSON file
 */
public class LinkVerificationManager {
    private static final Logger LOGGER = Logger.getLogger(LinkVerificationManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Pending verification: authCode -> PendingLink
    private final ConcurrentHashMap<String, PendingLink> pendingVerifications = new ConcurrentHashMap<>();
    
    // HytaleUuid -> authCode mapping (for lookup when code typed in game)
    private final ConcurrentHashMap<String, String> hytaleUuidToPending = new ConcurrentHashMap<>();
    
    // Discord username -> authCode mapping (for lookup when DM received)
    private final ConcurrentHashMap<String, String> discordUserToPending = new ConcurrentHashMap<>();
    
    // Verified links: hytaleUuid -> VerifiedLink
    private final ConcurrentHashMap<String, VerifiedLink> verifiedLinks = new ConcurrentHashMap<>();
    
    // Discord ID -> hytaleUuid reverse mapping
    private final ConcurrentHashMap<String, String> discordIdToUuid = new ConcurrentHashMap<>();
    
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // Auth code expiry in minutes
    private static final int CODE_EXPIRY_MINUTES = 10;
    
    // Persistence
    private final Path linksFilePath;
    private final Path playerDataDirectory;
    
    public LinkVerificationManager() {
        this(null);
    }
    
    public LinkVerificationManager(Path dataDirectory) {
        // Setup persistence path
        if (dataDirectory != null) {
            this.linksFilePath = dataDirectory.resolve("discord_links.json");
            this.playerDataDirectory = dataDirectory.resolve("player_data");
            try {
                Files.createDirectories(playerDataDirectory);
            } catch (IOException e) {
                LOGGER.warning("Failed to create player_data directory: " + e.getMessage());
            }
            loadLinks();
        } else {
            this.linksFilePath = null;
            this.playerDataDirectory = null;
        }
        
        // Schedule cleanup of expired codes every minute
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredCodes, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Generate a new auth code for linking
     * @param hytaleUuid Player's Hytale UUID
     * @param hytaleName Player's Hytale username
     * @param discordUsername Discord username to link to
     * @return The generated auth code
     */
    public String generateAuthCode(String hytaleUuid, String hytaleName, String discordUsername) {
        // Remove any existing pending verification for this player
        String oldCode = hytaleUuidToPending.remove(hytaleUuid);
        if (oldCode != null) {
            PendingLink oldPending = pendingVerifications.remove(oldCode);
            if (oldPending != null) {
                discordUserToPending.remove(oldPending.discordUsername.toLowerCase());
            }
        }
        
        // Generate 6-digit auth code
        String authCode = String.format("%06d", random.nextInt(1000000));
        
        // Store pending verification
        PendingLink pending = new PendingLink(hytaleUuid, hytaleName, discordUsername, System.currentTimeMillis());
        pendingVerifications.put(authCode, pending);
        hytaleUuidToPending.put(hytaleUuid, authCode);
        discordUserToPending.put(discordUsername.toLowerCase(), authCode);
        
        return authCode;
    }
    
    /**
     * Verify an auth code from Discord DM
     * @param discordUserId Discord user ID
     * @param discordUsername Discord username
     * @param authCode The auth code provided
     * @return VerificationResult with status and player info
     */
    public VerificationResult verifyCode(String discordUserId, String discordUsername, String authCode) {
        PendingLink pending = pendingVerifications.get(authCode);
        
        if (pending == null) {
            return new VerificationResult(false, null, null, "Invalid auth code. Please use /link in-game to get a new code.");
        }
        
        // Check if code is expired
        long ageMinutes = (System.currentTimeMillis() - pending.timestamp) / (1000 * 60);
        if (ageMinutes > CODE_EXPIRY_MINUTES) {
            pendingVerifications.remove(authCode);
            discordUserToPending.remove(pending.discordUsername.toLowerCase());
            return new VerificationResult(false, null, null, "Auth code has expired. Please use /link in-game to get a new code.");
        }
        
        // Verify the Discord username matches (case-insensitive)
        if (!pending.discordUsername.equalsIgnoreCase(discordUsername)) {
            return new VerificationResult(false, null, null, 
                "This code was generated for Discord user '" + pending.discordUsername + "'. " +
                "Please make sure you're using the correct Discord account.");
        }
        
        // Success! Create verified link
        VerifiedLink verified = new VerifiedLink(
            pending.hytaleUuid,
            pending.hytaleName,
            discordUserId,
            discordUsername,
            System.currentTimeMillis()
        );
        
        // Remove old link for this UUID if exists
        VerifiedLink oldLink = verifiedLinks.get(pending.hytaleUuid);
        if (oldLink != null) {
            discordIdToUuid.remove(oldLink.discordUserId);
        }
        
        // Store verified link
        verifiedLinks.put(pending.hytaleUuid, verified);
        discordIdToUuid.put(discordUserId, pending.hytaleUuid);
        
        // Persist to disk immediately
        LOGGER.info("Link verified via Discord DM - saving immediately for: " + pending.hytaleName);
        saveLinks();
        saveToPlayerJson(verified);
        
        // Clean up pending
        pendingVerifications.remove(authCode);
        discordUserToPending.remove(pending.discordUsername.toLowerCase());
        
        return new VerificationResult(true, pending.hytaleUuid, pending.hytaleName, 
            "Successfully linked! You are now linked to Hytale player: " + pending.hytaleName);
    }
    
    /**
     * Check if a Discord user has a pending verification
     */
    public boolean hasPendingVerification(String discordUsername) {
        return discordUserToPending.containsKey(discordUsername.toLowerCase());
    }
    
    /**
     * Get pending auth code for Discord user (for reminder messages)
     */
    public String getPendingAuthCode(String discordUsername) {
        return discordUserToPending.get(discordUsername.toLowerCase());
    }
    
    /**
     * Update the Discord user ID for a pending verification (called when DM is sent)
     * @param authCode The auth code
     * @param discordUserId The Discord user ID
     */
    public void updatePendingDiscordUserId(String authCode, String discordUserId) {
        PendingLink pending = pendingVerifications.get(authCode);
        if (pending != null) {
            pending.discordUserId = discordUserId;
        }
    }
    
    /**
     * Check if Hytale player is verified
     */
    public boolean isVerified(String hytaleUuid) {
        return verifiedLinks.containsKey(hytaleUuid);
    }
    
    /**
     * Get verified link for Hytale player
     */
    public VerifiedLink getVerifiedLink(String hytaleUuid) {
        return verifiedLinks.get(hytaleUuid);
    }
    
    /**
     * Get Hytale UUID for Discord user
     */
    public String getHytaleUuidByDiscordId(String discordUserId) {
        return discordIdToUuid.get(discordUserId);
    }
    
    /**
     * Get Discord ID for a verified Hytale player
     */
    public String getDiscordIdByHytaleUuid(String hytaleUuid) {
        VerifiedLink link = verifiedLinks.get(hytaleUuid);
        return link != null ? link.discordUserId : null;
    }
    
    /**
     * Verify a code by the Hytale player (typed in game chat)
     * This verifies that the player typing the code is the one who requested the link
     * @param hytaleUuid The player's UUID who typed the code
     * @param authCode The auth code typed
     * @return VerificationResult with status
     */
    public VerificationResult verifyCodeByPlayer(String hytaleUuid, String authCode) {
        PendingLink pending = pendingVerifications.get(authCode);
        
        if (pending == null) {
            return new VerificationResult(false, null, null, "Invalid code. Use /link <discord_username> first.");
        }
        
        // Check if code is expired
        long ageMinutes = (System.currentTimeMillis() - pending.timestamp) / (1000 * 60);
        if (ageMinutes > CODE_EXPIRY_MINUTES) {
            pendingVerifications.remove(authCode);
            hytaleUuidToPending.remove(pending.hytaleUuid);
            discordUserToPending.remove(pending.discordUsername.toLowerCase());
            return new VerificationResult(false, null, null, "Code expired. Use /link again.");
        }
        
        // Verify the Hytale UUID matches
        if (!pending.hytaleUuid.equals(hytaleUuid)) {
            return new VerificationResult(false, null, null, 
                "This code belongs to a different player.");
        }
        
        // Check if we have the Discord user ID (set when DM was sent)
        if (pending.discordUserId == null) {
            return new VerificationResult(false, null, null, 
                "Could not find Discord user. Make sure you entered the correct username.");
        }
        
        // Success! Create verified link
        VerifiedLink verified = new VerifiedLink(
            pending.hytaleUuid,
            pending.hytaleName,
            pending.discordUserId,
            pending.discordUsername,
            System.currentTimeMillis()
        );
        
        // Remove old link for this UUID if exists
        VerifiedLink oldLink = verifiedLinks.get(pending.hytaleUuid);
        if (oldLink != null) {
            discordIdToUuid.remove(oldLink.discordUserId);
        }
        
        // Store verified link
        verifiedLinks.put(pending.hytaleUuid, verified);
        discordIdToUuid.put(pending.discordUserId, pending.hytaleUuid);
        
        // Persist to disk immediately
        LOGGER.info("Link verified via game chat - saving immediately for: " + pending.hytaleName);
        saveLinks();
        
        // Clean up pending
        pendingVerifications.remove(authCode);
        hytaleUuidToPending.remove(pending.hytaleUuid);
        discordUserToPending.remove(pending.discordUsername.toLowerCase());
        
        return new VerificationResult(true, pending.hytaleUuid, pending.hytaleName, 
            "Successfully linked to Discord: " + pending.discordUsername);
    }
    
    /**
     * Unlink a player
     */
    public void unlink(String hytaleUuid) {
        VerifiedLink link = verifiedLinks.remove(hytaleUuid);
        if (link != null) {
            discordIdToUuid.remove(link.discordUserId);
            // Persist change to disk immediately
            LOGGER.info("Link removed - saving immediately");
            saveLinks();
        }
    }
    
    /**
     * Cleanup expired verification codes
     */
    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        long expiryMs = CODE_EXPIRY_MINUTES * 60 * 1000;
        
        pendingVerifications.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp > expiryMs) {
                hytaleUuidToPending.remove(entry.getValue().hytaleUuid);
                discordUserToPending.remove(entry.getValue().discordUsername.toLowerCase());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Load verified links from JSON file
     */
    private void loadLinks() {
        if (linksFilePath == null) {
            LOGGER.warning("No data directory configured, links will not persist");
            return;
        }
        
        LOGGER.info("Looking for discord links file at: " + linksFilePath.toAbsolutePath());
        
        if (!Files.exists(linksFilePath)) {
            LOGGER.info("No existing discord links file found, starting fresh");
            return;
        }
        
        try {
            String json = Files.readString(linksFilePath);
            LOGGER.info("Read discord links file: " + json.length() + " chars");
            
            Type type = new TypeToken<Map<String, LinkData>>(){}.getType();
            Map<String, LinkData> loadedLinks = GSON.fromJson(json, type);
            
            if (loadedLinks != null && !loadedLinks.isEmpty()) {
                for (Map.Entry<String, LinkData> entry : loadedLinks.entrySet()) {
                    String hytaleUuid = entry.getKey();
                    LinkData data = entry.getValue();
                    
                    VerifiedLink link = new VerifiedLink(
                        hytaleUuid,
                        data.hytaleName,
                        data.discordUserId,
                        data.discordUsername,
                        data.verifiedAt
                    );
                    
                    verifiedLinks.put(hytaleUuid, link);
                    discordIdToUuid.put(data.discordUserId, hytaleUuid);
                    LOGGER.info("Loaded link: " + data.hytaleName + " <-> " + data.discordUsername);
                }
                
                LOGGER.info("Successfully loaded " + verifiedLinks.size() + " Discord links from file");
            } else {
                LOGGER.info("Discord links file was empty or null");
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to load Discord links: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.warning("Error parsing Discord links file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save verified links to JSON file
     */
    private void saveLinks() {
        if (linksFilePath == null) {
            LOGGER.warning("Cannot save links - no data directory configured");
            return;
        }
        
        try {
            // Ensure parent directory exists
            Path parent = linksFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                LOGGER.info("Created data directory: " + parent);
            }
            
            // Convert to serializable format
            Map<String, LinkData> dataMap = new HashMap<>();
            for (Map.Entry<String, VerifiedLink> entry : verifiedLinks.entrySet()) {
                VerifiedLink link = entry.getValue();
                LinkData data = new LinkData();
                data.hytaleName = link.hytaleName;
                data.discordUserId = link.discordUserId;
                data.discordUsername = link.discordUsername;
                data.verifiedAt = link.verifiedAt;
                dataMap.put(entry.getKey(), data);
            }
            
            String json = GSON.toJson(dataMap);
            Files.writeString(linksFilePath, json);
            LOGGER.info("Saved " + verifiedLinks.size() + " Discord links to: " + linksFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.warning("Failed to save Discord links to " + linksFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save Discord link info to player's individual JSON file
     */
    private void saveToPlayerJson(VerifiedLink link) {
        if (playerDataDirectory == null) {
            return;
        }
        
        Path playerFile = playerDataDirectory.resolve(link.hytaleUuid + ".json");
        
        try {
            // Load existing data
            PlayerData data;
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                data = GSON.fromJson(json, PlayerData.class);
                if (data == null) {
                    data = new PlayerData();
                }
            } else {
                data = new PlayerData();
            }
            
            // Update Discord link info
            data.discordUsername = link.discordUsername;
            data.discordUserId = link.discordUserId;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            data.linkedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(link.verifiedAt), 
                                                    java.time.ZoneId.systemDefault())
                                        .format(formatter);
            
            // Save back to file
            String json = GSON.toJson(data);
            Files.writeString(playerFile, json);
            LOGGER.info("Saved Discord link to player file: " + playerFile.toAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.warning("Failed to save Discord link to player JSON " + link.hytaleUuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Shutdown cleanup executor
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a pending link verification
     */
    public static class PendingLink {
        public final String hytaleUuid;
        public final String hytaleName;
        public final String discordUsername;
        public volatile String discordUserId; // Set when DM is sent
        public final long timestamp;
        
        public PendingLink(String hytaleUuid, String hytaleName, String discordUsername, long timestamp) {
            this.hytaleUuid = hytaleUuid;
            this.hytaleName = hytaleName;
            this.discordUsername = discordUsername;
            this.discordUserId = null;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Represents a verified link
     */
    public static class VerifiedLink {
        public final String hytaleUuid;
        public final String hytaleName;
        public final String discordUserId;
        public final String discordUsername;
        public final long verifiedAt;
        
        public VerifiedLink(String hytaleUuid, String hytaleName, String discordUserId, 
                           String discordUsername, long verifiedAt) {
            this.hytaleUuid = hytaleUuid;
            this.hytaleName = hytaleName;
            this.discordUserId = discordUserId;
            this.discordUsername = discordUsername;
            this.verifiedAt = verifiedAt;
        }
    }
    
    /**
     * Result of verification attempt
     */
    public static class VerificationResult {
        public final boolean success;
        public final String hytaleUuid;
        public final String hytaleName;
        public final String message;
        
        public VerificationResult(boolean success, String hytaleUuid, String hytaleName, String message) {
            this.success = success;
            this.hytaleUuid = hytaleUuid;
            this.hytaleName = hytaleName;
            this.message = message;
        }
    }
    
    /**
     * JSON-serializable link data for persistence
     */
    private static class LinkData {
        public String hytaleName;
        public String discordUserId;
        public String discordUsername;
        public long verifiedAt;
    }
    
    /**
     * Player data structure for JSON storage (shared with PrivateMessageLogger)
     */
    private static class PlayerData {
        @SuppressWarnings("unused")
        public String discordUsername;
        @SuppressWarnings("unused")
        public String discordUserId;
        @SuppressWarnings("unused")
        public String linkedAt;
        @SuppressWarnings("unused")
        public List<Object> privateMessages;  // List of message records
    }
}
