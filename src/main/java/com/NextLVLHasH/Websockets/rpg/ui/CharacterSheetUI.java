package com.NextLVLHasH.Websockets.rpg.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * UI handler for the character sheet display.
 * Uses Hytale's native InteractiveCustomUIPage system with a custom .ui layout file.
 * Does NOT use Minecraft-style color codes (which Hytale does not support).
 */
public class CharacterSheetUI {

    private static final Logger LOGGER = Logger.getLogger(CharacterSheetUI.class.getName());

    /** Path to the character sheet UI layout file */
    private static final String UI_LAYOUT_PATH = "CharacterSheet.ui";

    /** Supplier to get online players map from the main plugin */
    private static Supplier<Map<String, PlayerRef>> onlinePlayersSupplier;

    /** Supplier to get Ref/Store for ECS operations */
    private static Supplier<RefStoreSupplier> refStoreSupplier;

    private final UUID playerId;
    private PlayerRPGData rpgData;
    private boolean isOpen;
    private CharacterSheetPage currentPage;

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
        LOGGER.fine("CharacterSheetUI initialized with online players supplier");
    }

    /**
     * Sets the supplier for getting Ref/Store for ECS operations.
     * Must be called during plugin initialization if using UI pages.
     *
     * @param supplier a supplier that returns RefStoreSupplier
     */
    public static void setRefStoreSupplier(Supplier<RefStoreSupplier> supplier) {
        refStoreSupplier = supplier;
        LOGGER.fine("CharacterSheetUI initialized with Ref/Store supplier");
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

    public CharacterSheetUI(UUID playerId, PlayerRPGData rpgData) {
        this.playerId = Objects.requireNonNull(playerId);
        this.rpgData = rpgData;
        this.isOpen = false;
    }

    /**
     * Shows the character sheet UI to the player.
     * Opens an interactive custom UI page.
     */
    public void show() {
        this.isOpen = true;
        LOGGER.fine("Opening character sheet for player: " + playerId);

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warning("Cannot show character sheet: player " + playerId + " is offline");
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
                            currentPage = new CharacterSheetPage(this, playerRef, rpgData);
                            player.getPageManager().openCustomPage(ref, store, currentPage);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to open character sheet UI page: " + e.getMessage());
            }
        }

        LOGGER.warning("Could not open character sheet UI - Ref/Store supplier not configured");
    }

    /**
     * Closes the character sheet UI.
     * Note: The page itself will close when the player presses ESC (CanDismiss)
     * or when the close button is clicked.
     */
    public void close() {
        this.isOpen = false;
        if (currentPage != null) {
            // Request the page to close itself
            currentPage.requestClose();
            currentPage = null;
        }
        LOGGER.fine("Closed character sheet for player: " + playerId);
    }

    /**
     * Refreshes the character sheet UI with updated data.
     */
    public void refresh() {
        if (!isOpen || rpgData == null) return;
        LOGGER.fine("Refreshed character sheet for: " + playerId);
        // If page is open and needs refresh, close and reopen
        if (currentPage != null) {
            close();
            show();
        }
    }

    public boolean isOpen() { return isOpen; }
    public UUID getPlayerId() { return playerId; }
    
    public void setRpgData(PlayerRPGData rpgData) {
        this.rpgData = rpgData;
        if (isOpen) refresh();
    }

    /**
     * Builds UI data map for external use or debugging.
     */
    public Map<String, Object> buildUIData() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (rpgData == null) return data;

        data.put("playerName", rpgData.getPlayerName());
        data.put("class", rpgData.getSelectedClass());
        data.put("race", rpgData.getSelectedRace());
        data.put("level", rpgData.getLevel());
        data.put("currentXP", rpgData.getCurrentXP());
        data.put("xpToNextLevel", rpgData.getXpToNextLevel());
        data.put("skillPoints", rpgData.getSkillPoints());
        data.put("currentHealth", rpgData.getCurrentHealth());
        data.put("maxHealth", rpgData.getMaxHealth());
        data.put("currentMana", rpgData.getCurrentMana());
        data.put("maxMana", rpgData.getMaxMana());
        data.put("currentStamina", rpgData.getCurrentStamina());
        data.put("maxStamina", rpgData.getMaxStamina());

        Map<String, Integer> attributes = new LinkedHashMap<>();
        for (AttributeType type : AttributeType.values()) {
            attributes.put(type.name(), rpgData.getCurrentAttribute(type));
        }
        data.put("attributes", attributes);

        return data;
    }

    /**
     * Static method to show character sheet for a player.
     * Creates a temporary CharacterSheetUI instance and shows it.
     *
     * @param playerId the player's UUID
     * @param rpgData the player's RPG data
     */
    public static void showCharacterSheet(UUID playerId, PlayerRPGData rpgData) {
        CharacterSheetUI ui = new CharacterSheetUI(playerId, rpgData);
        ui.show();
    }

    // ==================== INNER PAGE CLASS ====================

    /**
     * Custom UI Page for displaying the character sheet.
     * Uses InteractiveCustomUIPage with close button interaction.
     */
    public static class CharacterSheetPage extends InteractiveCustomUIPage<CharacterSheetPage.PageEventData> {

        private final CharacterSheetUI parentUI;
        private final PlayerRPGData rpgData;
        private boolean closeRequested = false;

        /**
         * Creates a new CharacterSheetPage.
         *
         * @param parentUI the parent CharacterSheetUI instance
         * @param playerRef the player to show the sheet to
         * @param rpgData the player's RPG data to display
         */
        public CharacterSheetPage(CharacterSheetUI parentUI, PlayerRef playerRef, PlayerRPGData rpgData) {
            super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
            this.parentUI = parentUI;
            this.rpgData = rpgData;
        }

        /**
         * Creates a new CharacterSheetPage without parent handler.
         * For use from commands that have direct ECS access.
         *
         * @param playerRef the player to show the sheet to
         * @param rpgData the player's RPG data to display
         */
        public CharacterSheetPage(PlayerRef playerRef, PlayerRPGData rpgData) {
            super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
            this.parentUI = null;
            this.rpgData = rpgData;
        }

        /**
         * Requests this page to close on the next event cycle.
         */
        public void requestClose() {
            this.closeRequested = true;
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref,
                          @Nonnull UICommandBuilder ui,
                          @Nonnull UIEventBuilder events,
                          @Nonnull Store<EntityStore> store) {
            // Load the .ui layout file
            ui.append(UI_LAYOUT_PATH);

            if (rpgData == null) {
                LOGGER.warning("RPG data is null, cannot populate character sheet");
                return;
            }

            // ===== HEADER SECTION =====
            ui.set("#PlayerNameLabel.Text", rpgData.getPlayerName() != null ? rpgData.getPlayerName() : "Unknown");
            
            String classRace = String.format("%s - %s",
                    rpgData.getSelectedClass() != null ? rpgData.getSelectedClass() : "None",
                    rpgData.getSelectedRace() != null ? rpgData.getSelectedRace() : "None");
            ui.set("#ClassRaceLabel.Text", classRace);
            
            ui.set("#LevelLabel.Text", "Level " + rpgData.getLevel());

            // ===== XP BAR =====
            long currentXP = rpgData.getCurrentXP();
            long xpToNext = rpgData.getXpToNextLevel();
            ui.set("#XPValueLabel.Text", currentXP + " / " + xpToNext);
            // Set segment visibility based on XP percentage
            int xpSegments = xpToNext > 0 ? (int) ((currentXP * 10) / xpToNext) : 0;
            for (int i = 0; i < 10; i++) {
                ui.set("#XPSeg" + i + ".Visible", i < xpSegments);
            }

            // ===== RESOURCE BARS =====
            // Health
            double currentHealth = rpgData.getCurrentHealth();
            double maxHealth = rpgData.getMaxHealth();
            ui.set("#HealthValueLabel.Text", String.format("%.0f / %.0f", currentHealth, maxHealth));
            int healthSegments = maxHealth > 0 ? (int) ((currentHealth * 10) / maxHealth) : 0;
            for (int i = 0; i < 10; i++) {
                ui.set("#HealthSeg" + i + ".Visible", i < healthSegments);
            }

            // Mana
            double currentMana = rpgData.getCurrentMana();
            double maxMana = rpgData.getMaxMana();
            ui.set("#ManaValueLabel.Text", String.format("%.0f / %.0f", currentMana, maxMana));
            int manaSegments = maxMana > 0 ? (int) ((currentMana * 10) / maxMana) : 0;
            for (int i = 0; i < 10; i++) {
                ui.set("#ManaSeg" + i + ".Visible", i < manaSegments);
            }

            // Stamina
            double currentStamina = rpgData.getCurrentStamina();
            double maxStamina = rpgData.getMaxStamina();
            ui.set("#StaminaValueLabel.Text", String.format("%.0f / %.0f", currentStamina, maxStamina));
            int staminaSegments = maxStamina > 0 ? (int) ((currentStamina * 10) / maxStamina) : 0;
            for (int i = 0; i < 10; i++) {
                ui.set("#StaminaSeg" + i + ".Visible", i < staminaSegments);
            }

            // ===== ATTRIBUTES =====
            ui.set("#StrengthValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.STRENGTH)));
            ui.set("#DexterityValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.DEXTERITY)));
            ui.set("#ConstitutionValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.CONSTITUTION)));
            ui.set("#IntelligenceValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.INTELLIGENCE)));
            ui.set("#WisdomValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.WISDOM)));
            ui.set("#CharismaValue.Text", String.valueOf(rpgData.getCurrentAttribute(AttributeType.CHARISMA)));

            // ===== SKILL POINTS =====
            int skillPoints = rpgData.getSkillPoints();
            String skillPointsText = skillPoints == 1 ? "1 Skill Point" : skillPoints + " Skill Points";
            ui.set("#SkillPointsValue.Text", skillPointsText);

            // ===== CLOSE BUTTON EVENT =====
            // Use Activating event type for button clicks
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#CloseButton",
                    new EventData().put("Action", "close"),
                    false);
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull PageEventData data) {
            // Handle close request from parent
            if (closeRequested) {
                LOGGER.fine("Close requested for character sheet");
                if (parentUI != null) {
                    parentUI.isOpen = false;
                    parentUI.currentPage = null;
                }
                this.close();
                return;
            }

            if ("close".equals(data.action)) {
                LOGGER.fine("Close button clicked on character sheet");
                if (parentUI != null) {
                    parentUI.isOpen = false;
                    parentUI.currentPage = null;
                }
                this.close();
            }
        }

        /**
         * Event data class for page events.
         */
        public static class PageEventData {
            public static final BuilderCodec<PageEventData> CODEC;

            public String action;

            static {
                CODEC = BuilderCodec.builder(PageEventData.class, PageEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (data, value) -> data.action = value,
                                (data) -> data.action)
                        .add()
                        .build();
            }
        }
    }
}
