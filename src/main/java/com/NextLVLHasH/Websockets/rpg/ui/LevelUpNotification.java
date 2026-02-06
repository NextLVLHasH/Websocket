package com.NextLVLHasH.Websockets.rpg.ui;

import java.util.Map;
import java.util.UUID;
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
 * Handles level-up notification display using Hytale's native UI system.
 * Uses a custom .ui layout file for styled notifications.
 * Does NOT use Minecraft-style color codes (which Hytale does not support).
 */
@SuppressWarnings("unused")
public class LevelUpNotification {

    private static final Logger LOGGER = Logger.getLogger(LevelUpNotification.class.getName());
    
    /** Path to the level-up notification UI layout file */
    private static final String UI_LAYOUT_PATH = "LevelUpNotification.ui";
    
    /** Supplier to get online players map from the main plugin */
    private static Supplier<Map<String, PlayerRef>> onlinePlayersSupplier;
    
    /** Supplier to get Player component from PlayerRef */
    private static Supplier<RefStoreSupplier> refStoreSupplier;

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
        LOGGER.info("LevelUpNotification initialized with online players supplier");
    }

    /**
     * Sets the supplier for getting Ref/Store for ECS operations.
     * Must be called during plugin initialization if using UI pages.
     *
     * @param supplier a supplier that returns RefStoreSupplier
     */
    public static void setRefStoreSupplier(Supplier<RefStoreSupplier> supplier) {
        refStoreSupplier = supplier;
        LOGGER.info("LevelUpNotification initialized with Ref/Store supplier");
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

    /**
     * Shows a simple level-up notification to a player using plain text.
     * For a styled notification, use showDetailed() which displays the UI page.
     * 
     * Note: Hytale does NOT support Minecraft-style color codes. Use plain text only.
     *
     * @param playerId the player's UUID
     * @param newLevel the new level achieved
     */
    public static void show(UUID playerId, int newLevel) {
        LOGGER.info("Player " + playerId + " leveled up to " + newLevel);
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warning("Cannot send level-up notification: player " + playerId + " is offline");
            return;
        }
        
        // Use plain text without Minecraft color codes - Hytale does not support them
        String message = "LEVEL UP! You are now Level " + newLevel + "!";
        playerRef.sendMessage(Message.raw(message));
    }

    /**
     * Shows a detailed level-up notification with stat changes using the custom UI page.
     * Falls back to plain text messages if UI system is not available.
     *
     * @param playerId the player's UUID
     * @param newLevel the new level
     * @param healthGain health gained
     * @param manaGain mana gained
     * @param staminaGain stamina gained
     * @param skillPointsGained skill points awarded
     */
    public static void showDetailed(UUID playerId, int newLevel, 
            double healthGain, double manaGain, double staminaGain, int skillPointsGained) {
        LOGGER.info(String.format("Player %s leveled up to %d! +%.0f HP, +%.0f MP, +%.0f SP, +%d skill points",
                playerId, newLevel, healthGain, manaGain, staminaGain, skillPointsGained));
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warning("Cannot send detailed level-up notification: player " + playerId + " is offline");
            return;
        }
        
        // Try to show the custom UI page
        if (refStoreSupplier != null) {
            try {
                RefStoreSupplier rss = refStoreSupplier.get();
                if (rss != null) {
                    Ref<EntityStore> ref = rss.getRef(playerRef);
                    Store<EntityStore> store = rss.getStore();
                    
                    if (ref != null && store != null) {
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            LevelUpPage page = new LevelUpPage(playerRef, newLevel, 
                                    healthGain, manaGain, staminaGain, skillPointsGained);
                            player.getPageManager().openCustomPage(ref, store, page);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to open level-up UI page, falling back to text: " + e.getMessage());
            }
        }
        
        // Fallback to plain text messages (no Minecraft color codes - Hytale doesn't support them)
        sendPlainTextNotification(playerRef, newLevel, healthGain, manaGain, staminaGain, skillPointsGained);
    }

    /**
     * Sends a plain text level-up notification without color codes.
     * Hytale does NOT support Minecraft-style color codes (e.g., section symbol codes).
     */
    private static void sendPlainTextNotification(PlayerRef playerRef, int newLevel,
            double healthGain, double manaGain, double staminaGain, int skillPointsGained) {
        
        // Build stat changes line using plain text
        StringBuilder statsLine = new StringBuilder();
        if (healthGain > 0) {
            statsLine.append("+").append(String.format("%.0f", healthGain)).append(" HP ");
        }
        if (manaGain > 0) {
            statsLine.append("+").append(String.format("%.0f", manaGain)).append(" MP ");
        }
        if (staminaGain > 0) {
            statsLine.append("+").append(String.format("%.0f", staminaGain)).append(" SP ");
        }
        if (skillPointsGained > 0) {
            statsLine.append("+").append(skillPointsGained).append(" Skill Points");
        }
        
        // Send plain text notifications - no Minecraft color codes
        playerRef.sendMessage(Message.raw("================================"));
        playerRef.sendMessage(Message.raw("LEVEL UP! Level " + newLevel));
        if (statsLine.length() > 0) {
            playerRef.sendMessage(Message.raw(statsLine.toString().trim()));
        }
        playerRef.sendMessage(Message.raw("================================"));
    }

    /**
     * Custom UI Page for displaying level-up notifications with styled layout.
     * Uses InteractiveCustomUIPage with a simple dismiss action.
     */
    public static class LevelUpPage extends InteractiveCustomUIPage<LevelUpPage.EventData> {

        private final int newLevel;
        private final double healthGain;
        private final double manaGain;
        private final double staminaGain;
        private final int skillPointsGained;

        /**
         * Creates a new LevelUpPage.
         *
         * @param playerRef the player to show the notification to
         * @param newLevel the new level achieved
         * @param healthGain health points gained
         * @param manaGain mana points gained
         * @param staminaGain stamina points gained
         * @param skillPointsGained skill points awarded
         */
        public LevelUpPage(PlayerRef playerRef, int newLevel,
                double healthGain, double manaGain, double staminaGain, int skillPointsGained) {
            super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
            this.newLevel = newLevel;
            this.healthGain = healthGain;
            this.manaGain = manaGain;
            this.staminaGain = staminaGain;
            this.skillPointsGained = skillPointsGained;
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref,
                          @Nonnull UICommandBuilder ui,
                          @Nonnull UIEventBuilder events,
                          @Nonnull Store<EntityStore> store) {
            // Load the .ui layout file
            ui.append(UI_LAYOUT_PATH);

            // Set the level text - plain text, styled by the .ui file
            ui.set("#LevelText.Text", "Level " + newLevel);

            // Set stat gain values - use plain text, styled by the .ui file
            ui.set("#HealthGain.Text", formatStatGain(healthGain));
            ui.set("#ManaGain.Text", formatStatGain(manaGain));
            ui.set("#StaminaGain.Text", formatStatGain(staminaGain));
            ui.set("#SkillPointsGain.Text", formatSkillPoints(skillPointsGained));

            // Hide stat rows with zero gain for cleaner display
            ui.set("#HealthLabel.Visible", healthGain > 0);
            ui.set("#HealthGain.Visible", healthGain > 0);
            ui.set("#ManaLabel.Visible", manaGain > 0);
            ui.set("#ManaGain.Visible", manaGain > 0);
            ui.set("#StaminaLabel.Visible", staminaGain > 0);
            ui.set("#StaminaGain.Visible", staminaGain > 0);
            ui.set("#SkillPointsLabel.Visible", skillPointsGained > 0);
            ui.set("#SkillPointsGain.Visible", skillPointsGained > 0);
            
            // No interactive events needed - player can press ESC to dismiss
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull EventData data) {
            // No events to handle - page is dismissible via ESC
            // Close the page if any event is received (shouldn't happen)
            this.close();
        }

        /**
         * Formats a stat gain value for display.
         */
        private String formatStatGain(double value) {
            if (value <= 0) {
                return "+0";
            }
            return "+" + String.format("%.0f", value);
        }

        /**
         * Formats skill points for display.
         */
        private String formatSkillPoints(int points) {
            if (points <= 0) {
                return "+0";
            }
            return "+" + points;
        }
        
        /**
         * Event data class for page events (minimal - page is non-interactive).
         */
        public static class EventData {
            public static final BuilderCodec<EventData> CODEC;

            public String action;

            static {
                CODEC = BuilderCodec.builder(EventData.class, EventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (data, value) -> data.action = value,
                                (data) -> data.action)
                        .add()
                        .build();
            }
        }
    }
}
