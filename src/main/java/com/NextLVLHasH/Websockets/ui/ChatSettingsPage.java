package com.NextLVLHasH.Websockets.ui;

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

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;

import javax.annotation.Nonnull;

import com.NextLVLHasH.Websockets.ChatSettingsManager;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.LinkVerificationManager;
import com.NextLVLHasH.Websockets.PlayerLinkManager;
import com.NextLVLHasH.Websockets.DiscordBot;

/**
 * Settings page for chat system configuration.
 * Allows changing chat opacity and visibility preferences.
 * Also allows Discord account linking.
 */
@SuppressWarnings("unused")
public class ChatSettingsPage extends InteractiveCustomUIPage<ChatSettingsPage.SettingsData> {

    public static final String LAYOUT = "WebsocketNotificationMod/chatsettings_test.ui";
    private final ChatSettingsManager settingsManager;
    private final WebsocketNotificationMod plugin;


    public ChatSettingsPage(@Nonnull PlayerRef playerRef, @Nonnull ChatSettingsManager manager, @Nonnull WebsocketNotificationMod plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, SettingsData.CODEC);
        this.settingsManager = manager;
        this.plugin = plugin;
    }

    // Track hidden state locally since we use a toggle button
    private boolean hiddenState = false;
    
    // Opacity values to cycle through
    private static final int[] OPACITY_VALUES = {0, 20, 40, 60, 80, 100};
    private int opacityIndex = 5; // Default to 100%

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder ui,
                      @Nonnull UIEventBuilder events,
                      @Nonnull Store<EntityStore> store) {
        ui.append(LAYOUT);

        String playerUuid = playerRef.getUuid().toString();
        
        // Get current settings and set initial values
        ChatSettingsManager.Settings settings = settingsManager.getSettings(playerUuid);
        
        // Find the closest opacity index for saved value
        opacityIndex = findClosestOpacityIndex(settings.opacityPercent);
        ui.set("#OpacityValue.Text", OPACITY_VALUES[opacityIndex] + "%");
        
        // Initialize hidden state from settings
        hiddenState = settings.hidden;
        ui.set("#HiddenValue.Text", hiddenState ? "ON" : "OFF");
        
        // Check and display Discord link status
        LinkVerificationManager verificationManager = plugin.getVerificationManager();
        if (verificationManager != null && verificationManager.isVerified(playerUuid)) {
            LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(playerUuid);
            ui.set("#LinkStatusLabel.Text", "Linked: " + link.discordUsername);
            ui.set("#LinkButton.Background", "#555555"); // Dim the button
        } else {
            ui.set("#LinkStatusLabel.Text", "Not linked - Enter Discord username below");
        }

        // Toggle opacity button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#ToggleOpacityButton",
                new EventData().put("Action", "TOGGLE_OPACITY"),
                false);

        // Toggle hidden button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#ToggleHiddenButton",
                new EventData().put("Action", "TOGGLE_HIDDEN"),
                false);

        // TextField validation (Enter key pressed) - capture value when Enter is pressed
        events.addEventBinding(CustomUIEventBindingType.Validating,
                "#DiscordUsernameInput",
                new EventData()
                    .put("Action", "LINK_DISCORD")
                    .put("@DiscordUsername", "#DiscordUsernameInput.Value"),
                false);

        // Link button click - capture TextField value and send with action
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#LinkButton",
                new EventData()
                    .put("Action", "LINK_DISCORD")
                    .put("@DiscordUsername", "#DiscordUsernameInput.Value"),
                false);

        // Save button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData().put("Action", "SAVE"),
                false);

        // Cancel button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CancelButton",
                new EventData().put("Action", "CANCEL"),
                false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull SettingsData data) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Action action = Action.from(data.action);
        if (action == null) {
            sendUpdate();
            return;
        }

        switch (action) {
            case TOGGLE_OPACITY:
                // Cycle to next opacity value
                opacityIndex = (opacityIndex + 1) % OPACITY_VALUES.length;
                UICommandBuilder opacityCmd = new UICommandBuilder();
                opacityCmd.set("#OpacityValue.Text", OPACITY_VALUES[opacityIndex] + "%");
                sendUpdate(opacityCmd, new UIEventBuilder(), false);
                break;

            case TOGGLE_HIDDEN:
                // Toggle the hidden state and update label
                hiddenState = !hiddenState;
                UICommandBuilder toggleCmd = new UICommandBuilder();
                toggleCmd.set("#HiddenValue.Text", hiddenState ? "ON" : "OFF");
                sendUpdate(toggleCmd, new UIEventBuilder(), false);
                break;

            case LINK_DISCORD:
                // Handle Discord linking with captured username from TextField
                String usernameToUse = data.discordUsername;
                plugin.getLogger().atInfo().log("[UI] Link clicked, captured username: '" + usernameToUse + "'");
                handleDiscordLink(player, usernameToUse);
                break;

            case SAVE:
                // Use tracked opacity value
                int opacity = OPACITY_VALUES[opacityIndex];

                // Save settings
                settingsManager.setSettings(
                        playerRef.getUuid().toString(),
                        "#ffffff",
                        opacity,
                        hiddenState
                );

                // Apply hidden setting to actual HUD
                HudManager hudManager = player.getHudManager();
                if (hiddenState) {
                    // Hide the native chat HUD component
                    hudManager.hideHudComponents(playerRef, HudComponent.Chat);
                } else {
                    // Show the native chat HUD component
                    hudManager.showHudComponents(playerRef, HudComponent.Chat);
                }

                player.sendMessage(Message.raw("Chat settings saved! Opacity: " + opacity + "%" + 
                        (hiddenState ? " (Chat hidden)" : "")));
                this.close();
                break;

            case CANCEL:
                this.close();
                break;
        }
    }

    private enum Action {
        TOGGLE_OPACITY,
        TOGGLE_HIDDEN,
        LINK_DISCORD,
        SAVE,
        CANCEL;
        
        static Action from(String raw) {
            if (raw == null) {
                return null;
            }
            try {
                return valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static class SettingsData {
        public static final BuilderCodec<SettingsData> CODEC;

        public String action;
        public String discordUsername;

        static {
            CODEC = BuilderCodec.builder(SettingsData.class, SettingsData::new)
                    .append(new KeyedCodec<>("Action", Codec.STRING),
                            (data, value) -> data.action = value,
                            (data) -> data.action)
                    .add()
                    
                    .append(new KeyedCodec<>("DiscordUsername", Codec.STRING),
                            (data, value) -> data.discordUsername = value,
                            (data) -> data.discordUsername)
                    .add()

                    .build();
        }
    }
    
    // Handle Discord link request from UI
    private void handleDiscordLink(Player player, String discordUsername) {
        String playerUuid = playerRef.getUuid().toString();
        LinkVerificationManager verificationManager = plugin.getVerificationManager();
        
        // Debug logging
        plugin.getLogger().atInfo().log("[UI Link] Button clicked by " + player.getDisplayName());
        plugin.getLogger().atInfo().log("[UI Link] Captured username: '" + discordUsername + "'");
        
        // Check if already linked
        if (verificationManager.isVerified(playerUuid)) {
            LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(playerUuid);
            if (link != null) {
                player.sendMessage(Message.raw("You are already linked to Discord as: " + link.discordUsername));
                player.sendMessage(Message.raw("Use /unlink to remove your link first."));
                return;
            }
        }
        
        // If no username provided, show instructions
        if (discordUsername == null || discordUsername.trim().isEmpty()) {
            plugin.getLogger().atInfo().log("[UI Link] Username was null or empty");
            player.sendMessage(Message.raw("=== Discord Account Linking ==="));
            player.sendMessage(Message.raw("Enter your Discord username in the field above"));
            player.sendMessage(Message.raw("Then click LINK or press Enter"));
            player.sendMessage(Message.raw(" "));
            player.sendMessage(Message.raw("Or use: /link YourDiscordUsername"));
            return;
        }
        
        discordUsername = discordUsername.trim();
        plugin.getLogger().atInfo().log("[UI Link] Processing link request for: " + discordUsername);
        String playerName = player.getDisplayName();
        String authCode = verificationManager.generateAuthCode(playerUuid, playerName, discordUsername);
        
        // Try to send DM via Discord bot
        DiscordBot discordBot = plugin.getDiscordBot();
        if (discordBot != null && discordBot.isReady()) {
            boolean dmSent = discordBot.sendLinkRequestDM(discordUsername, playerName, authCode);
            
            if (dmSent) {
                player.sendMessage(Message.raw("Link request sent! Check your Discord DMs."));
                player.sendMessage(Message.raw("After receiving the code, type it in game chat to verify."));
                
                // Update the UI status label
                UICommandBuilder updateCmd = new UICommandBuilder();
                updateCmd.set("#LinkStatusLabel.Text", "Pending: " + discordUsername);
                sendUpdate(updateCmd, new UIEventBuilder(), false);
            } else {
                player.sendMessage(Message.raw("Could not find Discord user: " + discordUsername));
                player.sendMessage(Message.raw(" "));
                player.sendMessage(Message.raw("Please make sure:"));
                player.sendMessage(Message.raw("1. The Discord username is spelled correctly"));
                player.sendMessage(Message.raw("2. The user is a member of the linked Discord server"));
            }
        } else {
            player.sendMessage(Message.raw("Discord bot not available. Please try again later."));
        }
    }

    // Helper method to find closest opacity index
    private int findClosestOpacityIndex(int value) {
        int closestIndex = 0;
        int minDiff = Math.abs(OPACITY_VALUES[0] - value);
        for (int i = 1; i < OPACITY_VALUES.length; i++) {
            int diff = Math.abs(OPACITY_VALUES[i] - value);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        return closestIndex;
    }
}
