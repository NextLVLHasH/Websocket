package com.NextLVLHasH.Websockets.rpg.ui;

import com.NextLVLHasH.Websockets.rpg.combat.Damage;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * UI handler for the combat log display using Hytale's native UI system.
 * Uses a custom .ui layout file for styled combat log entries.
 * Does NOT use Minecraft-style color codes (which Hytale does not support).
 */
@SuppressWarnings("unused")
public class CombatLogUI {

    private static final Logger LOGGER = Logger.getLogger(CombatLogUI.class.getName());
    
    /** Path to the combat log UI layout file */
    private static final String UI_LAYOUT_PATH = "CombatLog.ui";
    
    /** Maximum entries to store in the log */
    private static final int MAX_ENTRIES = 50;
    
    /** Number of visible entries in the UI */
    private static final int VISIBLE_ENTRIES = 10;
    
    /** Supplier to get online players map from the main plugin */
    private static Supplier<Map<String, PlayerRef>> onlinePlayersSupplier;
    
    /** Supplier to get Player component from PlayerRef */
    private static Supplier<RefStoreSupplier> refStoreSupplier;
    
    /** Time format for timestamps */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final UUID playerId;
    private final Deque<LogEntry> entries;
    private boolean isVisible;
    private CombatLogPage activePage;

    /**
     * Interface for providing Ref and Store for ECS operations.
     */
    public interface RefStoreSupplier {
        Ref<EntityStore> getRef(PlayerRef playerRef);
        Store<EntityStore> getStore();
    }

    /**
     * Sets the supplier for getting online players.
     * Must be called during plugin initialization.
     *
     * @param supplier a supplier that returns the online players map (UUID string -> PlayerRef)
     */
    public static void setOnlinePlayersSupplier(Supplier<Map<String, PlayerRef>> supplier) {
        onlinePlayersSupplier = supplier;
        LOGGER.fine("CombatLogUI initialized with online players supplier");
    }

    /**
     * Sets the supplier for getting Ref/Store for ECS operations.
     * Must be called during plugin initialization if using UI pages.
     *
     * @param supplier a supplier that returns RefStoreSupplier
     */
    public static void setRefStoreSupplier(Supplier<RefStoreSupplier> supplier) {
        refStoreSupplier = supplier;
        LOGGER.fine("CombatLogUI initialized with Ref/Store supplier");
    }

    /**
     * Gets the PlayerRef for a player by UUID.
     *
     * @param playerId the player's UUID
     * @return the PlayerRef if online, null otherwise
     */
    private static PlayerRef getPlayerRef(UUID playerId) {
        if (onlinePlayersSupplier == null) {
            LOGGER.warning("Online players supplier not set - call setOnlinePlayersSupplier() during initialization");
            return null;
        }
        Map<String, PlayerRef> onlinePlayers = onlinePlayersSupplier.get();
        return onlinePlayers != null ? onlinePlayers.get(playerId.toString()) : null;
    }

