package com.NextLVLHasH.Websockets.rpg.skills;

import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility class for managing skill-related permissions.
 * <p>
 * This class provides methods to check, grant, and revoke permissions
 * related to skill usage. Uses in-memory permission storage with support
 * for checking permissions directly on Player objects when available.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * UUID playerId = ...;
 * String skillId = "fireball";
 * 
 * // Get the required permission string
 * String perm = SkillPermission.getSkillPermission(skillId); // "rpg.skill.fireball"
 * 
 * // Check if player has permission (via cache)
 * if (SkillPermission.hasPermission(playerId, perm)) {
 *     // Player can use the skill
 * }
 * 
 * // Or check directly with Player object
 * if (SkillPermission.hasPermission(player, perm)) {
 *     // Player can use the skill
 * }
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public final class SkillPermission {

    /**
     * The prefix used for all skill-related permissions.
     */
    public static final String SKILL_PERMISSION_PREFIX = "rpg.skill.";

    /**
     * The wildcard permission that grants access to all skills.
     */
    public static final String SKILL_WILDCARD_PERMISSION = "rpg.skill.*";

    /**
     * In-memory permission storage (fallback/cache for permission system).
     * Maps player UUID to their set of granted permissions.
     */
    private static final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private SkillPermission() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Permission Checks ====================

    /**
     * Checks if a player has a specific permission using their Player object.
     * <p>
     * This method uses Hytale's native permission system when you have
     * direct access to the Player object.
     * </p>
     *
     * @param player     the Player object to check
     * @param permission the permission string to check
     * @return true if the player has the permission, false otherwise
     */
    public static boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null || permission.isEmpty()) {
            return false;
        }

        try {
            // Check via Hytale's permission system
            if (player.hasPermission(SKILL_WILDCARD_PERMISSION)) {
                return true;
            }
            return player.hasPermission(permission);
        } catch (Exception e) {
            // Fall through to return false
        }

        return false;
    }

    /**
     * Checks if a player has a specific permission using their UUID.
     * <p>
     * This method checks the in-memory permission cache. Use this when
     * you don't have direct access to the Player object.
     * </p>
     *
     * @param playerId   the UUID of the player to check
     * @param permission the permission string to check
     * @return true if the player has the permission in the cache, false otherwise
     */
    public static boolean hasPermission(UUID playerId, String permission) {
        if (playerId == null || permission == null || permission.isEmpty()) {
            return false;
        }

        // Check in-memory cache
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            if (perms.contains(SKILL_WILDCARD_PERMISSION)) {
                return true;
            }
            if (perms.contains(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a player has all of the specified permissions.
     *
     * @param playerId    the UUID of the player to check
     * @param permissions the list of permission strings to check
     * @return true if the player has all permissions, false if any are missing
     */
    public static boolean hasAllPermissions(UUID playerId, List<String> permissions) {
        if (playerId == null) {
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        for (String permission : permissions) {
            if (!hasPermission(playerId, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a player has any of the specified permissions.
     *
     * @param playerId    the UUID of the player to check
     * @param permissions the list of permission strings to check
     * @return true if the player has at least one of the permissions
     */
    public static boolean hasAnyPermission(UUID playerId, List<String> permissions) {
        if (playerId == null) {
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }

        for (String permission : permissions) {
            if (hasPermission(playerId, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a player has permission to use a specific skill.
     *
     * @param playerId the UUID of the player to check
     * @param skillId  the ID of the skill to check
     * @return true if the player has permission to use the skill
     */
    public static boolean hasSkillPermission(UUID playerId, String skillId) {
        return hasPermission(playerId, getSkillPermission(skillId));
    }

    // ==================== Permission String Generation ====================

    /**
     * Gets the permission string for a specific skill.
     *
     * @param skillId the unique identifier of the skill
     * @return the permission string in the format "rpg.skill.{skillId}"
     */
    public static String getSkillPermission(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return SKILL_PERMISSION_PREFIX;
        }
        return SKILL_PERMISSION_PREFIX + skillId.toLowerCase();
    }

    /**
     * Gets the permission string for a skill tree.
     *
     * @param treeId the unique identifier of the skill tree
     * @return the permission string in the format "rpg.skill.tree.{treeId}"
     */
    public static String getSkillTreePermission(String treeId) {
        if (treeId == null || treeId.isEmpty()) {
            return "rpg.skill.tree.";
        }
        return "rpg.skill.tree." + treeId.toLowerCase();
    }

    // ==================== Permission Management ====================

    /**
     * Grants a skill permission to a player.
     * <p>
     * This is a placeholder implementation using in-memory storage.
     * In production, this should integrate with the actual permission system.
     * </p>
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill to grant permission for
     */
    public static void grantSkillPermission(UUID playerId, String skillId) {
        if (playerId == null || skillId == null || skillId.isEmpty()) {
            return;
        }
        String permission = getSkillPermission(skillId);
        grantPermission(playerId, permission);
    }

    /**
     * Revokes a skill permission from a player.
     * <p>
     * This is a placeholder implementation using in-memory storage.
     * In production, this should integrate with the actual permission system.
     * </p>
     *
     * @param playerId the UUID of the player
     * @param skillId  the ID of the skill to revoke permission for
     */
    public static void revokeSkillPermission(UUID playerId, String skillId) {
        if (playerId == null || skillId == null || skillId.isEmpty()) {
            return;
        }
        String permission = getSkillPermission(skillId);
        revokePermission(playerId, permission);
    }

    /**
     * Grants a generic permission to a player.
     *
     * @param playerId   the UUID of the player
     * @param permission the permission string to grant
     */
    public static void grantPermission(UUID playerId, String permission) {
        if (playerId == null || permission == null || permission.isEmpty()) {
            return;
        }
        playerPermissions.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(permission);
    }

    /**
     * Revokes a generic permission from a player.
     *
     * @param playerId   the UUID of the player
     * @param permission the permission string to revoke
     */
    public static void revokePermission(UUID playerId, String permission) {
        if (playerId == null || permission == null || permission.isEmpty()) {
            return;
        }
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.remove(permission);
        }
    }

    /**
     * Gets all permissions granted to a player.
     *
     * @param playerId the UUID of the player
     * @return an unmodifiable set of permission strings
     */
    public static Set<String> getPlayerPermissions(UUID playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }
        Set<String> perms = playerPermissions.get(playerId);
        if (perms == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(perms));
    }

    /**
     * Clears all permissions for a player.
     *
     * @param playerId the UUID of the player
     */
    public static void clearPlayerPermissions(UUID playerId) {
        if (playerId != null) {
            playerPermissions.remove(playerId);
        }
    }

    /**
     * Clears all stored permission data.
     * <p>
     * This is primarily useful for testing or server shutdown cleanup.
     * </p>
     */
    public static void clearAllPermissions() {
        playerPermissions.clear();
    }
}
