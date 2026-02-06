package com.NextLVLHasH.Websockets.rpg.ui;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.skills.*;

import java.util.*;
import java.util.function.Supplier;


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
 * UI handler for the skill tree display using Hytale's native UI system.
 * Uses a custom .ui layout file for styled skill tree visualization.
 * Does NOT use Minecraft-style color codes (which Hytale does not support).
 */
public class SkillTreeUI {

    /** Path to the skill tree UI layout file */
    private static final String UI_LAYOUT_PATH = "SkillTree.ui";

    /** Maximum skill nodes per tier */
    private static final int MAX_NODES_PER_TIER = 5;

    /** Maximum number of tiers */
    private static final int MAX_TIERS = 5;

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
        
    }

    /**
     * Sets the supplier for getting Ref/Store for ECS operations.
     * Must be called during plugin initialization if using UI pages.
     *
     * @param supplier a supplier that returns RefStoreSupplier
     */
    public static void setRefStoreSupplier(Supplier<RefStoreSupplier> supplier) {
        refStoreSupplier = supplier;
        
    }

    /**
     * Gets the PlayerRef for a player by UUID.
     *
     * @param playerId the player's UUID
     * @return the PlayerRef if online, null otherwise
     */
    private static PlayerRef getPlayerRef(UUID playerId) {
        if (onlinePlayersSupplier == null) {
            return null;
        }
        Map<String, PlayerRef> onlinePlayers = onlinePlayersSupplier.get();
        return onlinePlayers != null ? onlinePlayers.get(playerId.toString()) : null;
    }

    // ==================== Instance Fields ====================

    private final UUID playerId;
    private PlayerRPGData rpgData;
    private SkillTree currentTree;
    private String selectedSkillId;
    private boolean isOpen;
    private SkillTreePage currentPage;

    public SkillTreeUI(UUID playerId, PlayerRPGData rpgData) {
        this.playerId = Objects.requireNonNull(playerId);
        this.rpgData = rpgData;
        this.isOpen = false;
    }

    /**
     * Attach a SkillTree to this UI instance so pages can route events back here.
     * Use when creating a UI parent manually instead of calling show().
     */
    public void attachTree(SkillTree tree) {
        this.currentTree = tree;
        this.selectedSkillId = null;
        this.isOpen = true;
    }

    /**
     * Shows the skill tree UI for the specified tree.
     *
     * @param treeId the skill tree ID to display
     */
    public void show(String treeId) {
        this.currentTree = SkillManager.getInstance().getSkillTree(treeId);
        if (this.currentTree == null) {
            return;
        }

        

        this.isOpen = true;
        this.selectedSkillId = null;

        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
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
                            currentPage = new SkillTreePage(this, playerRef, currentTree, rpgData);
                            player.getPageManager().openCustomPage(ref, store, currentPage);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                
            }
        }

        // Fallback to plain text
        sendPlainTextSkillTree(playerRef);
    }

    /**
     * Sends a plain text representation of the skill tree.
     */
    private void sendPlainTextSkillTree(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("================================"));
        playerRef.sendMessage(Message.raw("Skill Tree: " + currentTree.getDisplayName()));
        playerRef.sendMessage(Message.raw("Available Points: " + (rpgData != null ? rpgData.getSkillPoints() : 0)));
        playerRef.sendMessage(Message.raw("================================"));

        Set<String> unlockedSkills = SkillManager.getInstance().getUnlockedSkills(playerId);

        for (SkillNode node : currentTree.getAllNodes()) {
            Skill skill = node.getSkill();
            String status;
            if (unlockedSkills.contains(skill.getId())) {
                status = "[UNLOCKED]";
            } else if (node.canUnlock(unlockedSkills, 
                    rpgData != null ? rpgData.getLevel() : 0, 
                    rpgData != null ? rpgData.getSkillPoints() : 0)) {
                status = "[AVAILABLE]";
            } else {
                status = "[LOCKED]";
            }
            playerRef.sendMessage(Message.raw(status + " " + skill.getDisplayName() + " (Tier " + node.getTier() + ")"));
        }

        playerRef.sendMessage(Message.raw("================================"));
    }

    /**
     * Closes the skill tree UI.
     * Note: The page will close when the player presses ESC (CanDismiss)
     * or when the close button is clicked.
     */
    public void close() {
        this.isOpen = false;
        this.currentTree = null;
        this.selectedSkillId = null;
        if (currentPage != null) {
            // Request the page to close itself
            currentPage.requestClose();
            currentPage = null;
        }
        
    }

    /**
     * Refreshes the skill tree UI with updated data.
     */
    public void refresh() {
        if (!isOpen || currentTree == null) return;
        
        // If page is open and needs refresh, close and reopen
        if (currentPage != null) {
            String treeId = currentTree.getId();
            close();
            show(treeId);
        }
    }

    public boolean isOpen() { return isOpen; }
    public UUID getPlayerId() { return playerId; }
    public String getSelectedSkillId() { return selectedSkillId; }

    public void selectNode(String skillId) {
        if (currentTree != null && currentTree.getNode(skillId) != null) {
            this.selectedSkillId = skillId;
            
        }
    }

    public boolean unlockSelectedSkill() {
        if (selectedSkillId == null || rpgData == null) return false;

        SkillNode node = currentTree.getNode(selectedSkillId);
        if (node == null) return false;

        Set<String> unlockedSkills = SkillManager.getInstance().getUnlockedSkills(playerId);

        // Check if can unlock
        if (!node.canUnlock(unlockedSkills, rpgData.getLevel(), rpgData.getSkillPoints())) {
            return false;
        }

        // Unlock the skill
        boolean success = SkillManager.getInstance().unlockSkill(playerId, selectedSkillId, rpgData);
        if (success) {
            refresh();
        }
        return success;
    }

    public void setRpgData(PlayerRPGData rpgData) {
        this.rpgData = rpgData;
        if (isOpen) refresh();
    }

    public Map<String, Object> getNodeInfo(String skillId) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (currentTree == null) return info;

        SkillNode node = currentTree.getNode(skillId);
        if (node == null) return info;

        Skill skill = node.getSkill();
        info.put("id", skill.getId());
        info.put("name", skill.getDisplayName());
        info.put("description", skill.getDescription());
        info.put("type", skill.getType().name());
        info.put("requiredLevel", skill.getRequiredLevel());
        info.put("skillPointCost", skill.getSkillPointCost());
        info.put("cooldown", skill.getCooldownMillis());
        info.put("manaCost", skill.getManaCost());
        info.put("staminaCost", skill.getStaminaCost());
        info.put("prerequisites", skill.getPrerequisiteSkills());
        info.put("tier", node.getTier());
        info.put("position", node.getPosition());

        Set<String> unlocked = SkillManager.getInstance().getUnlockedSkills(playerId);
        info.put("isUnlocked", unlocked.contains(skillId));
        info.put("canUnlock", node.canUnlock(unlocked,
                rpgData != null ? rpgData.getLevel() : 0,
                rpgData != null ? rpgData.getSkillPoints() : 0));

        return info;
    }

    // ==================== Inner Class: SkillTreePage ====================

    /**
     * Custom UI Page for displaying the skill tree with interactive elements.
     * Uses InteractiveCustomUIPage with skill selection and unlock actions.
     */
    public static class SkillTreePage extends InteractiveCustomUIPage<SkillTreePage.SkillTreeEventData> {

        private final SkillTreeUI parentUI;
        private final SkillTree skillTree;
        private final PlayerRPGData rpgData;
        private final UUID playerId;
        private boolean closeRequested = false;

        /**
         * Creates a new SkillTreePage.
         *
         * @param parentUI the parent SkillTreeUI instance
         * @param playerRef the player to show the skill tree to
         * @param skillTree the skill tree to display
         * @param rpgData the player's RPG data
         */
        public SkillTreePage(SkillTreeUI parentUI, PlayerRef playerRef, 
                SkillTree skillTree, PlayerRPGData rpgData) {
            super(playerRef, CustomPageLifetime.CanDismiss, SkillTreeEventData.CODEC);
            this.parentUI = parentUI;
            this.skillTree = skillTree;
            this.rpgData = rpgData;
            this.playerId = parentUI.getPlayerId();
        }

        /**
         * Creates a new SkillTreePage without parent handler.
         * For use from commands that have direct ECS access.
         *
         * @param playerRef the player to show the skill tree to
         * @param rpgData the player's RPG data
         * @param treeId the skill tree ID to display
         */
        public SkillTreePage(PlayerRef playerRef, PlayerRPGData rpgData, String treeId) {
            super(playerRef, CustomPageLifetime.CanDismiss, SkillTreeEventData.CODEC);
            this.parentUI = null;
            this.rpgData = rpgData;
            this.playerId = playerRef.getUuid();
            // Get skill tree from the SkillManager, create empty fallback if not found
            SkillTree tree = SkillManager.getInstance().getSkillTree(treeId);
            if (tree == null) {
            // Create empty placeholder skill tree using builder
            tree = SkillTree.builder()
                .id(treeId != null ? treeId : "default")
                .displayName("Skill Tree")
                .description("No skill tree loaded")
                .build();
            }
            this.skillTree = tree;
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

            // Set tree title
            ui.set("#TreeTitle.Text", skillTree.getDisplayName());

            // Set available skill points
            int skillPoints = rpgData != null ? rpgData.getSkillPoints() : 0;
            ui.set("#SkillPointsLabel.Text", "Available Skill Points: " + skillPoints);

            // Register close button event using addEventBinding
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#CloseButton",
                    new EventData().put("Action", "close"),
                    false);

            // Register unlock button event
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#UnlockButton",
                    new EventData().put("Action", "unlock"),
                    false);

            // Get unlocked skills for state checking
            Set<String> unlockedSkills = SkillManager.getInstance().getUnlockedSkills(playerId);
            int playerLevel = rpgData != null ? rpgData.getLevel() : 0;

            // Build skill node grid organized by tier
            Map<Integer, List<SkillNode>> nodesByTier = organizeNodesByTier();

            // Debug: log tier layout and node ids to help trace empty UI issues
            StringBuilder nodesLog = new StringBuilder();
            nodesLog.append("NodesByTier: ");
            for (Map.Entry<Integer, List<SkillNode>> e : nodesByTier.entrySet()) {
                nodesLog.append("[Tier ").append(e.getKey()).append(": ").append(e.getValue().size()).append(" { ");
                for (SkillNode n : e.getValue()) {
                    nodesLog.append(n.getSkillId()).append(',');
                }
                nodesLog.append(" }]");
            }
            if (nodesByTier.isEmpty()) {
                
            }

            for (int tier = 0; tier < MAX_TIERS; tier++) {
                List<SkillNode> tierNodes = nodesByTier.getOrDefault(tier, Collections.emptyList());

                for (int pos = 0; pos < MAX_NODES_PER_TIER; pos++) {
                    String nodeId = "#SkillNodeR" + tier + "C" + pos;
                    String nameId = "#SkillNameR" + tier + "C" + pos;
                    String iconId = "#SkillIconR" + tier + "C" + pos;

                    if (pos < tierNodes.size()) {
                        SkillNode node = tierNodes.get(pos);
                        Skill skill = node.getSkill();
                        String skillId = skill.getId();

                        // Set skill name (truncate if too long)
                        String displayName = skill.getDisplayName();
                        if (displayName.length() > 8) {
                            displayName = displayName.substring(0, 7) + ".";
                        }
                        ui.set(nameId + ".Text", displayName);

                        // Determine node state and set appropriate colors
                        boolean isUnlocked = unlockedSkills.contains(skillId);
                        boolean canUnlock = node.canUnlock(unlockedSkills, playerLevel, skillPoints);
                        boolean isSelected = parentUI != null && skillId.equals(parentUI.getSelectedSkillId());

                        // Set background color based on state
                        if (isSelected) {
                            ui.set(nodeId + ".Background", "#4444AA");
                            ui.set(iconId + ".Background", "#6666CC");
                        } else if (isUnlocked) {
                            ui.set(nodeId + ".Background", "#225522");
                            ui.set(iconId + ".Background", "#338833");
                        } else if (canUnlock) {
                            ui.set(nodeId + ".Background", "#555522");
                            ui.set(iconId + ".Background", "#777733");
                        } else {
                            ui.set(nodeId + ".Background", "#333344");
                            ui.set(iconId + ".Background", "#555566");
                        }

                        // Set visibility
                        ui.set(nodeId + ".Visible", true);

                        // Register click event for skill selection
                        events.addEventBinding(CustomUIEventBindingType.Activating,
                                nodeId,
                                new EventData()
                                        .put("Action", "select")
                                        .put("SkillId", skillId),
                                false);
                    } else {
                        // Hide unused slots
                        ui.set(nodeId + ".Visible", false);
                    }
                }

                // Hide the entire tier row if empty
                ui.set("#Tier" + tier + ".Visible", !tierNodes.isEmpty());
            }

            // Update details panel
            updateDetailsPanel(ui, unlockedSkills, playerLevel, skillPoints);
        }

        /**
         * Updates the details panel with the selected skill info.
         */
        private void updateDetailsPanel(UICommandBuilder ui, Set<String> unlockedSkills, 
                int playerLevel, int skillPoints) {
            String selectedSkillId = parentUI != null ? parentUI.getSelectedSkillId() : null;

            if (selectedSkillId == null) {
                // No skill selected
                ui.set("#NoSelectionMessage.Visible", true);
                ui.set("#SkillDetailsContainer.Visible", false);
                return;
            }

            SkillNode node = skillTree.getNode(selectedSkillId);
            if (node == null) {
                ui.set("#NoSelectionMessage.Visible", true);
                ui.set("#SkillDetailsContainer.Visible", false);
                return;
            }

            Skill skill = node.getSkill();
            boolean isUnlocked = unlockedSkills.contains(selectedSkillId);
            boolean canUnlock = node.canUnlock(unlockedSkills, playerLevel, skillPoints);

            // Show details container
            ui.set("#NoSelectionMessage.Visible", false);
            ui.set("#SkillDetailsContainer.Visible", true);

            // Set skill name and description
            ui.set("#SelectedSkillName.Text", skill.getDisplayName());
            ui.set("#SkillDescription.Text", skill.getDescription() != null ? 
                    skill.getDescription() : "No description available.");

            // Set skill type
            SkillType skillType = skill.getType();
            String typeText = skillType != null ? skillType.name() : "UNKNOWN";
            ui.set("#SkillType.Text", typeText);

            // Set requirements
            int requiredLevel = skill.getRequiredLevel();
            boolean levelMet = playerLevel >= requiredLevel;
            ui.set("#LevelRequirement.Text", "Level " + requiredLevel + (levelMet ? "" : " (Need " + (requiredLevel - playerLevel) + " more)"));

            int pointCost = skill.getSkillPointCost();
            boolean pointsMet = skillPoints >= pointCost;
            ui.set("#SkillPointCost.Text", pointCost + " Skill Point" + (pointCost != 1 ? "s" : "") + 
                    (pointsMet ? "" : " (Need " + (pointCost - skillPoints) + " more)"));

            // Set prerequisites
            List<String> prereqs = skill.getPrerequisiteSkills();
            if (prereqs == null || prereqs.isEmpty()) {
                ui.set("#Prerequisites.Text", "None");
            } else {
                StringBuilder prereqText = new StringBuilder();
                for (String prereq : prereqs) {
                    if (prereqText.length() > 0) prereqText.append(", ");
                    boolean hasPre = unlockedSkills.contains(prereq);
                    prereqText.append(prereq).append(hasPre ? "" : " (missing)");
                }
                ui.set("#Prerequisites.Text", prereqText.toString());
            }

            // Set costs
            double manaCost = skill.getManaCost();
            double staminaCost = skill.getStaminaCost();
            ui.set("#ManaCostValue.Text", String.format("%.0f", manaCost));
            ui.set("#StaminaCostValue.Text", String.format("%.0f", staminaCost));

            // Set cooldown
            long cooldownMs = skill.getCooldownMillis();
            String cooldownText = cooldownMs > 0 ? String.format("%.1fs", cooldownMs / 1000.0) : "None";
            ui.set("#CooldownValue.Text", cooldownText);

            // Set button visibility based on state
            if (isUnlocked) {
                ui.set("#UnlockButton.Visible", false);
                ui.set("#UnlockButtonDisabled.Visible", false);
                ui.set("#AlreadyUnlocked.Visible", true);
            } else if (canUnlock) {
                ui.set("#UnlockButton.Visible", true);
                ui.set("#UnlockButtonDisabled.Visible", false);
                ui.set("#AlreadyUnlocked.Visible", false);
            } else {
                ui.set("#UnlockButton.Visible", false);
                ui.set("#UnlockButtonDisabled.Visible", true);
                ui.set("#AlreadyUnlocked.Visible", false);
            }
        }

        /**
         * Organizes skill nodes by tier for grid layout.
         */
        private Map<Integer, List<SkillNode>> organizeNodesByTier() {
            Map<Integer, List<SkillNode>> nodesByTier = new TreeMap<>();

            for (SkillNode node : skillTree.getAllNodes()) {
                int tier = node.getTier();
                nodesByTier.computeIfAbsent(tier, k -> new ArrayList<>()).add(node);
            }

            // Sort nodes within each tier by position
            for (List<SkillNode> tierNodes : nodesByTier.values()) {
                tierNodes.sort(Comparator.comparingInt(SkillNode::getPosition));
            }

            return nodesByTier;
        }

        @Override
        public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull SkillTreeEventData data) {
            // Handle close request from parent
            if (closeRequested) {
                if (parentUI != null) {
                    parentUI.isOpen = false;
                    parentUI.currentPage = null;
                }
                this.close();
                return;
            }

            String action = data.action;
            if (action == null) return;

            switch (action) {
                case "close":
                    if (parentUI != null) {
                        parentUI.isOpen = false;
                        parentUI.currentPage = null;
                    }
                    this.close();
                    break;

                case "select":
                    if (data.skillId != null && !data.skillId.isEmpty()) {
                        if (parentUI != null) {
                            parentUI.selectNode(data.skillId);
                        }
                        // Re-open this page to force a rebuild so selection details update
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            player.getPageManager().openCustomPage(ref, store, this);
                        }
                    }
                    break;

                case "unlock":
                    if (parentUI != null) {
                        String selectedId = parentUI.getSelectedSkillId();
                        if (selectedId != null) {
                            parentUI.unlockSelectedSkill();
                        }
                    }
                    // Re-open this page to refresh UI state after unlock
                    Player playerAfter = store.getComponent(ref, Player.getComponentType());
                    if (playerAfter != null) {
                        playerAfter.getPageManager().openCustomPage(ref, store, this);
                    }
                    break;

                default:
                    break;
            }
        }

        /**
         * Event data class for page events.
         */
        public static class SkillTreeEventData {
            public static final BuilderCodec<SkillTreeEventData> CODEC;

            public String action;
            public String skillId;

            static {
                CODEC = BuilderCodec.builder(SkillTreeEventData.class, SkillTreeEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (data, value) -> data.action = value,
                                (data) -> data.action)
                        .add()
                        .append(new KeyedCodec<>("SkillId", Codec.STRING),
                                (data, value) -> data.skillId = value,
                                (data) -> data.skillId)
                        .add()
                        .build();
            }
        }
    }
}
