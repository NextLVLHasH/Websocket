package com.NextLVLHasH.Websockets.rpg.ui;

import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.character.CharacterManager;
import com.NextLVLHasH.Websockets.rpg.character.Profession;
import com.NextLVLHasH.Websockets.rpg.character.Race;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.RPGManager;

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

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

/**
 * UI handler for character creation and selection.
 * Uses Hytale's native .ui file system with InteractiveCustomUIPage.
 */
@SuppressWarnings("unused")
public class CharacterSelectionUI {

    private static final Logger LOGGER = Logger.getLogger(CharacterSelectionUI.class.getName());
    
    /** Path to the character selection UI layout file */
    private static final String UI_LAYOUT_PATH = "CharacterSelection.ui";
    
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
        LOGGER.info("CharacterSelectionUI initialized with online players supplier");
    }

    /**
     * Sets the supplier for getting Ref/Store for ECS operations.
     * Must be called during plugin initialization if using UI pages.
     *
     * @param supplier a supplier that returns RefStoreSupplier
     */
    public static void setRefStoreSupplier(Supplier<RefStoreSupplier> supplier) {
        refStoreSupplier = supplier;
        LOGGER.info("CharacterSelectionUI initialized with Ref/Store supplier");
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

    private final UUID playerId;
    private String selectedClass;
    private String selectedRace;
    private String selectedProfession;
    private boolean isOpen;

    public CharacterSelectionUI(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId);
        this.isOpen = false;
    }

    /**
     * Shows the character selection UI to the player using the native UI page.
     * Falls back to text messages if UI system is not available.
     */
    public void show() {
        this.isOpen = true;
        LOGGER.info("Showing character selection UI to player: " + playerId);
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warning("Cannot show character selection UI: player " + playerId + " is offline");
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
                            CharacterSelectionPage page = new CharacterSelectionPage(playerRef, this);
                            player.getPageManager().openCustomPage(ref, store, page);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to open character selection UI page: " + e.getMessage());
            }
        }
        
        // Fallback to plain text message
        playerRef.sendMessage(Message.raw("Character Selection is available. Please use commands to select class/race/profession."));
    }

    public void close() {
        this.isOpen = false;
    }

    public boolean isOpen() { return isOpen; }
    public UUID getPlayerId() { return playerId; }
    public String getSelectedClass() { return selectedClass; }
    public String getSelectedRace() { return selectedRace; }
    public String getSelectedProfession() { return selectedProfession; }

    public boolean selectClass(String classId) {
        CharacterClass charClass = CharacterManager.getInstance().getClass(classId);
        if (charClass != null) {
            this.selectedClass = classId;
            return true;
        }
        return false;
    }

    public boolean selectRace(String raceId) {
        Race race = CharacterManager.getInstance().getRace(raceId);
        if (race != null) {
            this.selectedRace = raceId;
            return true;
        }
        return false;
    }

    public boolean selectProfession(String professionId) {
        // Allow null/empty to clear profession
        if (professionId == null || professionId.isEmpty() || professionId.equalsIgnoreCase("none")) {
            this.selectedProfession = null;
            return true;
        }
        Profession profession = CharacterManager.getInstance().getProfession(professionId);
        if (profession != null && (selectedClass == null || profession.isClassAllowed(selectedClass))) {
            this.selectedProfession = professionId;
            return true;
        }
        return false;
    }

    public boolean validateSelections() {
        if (selectedClass == null || selectedRace == null) return false;
        return CharacterManager.getInstance().isValidCombination(selectedClass, selectedRace, selectedProfession);
    }

    public boolean confirm() {
        if (!validateSelections()) return false;
        var result = RPGManager.getInstance().createCharacter(playerId, selectedClass, selectedRace, selectedProfession);
        boolean success = result != null;
        if (success) close();
        return success;
    }

    public void reset() {
        selectedClass = null;
        selectedRace = null;
        selectedProfession = null;
    }

    /**
     * Gets the description text for a class.
     */
    private static String getClassDescription(String classId) {
        if (classId == null) return "Click a class to select";
        CharacterClass charClass = CharacterManager.getInstance().getClass(classId);
        if (charClass != null) {
            return charClass.getDescription();
        }
        return switch (classId.toLowerCase()) {
            case "warrior" -> "Melee combat specialist with high defense";
            case "mage" -> "Master of arcane magic with powerful spells";
            case "rogue" -> "Stealthy fighter with high critical damage";
            default -> "Unknown class";
        };
    }

    /**
     * Gets the description text for a race.
     */
    private static String getRaceDescription(String raceId) {
        if (raceId == null) return "Click a race to select";
        Race race = CharacterManager.getInstance().getRace(raceId);
        if (race != null) {
            return race.getDescription();
        }
        return switch (raceId.toLowerCase()) {
            case "human" -> "Balanced stats with versatile abilities";
            case "elf" -> "High mana and magical affinity";
            case "dwarf" -> "High health and crafting bonuses";
            case "orc" -> "High strength and combat prowess";
            default -> "Unknown race";
        };
    }

    /**
     * Gets the description text for a profession.
     */
    private static String getProfessionDescription(String professionId) {
        if (professionId == null) return "Professions provide crafting abilities";
        Profession profession = CharacterManager.getInstance().getProfession(professionId);
        if (profession != null) {
            return profession.getDescription();
        }
        return switch (professionId.toLowerCase()) {
            case "none" -> "No profession selected";
            case "blacksmith" -> "Craft weapons and armor";
            case "alchemist" -> "Create potions and elixirs";
            case "enchanter" -> "Imbue items with magical effects";
            default -> "Unknown profession";
        };
    }

    /**
     * Builds the summary text for current selections.
     */
    private String buildSummaryText() {
        StringBuilder sb = new StringBuilder();
        if (selectedClass != null) {
            sb.append(capitalize(selectedClass));
        }
        if (selectedRace != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(capitalize(selectedRace));
        }
        if (selectedProfession != null) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(capitalize(selectedProfession));
        }
        return sb.length() > 0 ? sb.toString() : "None";
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // =======================================================================
    // Inner Class: InteractiveCustomUIPage Implementation
    // =======================================================================

    /**
     * Custom UI Page for character creation/selection.
     * Uses InteractiveCustomUIPage with button event handling.
     */
    public static class CharacterSelectionPage extends InteractiveCustomUIPage<CharacterSelectionPage.SelectionEventData> {

        private final CharacterSelectionUI uiHandler;
        // Standalone mode state for when no uiHandler is provided
        private String selectedClass = null;
        private String selectedRace = null;
        private String selectedProfession = null;

        /**
         * Creates a new CharacterSelectionPage.
         *
         * @param playerRef the player to show the UI to
         * @param uiHandler the CharacterSelectionUI instance managing state
         */
        public CharacterSelectionPage(PlayerRef playerRef, CharacterSelectionUI uiHandler) {
            super(playerRef, CustomPageLifetime.CanDismiss, SelectionEventData.CODEC);
            this.uiHandler = uiHandler;
        }

        /**
         * Creates a new CharacterSelectionPage without parent handler.
         * For use from commands that have direct ECS access.
         *
         * @param playerRef the player to show the UI to
         */
        public CharacterSelectionPage(PlayerRef playerRef) {
            super(playerRef, CustomPageLifetime.CanDismiss, SelectionEventData.CODEC);
            this.uiHandler = null;
        }

        // Helper methods to get selected values with null-safety
        private String getSelectedClass() {
            return uiHandler != null ? uiHandler.selectedClass : selectedClass;
        }

        private String getSelectedRace() {
            return uiHandler != null ? uiHandler.selectedRace : selectedRace;
        }

        private String getSelectedProfession() {
            return uiHandler != null ? uiHandler.selectedProfession : selectedProfession;
        }

        private String buildLocalSummaryText() {
            StringBuilder sb = new StringBuilder();
            String cls = getSelectedClass();
            String race = getSelectedRace();
            String prof = getSelectedProfession();
            if (cls != null) {
                sb.append(capitalize(cls));
            }
            if (race != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(capitalize(race));
            }
            if (prof != null) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(capitalize(prof));
            }
            return sb.length() > 0 ? sb.toString() : "None";
        }

        @Override
        public void build(@Nonnull Ref<EntityStore> ref,
                          @Nonnull UICommandBuilder ui,
                          @Nonnull UIEventBuilder events,
                          @Nonnull Store<EntityStore> store) {
            // Load the .ui layout file
            ui.append(UI_LAYOUT_PATH);

            // Update button states and descriptions based on current selection
            updateButtonStates(ui);
            updateDescriptions(ui);
            updateSummary(ui);

            // Bind button click events using addEventBinding with Activating type
            // Class buttons
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ClassWarrior",
                    new EventData().put("Action", "select_class").put("Value", "warrior"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ClassMage",
                    new EventData().put("Action", "select_class").put("Value", "mage"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ClassRogue",
                    new EventData().put("Action", "select_class").put("Value", "rogue"), false);

            // Race buttons
            events.addEventBinding(CustomUIEventBindingType.Activating, "#RaceHuman",
                    new EventData().put("Action", "select_race").put("Value", "human"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#RaceElf",
                    new EventData().put("Action", "select_race").put("Value", "elf"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#RaceDwarf",
                    new EventData().put("Action", "select_race").put("Value", "dwarf"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#RaceOrc",
                    new EventData().put("Action", "select_race").put("Value", "orc"), false);

            // Profession buttons
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ProfessionNone",
                    new EventData().put("Action", "select_profession").put("Value", "none"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ProfessionBlacksmith",
                    new EventData().put("Action", "select_profession").put("Value", "blacksmith"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ProfessionAlchemist",
                    new EventData().put("Action", "select_profession").put("Value", "alchemist"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ProfessionEnchanter",
                    new EventData().put("Action", "select_profession").put("Value", "enchanter"), false);

            // Action buttons
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetButton",
                    new EventData().put("Action", "reset"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton",
                    new EventData().put("Action", "confirm"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                    new EventData().put("Action", "cancel"), false);
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull SelectionEventData data) {
            String action = data.action;
            String value = data.value;

            if (action == null) {
                sendUpdate();
                return;
            }

            switch (action) {
                case "select_class" -> {
                    if (selectClassLocal(value)) {
                        // Refresh the UI to show updated state
                        refresh();
                    }
                }
                case "select_race" -> {
                    if (selectRaceLocal(value)) {
                        refresh();
                    }
                }
                case "select_profession" -> {
                    if (selectProfessionLocal(value)) {
                        refresh();
                    }
                }
                case "reset" -> {
                    resetLocal();
                    refresh();
                }
                case "confirm" -> {
                    if (validateSelectionsLocal()) {
                        if (confirmLocal()) {
                            playerRef.sendMessage(Message.raw("Character created successfully!"));
                            this.close();
                        } else {
                            // Show error message
                            refreshWithError("Failed to create character");
                        }
                    } else {
                        refreshWithError("Please select a class and race");
                    }
                }
                case "cancel" -> {
                    closeLocal();
                    this.close();
                }
            }
        }

        // Local action methods that handle both uiHandler and standalone modes
        private boolean selectClassLocal(String value) {
            if (uiHandler != null) {
                return uiHandler.selectClass(value);
            }
            CharacterClass charClass = CharacterManager.getInstance().getClass(value);
            if (charClass != null) {
                this.selectedClass = value;
                return true;
            }
            return false;
        }

        private boolean selectRaceLocal(String value) {
            if (uiHandler != null) {
                return uiHandler.selectRace(value);
            }
            Race race = CharacterManager.getInstance().getRace(value);
            if (race != null) {
                this.selectedRace = value;
                return true;
            }
            return false;
        }

        private boolean selectProfessionLocal(String value) {
            if (uiHandler != null) {
                return uiHandler.selectProfession(value);
            }
            if (value == null || value.isEmpty() || value.equalsIgnoreCase("none")) {
                this.selectedProfession = null;
                return true;
            }
            Profession prof = CharacterManager.getInstance().getProfession(value);
            if (prof != null) {
                this.selectedProfession = value;
                return true;
            }
            return false;
        }

        private void resetLocal() {
            if (uiHandler != null) {
                uiHandler.reset();
            } else {
                this.selectedClass = null;
                this.selectedRace = null;
                this.selectedProfession = null;
            }
        }

        private boolean validateSelectionsLocal() {
            if (uiHandler != null) {
                return uiHandler.validateSelections();
            }
            if (selectedClass == null || selectedRace == null) return false;
            return CharacterManager.getInstance().isValidCombination(selectedClass, selectedRace, selectedProfession);
        }

        private boolean confirmLocal() {
            if (uiHandler != null) {
                return uiHandler.confirm();
            }
            if (!validateSelectionsLocal()) return false;
            var result = RPGManager.getInstance().createCharacter(playerRef.getUuid(), selectedClass, selectedRace, selectedProfession);
            return result != null;
        }

        private void closeLocal() {
            if (uiHandler != null) {
                uiHandler.close();
            }
        }

        /**
         * Refreshes the UI with current state.
         */
        private void refresh() {
            UICommandBuilder ui = new UICommandBuilder();
            updateButtonStates(ui);
            updateDescriptions(ui);
            updateSummary(ui);
            ui.set("#ValidationMessage.Text", "");
            sendUpdate(ui, new UIEventBuilder(), false);
        }

        /**
         * Refreshes the UI with an error message.
         */
        private void refreshWithError(String error) {
            UICommandBuilder ui = new UICommandBuilder();
            updateButtonStates(ui);
            updateDescriptions(ui);
            updateSummary(ui);
            ui.set("#ValidationMessage.Text", error);
            sendUpdate(ui, new UIEventBuilder(), false);
        }

        /**
         * Updates button backgrounds to show selected state.
         */
        private void updateButtonStates(UICommandBuilder ui) {
            String selectedBg = "#4a6a4a";  // Green-ish for selected
            String defaultBg = "#2a2a2a";   // Dark gray for unselected
            String cls = getSelectedClass();
            String race = getSelectedRace();
            String prof = getSelectedProfession();

            // Class buttons
            ui.set("#ClassWarrior.Background", 
                    "warrior".equalsIgnoreCase(cls) ? selectedBg : defaultBg);
            ui.set("#ClassMage.Background", 
                    "mage".equalsIgnoreCase(cls) ? selectedBg : defaultBg);
            ui.set("#ClassRogue.Background", 
                    "rogue".equalsIgnoreCase(cls) ? selectedBg : defaultBg);

            // Race buttons
            ui.set("#RaceHuman.Background", 
                    "human".equalsIgnoreCase(race) ? selectedBg : defaultBg);
            ui.set("#RaceElf.Background", 
                    "elf".equalsIgnoreCase(race) ? selectedBg : defaultBg);
            ui.set("#RaceDwarf.Background", 
                    "dwarf".equalsIgnoreCase(race) ? selectedBg : defaultBg);
            ui.set("#RaceOrc.Background", 
                    "orc".equalsIgnoreCase(race) ? selectedBg : defaultBg);

            // Profession buttons
            ui.set("#ProfessionNone.Background", 
                    prof == null ? selectedBg : defaultBg);
            ui.set("#ProfessionBlacksmith.Background", 
                    "blacksmith".equalsIgnoreCase(prof) ? selectedBg : defaultBg);
            ui.set("#ProfessionAlchemist.Background", 
                    "alchemist".equalsIgnoreCase(prof) ? selectedBg : defaultBg);
            ui.set("#ProfessionEnchanter.Background", 
                    "enchanter".equalsIgnoreCase(prof) ? selectedBg : defaultBg);

            // Confirm button - enable/disable appearance based on valid selections
            boolean valid = cls != null && race != null;
            ui.set("#ConfirmButton.Background", valid ? "#3a7a3a" : "#2a5a2a");
        }

        /**
         * Updates description labels based on current selections.
         */
        private void updateDescriptions(UICommandBuilder ui) {
            ui.set("#ClassDescription.Text", getClassDescription(getSelectedClass()));
            ui.set("#RaceDescription.Text", getRaceDescription(getSelectedRace()));
            ui.set("#ProfessionDescription.Text", getProfessionDescription(getSelectedProfession()));
        }

        /**
         * Updates the summary label with current selections.
         */
        private void updateSummary(UICommandBuilder ui) {
            ui.set("#SummaryValue.Text", buildLocalSummaryText());
        }

        /**
         * Event data class for selection events.
         */
        public static class SelectionEventData {
            public static final BuilderCodec<SelectionEventData> CODEC;

            public String action;
            public String value;

            public SelectionEventData() {}

            public SelectionEventData(String action, String value) {
                this.action = action;
                this.value = value;
            }

            static {
                CODEC = BuilderCodec.builder(SelectionEventData.class, SelectionEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (data, v) -> data.action = v,
                                (data) -> data.action)
                        .add()
                        .append(new KeyedCodec<>("Value", Codec.STRING),
                                (data, v) -> data.value = v,
                                (data) -> data.value)
                        .add()
                        .build();
            }
        }
    }
}
