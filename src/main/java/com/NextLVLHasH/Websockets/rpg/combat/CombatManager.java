package com.NextLVLHasH.Websockets.rpg.combat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Singleton manager for the combat system.
 * Handles effect registration, application, tick processing, and damage events.
 * 
 * <p>The manager provides:
 * <ul>
 *     <li>Effect definition storage and retrieval</li>
 *     <li>Active effect tracking per entity</li>
 *     <li>Periodic tick processing for DOT/HOT effects</li>
 *     <li>Damage event firing and listener management</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * CombatManager manager = CombatManager.getInstance();
 * manager.loadEffects(Paths.get("config/effects"));
 * 
 * // Apply an effect
 * manager.applyEffect(targetUUID, sourceUUID, "poison_strong", 10000);
 * 
 * // Process ticks (call periodically)
 * manager.tickEffects();
 * 
 * // Deal damage
 * manager.dealDamage(damage);
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public final class CombatManager {

    private static final Logger LOGGER = Logger.getLogger(CombatManager.class.getName());
    private static volatile CombatManager instance;
    private static final Object LOCK = new Object();

    // Effect definitions loaded from config
    private final Map<String, CombatEffect> effects;

    // Active effects per entity (UUID -> list of active effect instances)
    private final Map<UUID, List<EffectInstance>> activeEffects;

    // Damage event listeners
    private final List<DamageEventListener> damageListeners;

    // Configuration
    private Path effectsDirectory;
    private boolean initialized;

    /**
     * Private constructor for singleton pattern.
     */
    private CombatManager() {
        this.effects = new ConcurrentHashMap<>();
        this.activeEffects = new ConcurrentHashMap<>();
        this.damageListeners = new CopyOnWriteArrayList<>();
        this.initialized = false;
    }

    /**
     * Gets the singleton instance of CombatManager.
     *
     * @return the singleton CombatManager instance
     */
    public static CombatManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CombatManager();
                }
            }
        }
        return instance;
    }

    /**
     * Resets the singleton instance (primarily for testing).
     */
    public static void resetInstance() {
        synchronized (LOCK) {
            if (instance != null) {
                instance.shutdown();
                instance = null;
            }
        }
    }

    // ========== Initialization ==========

    /**
     * Initializes the combat manager with the default effects directory.
     */
    public void initialize() {
        initialize(Paths.get("config/effects"));
    }

    /**
     * Initializes the combat manager with a custom effects directory.
     *
     * @param effectsDirectory the path to the effects configuration directory
     */
    public void initialize(Path effectsDirectory) {
        if (initialized) {
            LOGGER.warning("CombatManager is already initialized");
            return;
        }
        
        this.effectsDirectory = effectsDirectory;
        loadEffects();
        registerBuiltInEffects();
        initialized = true;
        
        LOGGER.info("CombatManager initialized with " + effects.size() + " effects");
    }

    /**
     * Shuts down the combat manager, clearing all data.
     */
    public void shutdown() {
        effects.clear();
        activeEffects.clear();
        damageListeners.clear();
        initialized = false;
        LOGGER.info("CombatManager shut down");
    }

    /**
     * Checks if the manager is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    // ========== Effect Loading ==========

    /**
     * Loads all effect definitions from the configured directory.
     * Expects JSON files in the effects directory.
     */
    public void loadEffects() {
        if (effectsDirectory == null) {
            LOGGER.warning("Effects directory not set");
            return;
        }
        
        if (!Files.exists(effectsDirectory)) {
            LOGGER.info("Effects directory does not exist, creating: " + effectsDirectory);
            try {
                Files.createDirectories(effectsDirectory);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create effects directory", e);
                return;
            }
        }
        
        int loaded = 0;
        int failed = 0;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(effectsDirectory, "*.json")) {
            for (Path file : stream) {
                try {
                    CombatEffect effect = CombatEffect.fromJson(file);
                    registerEffect(effect);
                    loaded++;
                    LOGGER.fine("Loaded effect: " + effect.getId() + " from " + file.getFileName());
                } catch (Exception e) {
                    failed++;
                    LOGGER.log(Level.WARNING, "Failed to load effect from: " + file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading effects directory", e);
        }
        
        LOGGER.info("Loaded " + loaded + " effects" + (failed > 0 ? " (" + failed + " failed)" : ""));
    }

    /**
     * Reloads all effect definitions.
     * Clears existing effects and loads fresh from config.
     */
    public void reloadEffects() {
        effects.clear();
        loadEffects();
        registerBuiltInEffects();
        LOGGER.info("Effects reloaded: " + effects.size() + " total");
    }

    /**
     * Registers built-in/default effects that are always available.
     */
    private void registerBuiltInEffects() {
        // Basic poison effect
        if (!effects.containsKey("poison_basic")) {
            registerEffect(CombatEffect.builder()
                    .id("poison_basic")
                    .displayName("Poison")
                    .description("Deals damage over time")
                    .type(EffectType.DOT)
                    .category(EffectCategory.POISON)
                    .duration(10000)
                    .tickInterval(1000)
                    .damagePerTick(5.0)
                    .damageType(DamageType.POISON)
                    .stackable(true)
                    .maxStacks(5)
                    .build());
        }

        // Basic burn effect
        if (!effects.containsKey("burn_basic")) {
            registerEffect(CombatEffect.builder()
                    .id("burn_basic")
                    .displayName("Burning")
                    .description("Taking fire damage over time")
                    .type(EffectType.DOT)
                    .category(EffectCategory.BURN)
                    .duration(5000)
                    .tickInterval(500)
                    .damagePerTick(8.0)
                    .damageType(DamageType.FIRE)
                    .stackable(false)
                    .build());
        }

        // Basic regeneration effect
        if (!effects.containsKey("regen_basic")) {
            registerEffect(CombatEffect.builder()
                    .id("regen_basic")
                    .displayName("Regeneration")
                    .description("Slowly regenerating health")
                    .type(EffectType.HOT)
                    .category(EffectCategory.MAGIC)
                    .duration(15000)
                    .tickInterval(1000)
                    .healingPerTick(3.0)
                    .stackable(false)
                    .build());
        }

        // Basic stun effect
        if (!effects.containsKey("stun_basic")) {
            registerEffect(CombatEffect.builder()
                    .id("stun_basic")
                    .displayName("Stunned")
                    .description("Unable to move or act")
                    .type(EffectType.CROWD_CONTROL)
                    .category(EffectCategory.STUN)
                    .duration(2000)
                    .stackable(false)
                    .build());
        }
    }

    // ========== Effect Registration ==========

    /**
     * Registers an effect definition.
     *
     * @param effect the effect to register
     */
    public void registerEffect(CombatEffect effect) {
        Objects.requireNonNull(effect, "Effect cannot be null");
        effects.put(effect.getId(), effect);
    }

    /**
     * Unregisters an effect definition.
     *
     * @param effectId the ID of the effect to unregister
     * @return the removed effect, or null if not found
     */
    public CombatEffect unregisterEffect(String effectId) {
        return effects.remove(effectId);
    }

    /**
     * Gets an effect definition by ID.
     *
     * @param effectId the effect ID
     * @return the CombatEffect, or null if not found
     */
    public CombatEffect getEffect(String effectId) {
        return effects.get(effectId);
    }

    /**
     * Checks if an effect is registered.
     *
     * @param effectId the effect ID
     * @return true if the effect exists
     */
    public boolean hasEffect(String effectId) {
        return effects.containsKey(effectId);
    }

    /**
     * Gets all registered effect IDs.
     *
     * @return unmodifiable set of effect IDs
     */
    public java.util.Set<String> getEffectIds() {
        return Collections.unmodifiableSet(effects.keySet());
    }

    /**
     * Gets all registered effects.
     *
     * @return unmodifiable collection of effects
     */
    public java.util.Collection<CombatEffect> getAllEffects() {
        return Collections.unmodifiableCollection(effects.values());
    }

    // ========== Effect Application ==========

    /**
     * Applies an effect to a target using the effect's default duration.
     *
     * @param targetId the target's UUID
     * @param sourceId the source's UUID (who applied it)
     * @param effectId the effect ID to apply
     * @return the created EffectInstance, or null if failed
     */
    public EffectInstance applyEffect(UUID targetId, UUID sourceId, String effectId) {
        return applyEffect(targetId, sourceId, effectId, -1);
    }

    /**
     * Applies an effect to a target with a custom duration.
     *
     * @param targetId the target's UUID
     * @param sourceId the source's UUID (who applied it)
     * @param effectId the effect ID to apply
     * @param durationMillis custom duration in milliseconds (use -1 for default)
     * @return the created EffectInstance, or null if failed
     */
    public EffectInstance applyEffect(UUID targetId, UUID sourceId, String effectId, int durationMillis) {
        Objects.requireNonNull(targetId, "Target ID cannot be null");
        Objects.requireNonNull(effectId, "Effect ID cannot be null");
        
        CombatEffect effect = effects.get(effectId);
        if (effect == null) {
            LOGGER.warning("Unknown effect ID: " + effectId);
            return null;
        }
        
        List<EffectInstance> targetEffects = activeEffects.computeIfAbsent(
                targetId, k -> new CopyOnWriteArrayList<>());
        
        // Check for existing effect of same type
        EffectInstance existing = findActiveEffect(targetId, effectId);
        
        if (existing != null) {
            if (effect.isStackable()) {
                // Add stack and refresh duration
                existing.addStack();
                LOGGER.fine("Added stack to effect " + effectId + " on " + targetId + 
                           ", now " + existing.getStacks() + " stacks");
                return existing;
            } else {
                // Refresh duration for non-stackable
                existing.refreshDuration();
                LOGGER.fine("Refreshed effect " + effectId + " on " + targetId);
                return existing;
            }
        }
        
        // Create new instance
        EffectInstance instance = new EffectInstance(effect, targetId, sourceId, durationMillis);
        targetEffects.add(instance);
        
        LOGGER.fine("Applied effect " + effectId + " to " + targetId);
        return instance;
    }

    /**
     * Removes an effect from a target.
     *
     * @param targetId the target's UUID
     * @param effectId the effect ID to remove
     * @return true if an effect was removed
     */
    public boolean removeEffect(UUID targetId, String effectId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return false;
        
        boolean removed = targetEffects.removeIf(instance -> instance.getEffectId().equals(effectId));
        
        if (removed) {
            LOGGER.fine("Removed effect " + effectId + " from " + targetId);
        }
        
        // Clean up empty lists
        if (targetEffects.isEmpty()) {
            activeEffects.remove(targetId);
        }
        
        return removed;
    }

    /**
     * Removes all effects from a target.
     *
     * @param targetId the target's UUID
     * @return the number of effects removed
     */
    public int removeAllEffects(UUID targetId) {
        List<EffectInstance> removed = activeEffects.remove(targetId);
        int count = removed != null ? removed.size() : 0;
        
        if (count > 0) {
            LOGGER.fine("Removed all " + count + " effects from " + targetId);
        }
        
        return count;
    }

    /**
     * Removes effects that should be removed on death.
     *
     * @param targetId the target's UUID
     * @return the number of effects removed
     */
    public int removeEffectsOnDeath(UUID targetId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return 0;
        
        int before = targetEffects.size();
        targetEffects.removeIf(EffectInstance::shouldRemoveOnDeath);
        int removed = before - targetEffects.size();
        
        if (targetEffects.isEmpty()) {
            activeEffects.remove(targetId);
        }
        
        return removed;
    }

    /**
     * Dispels effects of a specific category from a target.
     *
     * @param targetId the target's UUID
     * @param category the category to dispel
     * @return the number of effects dispelled
     */
    public int dispelEffects(UUID targetId, EffectCategory category) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return 0;
        
        int before = targetEffects.size();
        targetEffects.removeIf(instance -> 
                instance.canDispel() && instance.getEffect().getCategory() == category);
        int removed = before - targetEffects.size();
        
        if (targetEffects.isEmpty()) {
            activeEffects.remove(targetId);
        }
        
        LOGGER.fine("Dispelled " + removed + " " + category + " effects from " + targetId);
        return removed;
    }

    /**
     * Dispels all dispellable effects from a target.
     *
     * @param targetId the target's UUID
     * @return the number of effects dispelled
     */
    public int dispelAllEffects(UUID targetId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return 0;
        
        int before = targetEffects.size();
        targetEffects.removeIf(EffectInstance::canDispel);
        int removed = before - targetEffects.size();
        
        if (targetEffects.isEmpty()) {
            activeEffects.remove(targetId);
        }
        
        return removed;
    }

    // ========== Effect Queries ==========

    /**
     * Gets all active effects on a target.
     *
     * @param targetId the target's UUID
     * @return unmodifiable list of active effects
     */
    public List<EffectInstance> getActiveEffects(UUID targetId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(targetEffects));
    }

    /**
     * Finds a specific active effect on a target.
     *
     * @param targetId the target's UUID
     * @param effectId the effect ID to find
     * @return the EffectInstance, or null if not found
     */
    public EffectInstance findActiveEffect(UUID targetId, String effectId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return null;
        
        return targetEffects.stream()
                .filter(instance -> instance.getEffectId().equals(effectId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a target has a specific effect.
     *
     * @param targetId the target's UUID
     * @param effectId the effect ID to check
     * @return true if the target has this effect
     */
    public boolean hasActiveEffect(UUID targetId, String effectId) {
        return findActiveEffect(targetId, effectId) != null;
    }

    /**
     * Gets active effects of a specific type on a target.
     *
     * @param targetId the target's UUID
     * @param type the effect type to filter by
     * @return list of matching effects
     */
    public List<EffectInstance> getActiveEffectsByType(UUID targetId, EffectType type) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return Collections.emptyList();
        
        return targetEffects.stream()
                .filter(instance -> instance.getEffect().getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Gets active effects of a specific category on a target.
     *
     * @param targetId the target's UUID
     * @param category the effect category to filter by
     * @return list of matching effects
     */
    public List<EffectInstance> getActiveEffectsByCategory(UUID targetId, EffectCategory category) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return Collections.emptyList();
        
        return targetEffects.stream()
                .filter(instance -> instance.getEffect().getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a target is affected by any crowd control.
     *
     * @param targetId the target's UUID
     * @return true if under crowd control
     */
    public boolean isUnderCrowdControl(UUID targetId) {
        return !getActiveEffectsByType(targetId, EffectType.CROWD_CONTROL).isEmpty();
    }

    /**
     * Checks if a target is movement impaired.
     *
     * @param targetId the target's UUID
     * @return true if movement is impaired
     */
    public boolean isMovementImpaired(UUID targetId) {
        List<EffectInstance> targetEffects = activeEffects.get(targetId);
        if (targetEffects == null) return false;
        
        return targetEffects.stream()
                .anyMatch(instance -> instance.getEffect().getCategory().isMovementImpairing());
    }

    // ========== Tick Processing ==========

    /**
     * Processes all active effects, handling ticks and expiration.
     * Should be called periodically (e.g., every 100ms).
     *
     * @return a summary of the tick processing
     */
    public TickSummary tickEffects() {
        int entitiesProcessed = 0;
        int effectsProcessed = 0;
        int effectsExpired = 0;
        double totalDamage = 0;
        double totalHealing = 0;
        
        List<TickDamage> damageDealt = new ArrayList<>();
        List<TickHealing> healingApplied = new ArrayList<>();
        
        for (Map.Entry<UUID, List<EffectInstance>> entry : activeEffects.entrySet()) {
            UUID targetId = entry.getKey();
            List<EffectInstance> targetEffects = entry.getValue();
            
            if (targetEffects.isEmpty()) continue;
            entitiesProcessed++;
            
            Iterator<EffectInstance> iterator = targetEffects.iterator();
            while (iterator.hasNext()) {
                EffectInstance instance = iterator.next();
                effectsProcessed++;
                
                // Check expiration
                if (instance.isExpired()) {
                    iterator.remove();
                    effectsExpired++;
                    continue;
                }
                
                // Process tick
                EffectInstance.TickResult result = instance.tick();
                if (result != null) {
                    if (result.hasDamage()) {
                        damageDealt.add(new TickDamage(targetId, instance.getSourceId(), 
                                result.getDamage(), result.getDamageType(), instance.getEffectId()));
                        totalDamage += result.getDamage();
                    }
                    if (result.hasHealing()) {
                        healingApplied.add(new TickHealing(targetId, instance.getSourceId(),
                                result.getHealing(), instance.getEffectId()));
                        totalHealing += result.getHealing();
                    }
                }
            }
        }
        
        // Clean up empty effect lists
        activeEffects.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        return new TickSummary(entitiesProcessed, effectsProcessed, effectsExpired,
                totalDamage, totalHealing, damageDealt, healingApplied);
    }

    // ========== Damage Processing ==========

    /**
     * Deals damage and fires the damage event to all listeners.
     * Listeners can modify or cancel the damage.
     *
     * @param damage the Damage to deal
     * @return the DamageEvent after processing (check isCancelled)
     */
    public DamageEvent dealDamage(Damage damage) {
        Objects.requireNonNull(damage, "Damage cannot be null");
        
        DamageEvent event = new DamageEvent(damage);
        
        // Fire to all listeners
        for (DamageEventListener listener : damageListeners) {
            try {
                listener.onDamage(event);
                if (event.isCancelled()) break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in damage listener", e);
            }
        }
        
        return event;
    }

    // ========== Listener Management ==========

    /**
     * Adds a damage event listener.
     *
     * @param listener the listener to add
     */
    public void addDamageListener(DamageEventListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        damageListeners.add(listener);
    }

    /**
     * Removes a damage event listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed
     */
    public boolean removeDamageListener(DamageEventListener listener) {
        return damageListeners.remove(listener);
    }

    /**
     * Clears all damage event listeners.
     */
    public void clearDamageListeners() {
        damageListeners.clear();
    }

    // ========== Inner Classes ==========

    /**
     * Listener interface for damage events.
     */
    @FunctionalInterface
    public interface DamageEventListener {
        /**
         * Called when damage is dealt.
         *
         * @param event the damage event
         */
        void onDamage(DamageEvent event);
    }

    /**
     * Summary of a tick processing cycle.
     */
    public static final class TickSummary {
        private final int entitiesProcessed;
        private final int effectsProcessed;
        private final int effectsExpired;
        private final double totalDamage;
        private final double totalHealing;
        private final List<TickDamage> damageDealt;
        private final List<TickHealing> healingApplied;

        public TickSummary(int entitiesProcessed, int effectsProcessed, int effectsExpired,
                          double totalDamage, double totalHealing,
                          List<TickDamage> damageDealt, List<TickHealing> healingApplied) {
            this.entitiesProcessed = entitiesProcessed;
            this.effectsProcessed = effectsProcessed;
            this.effectsExpired = effectsExpired;
            this.totalDamage = totalDamage;
            this.totalHealing = totalHealing;
            this.damageDealt = Collections.unmodifiableList(damageDealt);
            this.healingApplied = Collections.unmodifiableList(healingApplied);
        }

        public int getEntitiesProcessed() { return entitiesProcessed; }
        public int getEffectsProcessed() { return effectsProcessed; }
        public int getEffectsExpired() { return effectsExpired; }
        public double getTotalDamage() { return totalDamage; }
        public double getTotalHealing() { return totalHealing; }
        public List<TickDamage> getDamageDealt() { return damageDealt; }
        public List<TickHealing> getHealingApplied() { return healingApplied; }

        @Override
        public String toString() {
            return "TickSummary{entities=" + entitiesProcessed + 
                   ", effects=" + effectsProcessed + 
                   ", expired=" + effectsExpired +
                   ", damage=" + totalDamage + 
                   ", healing=" + totalHealing + '}';
        }
    }

    /**
     * Record of damage dealt during a tick.
     */
    public static final class TickDamage {
        private final UUID targetId;
        private final UUID sourceId;
        private final double amount;
        private final DamageType type;
        private final String effectId;

        public TickDamage(UUID targetId, UUID sourceId, double amount, DamageType type, String effectId) {
            this.targetId = targetId;
            this.sourceId = sourceId;
            this.amount = amount;
            this.type = type;
            this.effectId = effectId;
        }

        public UUID getTargetId() { return targetId; }
        public UUID getSourceId() { return sourceId; }
        public double getAmount() { return amount; }
        public DamageType getType() { return type; }
        public String getEffectId() { return effectId; }
    }

    /**
     * Record of healing applied during a tick.
     */
    public static final class TickHealing {
        private final UUID targetId;
        private final UUID sourceId;
        private final double amount;
        private final String effectId;

        public TickHealing(UUID targetId, UUID sourceId, double amount, String effectId) {
            this.targetId = targetId;
            this.sourceId = sourceId;
            this.amount = amount;
            this.effectId = effectId;
        }

        public UUID getTargetId() { return targetId; }
        public UUID getSourceId() { return sourceId; }
        public double getAmount() { return amount; }
        public String getEffectId() { return effectId; }
    }
}
