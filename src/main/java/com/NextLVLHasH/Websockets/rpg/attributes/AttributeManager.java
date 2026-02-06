package com.NextLVLHasH.Websockets.rpg.attributes;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for player attributes.
 * Handles attribute sets for all players, modifier application and removal,
 * derived stat calculations, and periodic cleanup of expired modifiers.
 * 
 * <p>This manager is thread-safe and can be accessed from multiple threads.
 * 
 * <p>Usage example:
 * <pre>
 * AttributeManager manager = AttributeManager.getInstance();
 * 
 * // Initialize a player with default attributes
 * manager.initializePlayer(playerId, Map.of(
 *     AttributeType.STRENGTH, 12,
 *     AttributeType.DEXTERITY, 14,
 *     AttributeType.INTELLIGENCE, 10,
 *     AttributeType.CONSTITUTION, 13,
 *     AttributeType.WISDOM, 11,
 *     AttributeType.CHARISMA, 8
 * ));
 * 
 * // Add a buff
 * manager.addModifier(playerId, AttributeModifier.builder()
 *     .source("Potion of Strength")
 *     .attribute(AttributeType.STRENGTH)
 *     .type(ModifierType.FLAT)
 *     .value(5)
 *     .duration(60000)
 *     .build());
 * 
 * // Get calculated values
 * int strength = manager.getAttribute(playerId, AttributeType.STRENGTH);
 * double maxHealth = manager.getDerivedStat(playerId, DerivedStat.MAX_HEALTH, 10);
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class AttributeManager {

    private static final Logger LOGGER = Logger.getLogger(AttributeManager.class.getName());
    private static final int DEFAULT_BASE_VALUE = 10;

    private static volatile AttributeManager instance;
    private static final Object LOCK = new Object();

    private final Map<UUID, AttributeSet> playerAttributes;
    private final List<BiConsumer<UUID, AttributeModifier>> modifierAddedListeners;
    private final List<BiConsumer<UUID, AttributeModifier>> modifierRemovedListeners;
    private final List<BiConsumer<UUID, AttributeType>> attributeChangedListeners;

    /**
     * Private constructor for singleton pattern.
     */
    private AttributeManager() {
        this.playerAttributes = new ConcurrentHashMap<>();
        this.modifierAddedListeners = new ArrayList<>();
        this.modifierRemovedListeners = new ArrayList<>();
        this.attributeChangedListeners = new ArrayList<>();
    }

    /**
     * Gets the singleton instance of AttributeManager.
     *
     * @return the AttributeManager instance
     */
    public static AttributeManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AttributeManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (mainly for testing purposes).
     */
    public static void resetInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /**
     * Initializes a player with the specified base attributes.
     * If the player already exists, their attributes will be reset.
     *
     * @param playerId the player's UUID
     * @param baseAttributes map of attribute types to base values
     */
    public void initializePlayer(UUID playerId, Map<AttributeType, Integer> baseAttributes) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        AttributeSet attributeSet = new AttributeSet(baseAttributes != null ? baseAttributes : Collections.emptyMap());
        playerAttributes.put(playerId, attributeSet);
        
        LOGGER.fine(() -> "Initialized attributes for player " + playerId);
    }

    /**
     * Initializes a player with default base attributes.
     *
     * @param playerId the player's UUID
     */
    public void initializePlayer(UUID playerId) {
        initializePlayer(playerId, null);
    }

    /**
     * Removes a player from the attribute system.
     *
     * @param playerId the player's UUID
     * @return the removed AttributeSet, or null if the player wasn't tracked
     */
    public AttributeSet removePlayer(UUID playerId) {
        AttributeSet removed = playerAttributes.remove(playerId);
        if (removed != null) {
            LOGGER.fine(() -> "Removed attributes for player " + playerId);
        }
        return removed;
    }

    /**
     * Checks if a player is being tracked.
     *
     * @param playerId the player's UUID
     * @return true if the player has an AttributeSet
     */
    public boolean hasPlayer(UUID playerId) {
        return playerAttributes.containsKey(playerId);
    }

    /**
     * Gets the AttributeSet for a player.
     * If the player isn't tracked, initializes them with defaults.
     *
     * @param playerId the player's UUID
     * @return the player's AttributeSet
     */
    public AttributeSet getAttributeSet(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return playerAttributes.computeIfAbsent(playerId, id -> {
            LOGGER.fine(() -> "Auto-initializing attributes for player " + id);
            return new AttributeSet(DEFAULT_BASE_VALUE);
        });
    }

    /**
     * Gets the AttributeSet for a player without auto-creating.
     *
     * @param playerId the player's UUID
     * @return Optional containing the AttributeSet if present
     */
    public Optional<AttributeSet> getAttributeSetOptional(UUID playerId) {
        return Optional.ofNullable(playerAttributes.get(playerId));
    }

    /**
     * Gets the calculated value of an attribute for a player.
     *
     * @param playerId the player's UUID
     * @param type the attribute type
     * @return the calculated attribute value
     */
    public int getAttribute(UUID playerId, AttributeType type) {
        return getAttributeSet(playerId).getValue(type);
    }

    /**
     * Gets the base value of an attribute for a player.
     *
     * @param playerId the player's UUID
     * @param type the attribute type
     * @return the base attribute value
     */
    public int getBaseAttribute(UUID playerId, AttributeType type) {
        return getAttributeSet(playerId).getBaseValue(type);
    }

    /**
     * Sets the base value of an attribute for a player.
     *
     * @param playerId the player's UUID
     * @param type the attribute type
     * @param value the new base value
     */
    public void setBaseAttribute(UUID playerId, AttributeType type, int value) {
        getAttributeSet(playerId).setBaseValue(type, value);
        notifyAttributeChanged(playerId, type);
    }

    /**
     * Adds a modifier to a player's attributes.
     *
     * @param playerId the player's UUID
     * @param modifier the modifier to add
     */
    public void addModifier(UUID playerId, AttributeModifier modifier) {
        Objects.requireNonNull(modifier, "Modifier cannot be null");
        
        AttributeSet attributeSet = getAttributeSet(playerId);
        attributeSet.addModifier(modifier);
        
        LOGGER.fine(() -> "Added modifier " + modifier.getId() + " to player " + playerId);
        notifyModifierAdded(playerId, modifier);
        notifyAttributeChanged(playerId, modifier.getAttribute());
    }

    /**
     * Adds multiple modifiers to a player's attributes.
     *
     * @param playerId the player's UUID
     * @param modifiers the collection of modifiers to add
     */
    public void addModifiers(UUID playerId, Collection<AttributeModifier> modifiers) {
        AttributeSet attributeSet = getAttributeSet(playerId);
        Set<AttributeType> changedAttributes = new HashSet<>();
        
        for (AttributeModifier modifier : modifiers) {
            attributeSet.addModifier(modifier);
            notifyModifierAdded(playerId, modifier);
            changedAttributes.add(modifier.getAttribute());
        }
        
        for (AttributeType type : changedAttributes) {
            notifyAttributeChanged(playerId, type);
        }
    }

    /**
     * Removes a modifier from a player by modifier ID.
     *
     * @param playerId the player's UUID
     * @param modifierId the ID of the modifier to remove
     * @return true if the modifier was found and removed
     */
    public boolean removeModifier(UUID playerId, String modifierId) {
        AttributeSet attributeSet = playerAttributes.get(playerId);
        if (attributeSet == null) {
            return false;
        }
        
        Optional<AttributeModifier> modifier = attributeSet.getModifier(modifierId);
        boolean removed = attributeSet.removeModifier(modifierId);
        
        if (removed && modifier.isPresent()) {
            LOGGER.fine(() -> "Removed modifier " + modifierId + " from player " + playerId);
            notifyModifierRemoved(playerId, modifier.get());
            notifyAttributeChanged(playerId, modifier.get().getAttribute());
        }
        
        return removed;
    }

    /**
     * Removes all modifiers from a specific source for a player.
     *
     * @param playerId the player's UUID
     * @param source the source to match
     * @return the number of modifiers removed
     */
    public int removeModifiersBySource(UUID playerId, String source) {
        AttributeSet attributeSet = playerAttributes.get(playerId);
        if (attributeSet == null) {
            return 0;
        }
        
        int removed = attributeSet.removeModifiersBySource(source);
        if (removed > 0) {
            LOGGER.fine(() -> "Removed " + removed + " modifiers from source '" + source + "' for player " + playerId);
            attributeSet.recalculateAll();
        }
        
        return removed;
    }

    /**
     * Calculates a derived stat for a player.
     *
     * @param playerId the player's UUID
     * @param stat the derived stat to calculate
     * @param level the player's level
     * @return the calculated stat value
     */
    public double getDerivedStat(UUID playerId, DerivedStat stat, int level) {
        AttributeSet attributeSet = getAttributeSet(playerId);
        return stat.calculate(attributeSet, level);
    }

    /**
     * Gets all derived stats for a player.
     *
     * @param playerId the player's UUID
     * @param level the player's level
     * @return map of derived stats to their calculated values
     */
    public Map<DerivedStat, Double> getAllDerivedStats(UUID playerId, int level) {
        Map<DerivedStat, Double> stats = new EnumMap<>(DerivedStat.class);
        AttributeSet attributeSet = getAttributeSet(playerId);
        
        for (DerivedStat stat : DerivedStat.values()) {
            stats.put(stat, stat.calculate(attributeSet, level));
        }
        
        return stats;
    }

    /**
     * Forces recalculation of all attributes for a player.
     *
     * @param playerId the player's UUID
     */
    public void recalculatePlayer(UUID playerId) {
        AttributeSet attributeSet = playerAttributes.get(playerId);
        if (attributeSet != null) {
            attributeSet.recalculateAll();
            LOGGER.fine(() -> "Recalculated attributes for player " + playerId);
        }
    }

    /**
     * Clears expired modifiers for all players.
     * Should be called periodically (e.g., every second).
     *
     * @return the total number of expired modifiers removed
     */
    public int tickExpiredModifiers() {
        int totalRemoved = 0;
        
        for (Map.Entry<UUID, AttributeSet> entry : playerAttributes.entrySet()) {
            UUID playerId = entry.getKey();
            AttributeSet attributeSet = entry.getValue();
            
            if (attributeSet.hasExpiredModifiers()) {
                int removed = attributeSet.clearExpiredModifiers();
                if (removed > 0) {
                    totalRemoved += removed;
                    final int removedCount = removed;
                    LOGGER.fine(() -> "Cleared " + removedCount + " expired modifiers for player " + playerId);
                }
            }
        }
        
        if (totalRemoved > 0) {
            final int finalTotal = totalRemoved;
            LOGGER.fine(() -> "Total expired modifiers cleared: " + finalTotal);
        }
        
        return totalRemoved;
    }

    /**
     * Clears expired modifiers for a specific player.
     *
     * @param playerId the player's UUID
     * @return the number of expired modifiers removed
     */
    public int clearExpiredModifiers(UUID playerId) {
        AttributeSet attributeSet = playerAttributes.get(playerId);
        if (attributeSet != null) {
            return attributeSet.clearExpiredModifiers();
        }
        return 0;
    }

    /**
     * Gets the number of players being tracked.
     *
     * @return the player count
     */
    public int getPlayerCount() {
        return playerAttributes.size();
    }

    /**
     * Gets all tracked player IDs.
     *
     * @return unmodifiable set of player UUIDs
     */
    public Set<UUID> getTrackedPlayers() {
        return Collections.unmodifiableSet(playerAttributes.keySet());
    }

    /**
     * Registers a listener for modifier added events.
     *
     * @param listener the listener to add
     */
    public void addModifierAddedListener(BiConsumer<UUID, AttributeModifier> listener) {
        modifierAddedListeners.add(listener);
    }

    /**
     * Registers a listener for modifier removed events.
     *
     * @param listener the listener to add
     */
    public void addModifierRemovedListener(BiConsumer<UUID, AttributeModifier> listener) {
        modifierRemovedListeners.add(listener);
    }

    /**
     * Registers a listener for attribute changed events.
     *
     * @param listener the listener to add
     */
    public void addAttributeChangedListener(BiConsumer<UUID, AttributeType> listener) {
        attributeChangedListeners.add(listener);
    }

    /**
     * Removes a modifier added listener.
     *
     * @param listener the listener to remove
     */
    public void removeModifierAddedListener(BiConsumer<UUID, AttributeModifier> listener) {
        modifierAddedListeners.remove(listener);
    }

    /**
     * Removes a modifier removed listener.
     *
     * @param listener the listener to remove
     */
    public void removeModifierRemovedListener(BiConsumer<UUID, AttributeModifier> listener) {
        modifierRemovedListeners.remove(listener);
    }

    /**
     * Removes an attribute changed listener.
     *
     * @param listener the listener to remove
     */
    public void removeAttributeChangedListener(BiConsumer<UUID, AttributeType> listener) {
        attributeChangedListeners.remove(listener);
    }

    /**
     * Clears all listeners.
     */
    public void clearListeners() {
        modifierAddedListeners.clear();
        modifierRemovedListeners.clear();
        attributeChangedListeners.clear();
    }

    private void notifyModifierAdded(UUID playerId, AttributeModifier modifier) {
        for (BiConsumer<UUID, AttributeModifier> listener : modifierAddedListeners) {
            try {
                listener.accept(playerId, modifier);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in modifier added listener", e);
            }
        }
    }

    private void notifyModifierRemoved(UUID playerId, AttributeModifier modifier) {
        for (BiConsumer<UUID, AttributeModifier> listener : modifierRemovedListeners) {
            try {
                listener.accept(playerId, modifier);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in modifier removed listener", e);
            }
        }
    }

    private void notifyAttributeChanged(UUID playerId, AttributeType type) {
        for (BiConsumer<UUID, AttributeType> listener : attributeChangedListeners) {
            try {
                listener.accept(playerId, type);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in attribute changed listener", e);
            }
        }
    }

    /**
     * Gets a summary of a player's attributes for debugging.
     *
     * @param playerId the player's UUID
     * @return formatted summary string
     */
    public String getPlayerSummary(UUID playerId) {
        AttributeSet attributeSet = playerAttributes.get(playerId);
        if (attributeSet == null) {
            return "Player " + playerId + " not found";
        }
        return "Player " + playerId + ":\n" + attributeSet.getSummary();
    }

    /**
     * Creates a temporary modifier for testing or preview purposes.
     * Does not actually apply the modifier.
     *
     * @param attribute the target attribute
     * @param type the modifier type
     * @param value the modifier value
     * @param source the source name
     * @return the created modifier
     */
    public AttributeModifier createTemporaryModifier(
            AttributeType attribute, 
            ModifierType type, 
            double value, 
            String source) {
        return AttributeModifier.builder()
                .attribute(attribute)
                .type(type)
                .value(value)
                .source(source)
                .permanent()
                .build();
    }

    /**
     * Simulates applying a modifier without actually changing the player's attributes.
     * Useful for previewing buff effects.
     *
     * @param playerId the player's UUID
     * @param modifier the modifier to simulate
     * @return map of attribute types to their would-be values
     */
    public Map<AttributeType, Integer> simulateModifier(UUID playerId, AttributeModifier modifier) {
        AttributeSet original = getAttributeSet(playerId);
        AttributeSet simulation = original.createSnapshot();
        
        // Create a new modifier targeting the same attribute
        simulation.setBaseValue(modifier.getAttribute(), original.getValue(modifier.getAttribute()));
        
        AttributeModifier simMod = modifier.toBuilder()
                .id("simulation_" + UUID.randomUUID())
                .build();
        simulation.addModifier(simMod);
        
        return simulation.getAllValues();
    }
}
