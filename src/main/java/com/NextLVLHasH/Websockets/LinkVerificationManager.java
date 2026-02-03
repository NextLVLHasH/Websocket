package com.NextLVLHasH.Websockets;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages Discord <-> Hytale account linking verification
 * Uses auth codes for secure verification via game chat
 */
public class LinkVerificationManager {
    
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
    
    public LinkVerificationManager() {
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
}