    public CombatLogUI(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId);
        this.entries = new ConcurrentLinkedDeque<>();
        this.isVisible = false;
        this.activePage = null;
    }

    /**
     * Shows the combat log UI page to the player.
     */
    public void show() {
        this.isVisible = true;
        openPage();
    }

    /**
     * Hides the combat log UI.
     */
    public void hide() {
        this.isVisible = false;
        closePage();
    }

    /**
     * Toggles the combat log visibility.
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }

    public boolean isVisible() { return isVisible; }
    public UUID getPlayerId() { return playerId; }

    /**
     * Opens the combat log UI page for the player.
     */
    private void openPage() {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warning("Cannot open combat log: player " + playerId + " is offline");
            return;
        }
        
        if (refStoreSupplier != null) {
            try {
                RefStoreSupplier rss = refStoreSupplier.get();
                if (rss != null) {
                    Ref<EntityStore> ref = rss.getRef(playerRef);
                    Store<EntityStore> store = rss.getStore();
                    
                    if (ref != null && store != null) {
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            activePage = new CombatLogPage(playerRef, this);
                            player.getPageManager().openCustomPage(ref, store, activePage);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to open combat log UI page: " + e.getMessage());
            }
        }
        
        LOGGER.warning("UI system not available for combat log");
    }

    /**
     * Closes the active combat log page.
     * Note: close() is protected, so we signal the page to close itself.
     */
    private void closePage() {
        if (activePage != null) {
            activePage.signalClose();
            activePage = null;
        }
    }

    /**
     * Refreshes the UI page if visible.
     */
    private void refreshPage() {
        if (isVisible && activePage != null) {
            activePage.requestRefresh();
        }
    }

    public void addDamageEntry(Damage damage) {
        if (damage == null) return;
        String attackerName = damage.getAttackerId() != null ? 
                damage.getAttackerId().toString().substring(0, 8) : "Environment";
        String text = damage.isCritical() ?
                String.format("CRITICAL! %s dealt %.0f %s damage!", attackerName, damage.getFinalAmount(), damage.getType().getDisplayName()) :
                String.format("%s dealt %.0f %s damage", attackerName, damage.getFinalAmount(), damage.getType().getDisplayName());
        addEntry(new LogEntry(LogEntryType.DAMAGE, text, damage.getType().getColor(), System.currentTimeMillis()));
    }

    public void addEffectEntry(String effectName, boolean applied) {
        String text = applied ? "Effect applied: " + effectName : "Effect removed: " + effectName;
        addEntry(new LogEntry(applied ? LogEntryType.EFFECT_APPLIED : LogEntryType.EFFECT_REMOVED, 
                text, applied ? "#55FFFF" : "#FFFF55", System.currentTimeMillis()));
    }

    public void addHealEntry(String sourceName, double amount) {
        addEntry(new LogEntry(LogEntryType.HEAL, 
                String.format("%s healed for %.0f HP", sourceName, amount), "#55FF55", System.currentTimeMillis()));
    }

    public void addLevelUpEntry(int newLevel) {
        addEntry(new LogEntry(LogEntryType.LEVEL_UP, 
                "Level Up! Now level " + newLevel, "#FFD700", System.currentTimeMillis()));
    }

    public void addSkillUsedEntry(String skillName, String targetName) {
        String text = targetName != null ? 
                String.format("Used %s on %s", skillName, targetName) :
                String.format("Used %s", skillName);
        addEntry(new LogEntry(LogEntryType.SKILL_USED, text, "#FF55FF", System.currentTimeMillis()));
    }

    public void addInfoEntry(String message) {
        addEntry(new LogEntry(LogEntryType.INFO, message, "#CCCCCC", System.currentTimeMillis()));
    }

    private void addEntry(LogEntry entry) {
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) entries.removeLast();
        refreshPage();
    }

    public List<LogEntry> getEntries() { return new ArrayList<>(entries); }
    
    public List<LogEntry> getRecentEntries(int count) {
        List<LogEntry> result = new ArrayList<>();
        int i = 0;
        for (LogEntry entry : entries) {
            if (i++ >= count) break;
            result.add(entry);
        }
        return result;
    }
    
    public void clear() { 
        entries.clear();
        refreshPage();
    }

    /**
     * Formats a timestamp for display.
     */
    static String formatTimestamp(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Gets the style name for an entry type.
     */
    static String getStyleForType(LogEntryType type) {
        return switch (type) {
            case DAMAGE -> "@DamageStyle";
            case HEAL -> "@HealStyle";
            case EFFECT_APPLIED -> "@EffectAppliedStyle";
            case EFFECT_REMOVED -> "@EffectRemovedStyle";
            case LEVEL_UP -> "@LevelUpStyle";
            case SKILL_USED -> "@SkillUsedStyle";
            case INFO -> "@InfoStyle";
        };
    }

    public enum LogEntryType { DAMAGE, HEAL, EFFECT_APPLIED, EFFECT_REMOVED, LEVEL_UP, SKILL_USED, INFO }
    
    public record LogEntry(LogEntryType type, String text, String color, long timestamp) {
        public long getAge() { return System.currentTimeMillis() - timestamp; }
    }

    /**
     * Custom UI Page for displaying the combat log with interactive buttons.
     * Uses InteractiveCustomUIPage with clear and close actions.
     */
    public static class CombatLogPage extends InteractiveCustomUIPage<CombatLogPage.CombatLogEventData> {

        private final CombatLogUI combatLog;
        private final PlayerRPGData rpgData;
        private boolean needsRefresh;
        private boolean shouldClose;

        /**
         * Creates a new CombatLogPage.
         *
         * @param playerRef the player to show the combat log to
         * @param combatLog the combat log instance to display
         */
        public CombatLogPage(PlayerRef playerRef, CombatLogUI combatLog) {
            super(playerRef, CustomPageLifetime.CanDismiss, CombatLogEventData.CODEC);
            this.combatLog = combatLog;
            this.rpgData = null;
            this.needsRefresh = false;
            this.shouldClose = false;
        }

        /**
         * Creates a new CombatLogPage without parent handler.
         * For use from commands that have direct ECS access.
         *
         * @param playerRef the player to show the combat log to
         * @param rpgData the player's RPG data (for context)
         */
        public CombatLogPage(PlayerRef playerRef, PlayerRPGData rpgData) {
            super(playerRef, CustomPageLifetime.CanDismiss, CombatLogEventData.CODEC);
            // Create a temporary CombatLogUI for this page
            this.combatLog = new CombatLogUI(playerRef.getUuid());
            this.rpgData = rpgData;
            this.needsRefresh = false;
            this.shouldClose = false;
        }

        /**
         * Requests a UI refresh on next update.
         */
        public void requestRefresh() {
            this.needsRefresh = true;
        }

        /**
         * Signals the page to close itself.
         */
        public void signalClose() {
            this.shouldClose = true;
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref,
                          @Nonnull UICommandBuilder ui,
                          @Nonnull UIEventBuilder events,
                          @Nonnull Store<EntityStore> store) {
            // Check if we should close
            if (shouldClose) {
                this.close();
                return;
            }

            // Load the .ui layout file
            ui.append(UI_LAYOUT_PATH);

            // Populate log entries
            List<LogEntry> recentEntries = combatLog.getRecentEntries(VISIBLE_ENTRIES);
            
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                String entryId = "#Entry" + i;
                
                if (i < recentEntries.size()) {
                    LogEntry entry = recentEntries.get(i);
                    
                    // Show the entry row
                    ui.set(entryId + ".Visible", true);
                    
                    // Set timestamp
                    ui.set(entryId + "_Timestamp.Text", formatTimestamp(entry.timestamp()));
                    
                    // Set entry text
                    ui.set(entryId + "_Text.Text", entry.text());
                    
                    // Apply the appropriate style based on entry type
                    ui.set(entryId + "_Text.Style", getStyleForType(entry.type()));
                } else {
                    // Hide unused entry rows
                    ui.set(entryId + ".Visible", false);
                }
            }

            // Bind button events using addEventBinding with Activating type
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#ClearButton",
                    new EventData().put("Action", "CLEAR"),
                    false);
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#CloseButton",
                    new EventData().put("Action", "CLOSE"),
                    false);
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull CombatLogEventData data) {
            if (data.action == null) return;
            
            switch (data.action.toUpperCase()) {
                case "CLEAR" -> {
                    combatLog.clear();
                    // Rebuild the UI to show empty log
                    this.rebuild();
                }
                case "CLOSE" -> {
                    combatLog.isVisible = false;
                    combatLog.activePage = null;
                    this.close();
                }
            }
        }
        
        /**
         * Event data class for page button events.
         */
        public static class CombatLogEventData {
            public static final BuilderCodec<CombatLogEventData> CODEC;

            public String action;

            static {
                CODEC = BuilderCodec.builder(CombatLogEventData.class, CombatLogEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (data, value) -> data.action = value,
                                (data) -> data.action)
                        .add()
                        .build();
            }
        }
    }
}
