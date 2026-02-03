package com.NextLVLHasH.Websockets;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Discord Bot Manager with full bot capabilities
 * - Send rich embeds
 * - Manage roles
 * - Bridge chat between Hytale and Discord
 * - Assign/manage usernames
 * - Reaction role system for in-game role display
 */
public class DiscordBot extends ListenerAdapter {
    private static final Logger LOGGER = Logger.getLogger(DiscordBot.class.getName());
    
    private JDA jda;
    private final String botToken;
    private final String serverName;
    private final String serverAddress;
    private final String serverDescription;
    private final String notificationChannelId;
    private final String chatBridgeChannelId;
    private final String linkedRoleId;
    
    // Reaction role settings
    private final boolean enableReactionRoles;
    private final String reactionRoleChannelId;
    private final String reactionRoleMessageTitle;
    private final String reactionRoleMessageDescription;
    private final List<RoleReactionConfig> reactionRoles;
    private String reactionRoleMessageId; // Store message ID for reaction handling
    
    // Emoji -> RoleReactionConfig mapping for quick lookup
    private final Map<String, RoleReactionConfig> emojiToRoleMap = new HashMap<>();
    
    // RoleId -> RoleReactionConfig mapping for button handling
    private final Map<String, RoleReactionConfig> roleIdToConfigMap = new HashMap<>();
    
    // Callback for chat messages from Discord to Hytale
    private ChatMessageCallback chatCallback;
    
    // Verification manager for link verification
    private LinkVerificationManager verificationManager;
    
    // Callback for when a player is verified
    private VerificationCallback verificationCallback;
    
    public DiscordBot(String botToken, String serverName, String serverAddress, 
                     String serverDescription, String notificationChannelId, 
                     String chatBridgeChannelId, String linkedRoleId,
                     boolean enableReactionRoles, String reactionRoleChannelId,
                     String reactionRoleMessageTitle, String reactionRoleMessageDescription,
                     List<RoleReactionConfig> reactionRoles) {
        this.botToken = botToken;
        this.serverName = serverName;
        this.serverAddress = serverAddress;
        this.serverDescription = serverDescription;
        this.notificationChannelId = notificationChannelId;
        this.chatBridgeChannelId = chatBridgeChannelId;
        this.linkedRoleId = linkedRoleId;
        this.enableReactionRoles = enableReactionRoles;
        this.reactionRoleChannelId = reactionRoleChannelId;
        this.reactionRoleMessageTitle = reactionRoleMessageTitle;
        this.reactionRoleMessageDescription = reactionRoleMessageDescription;
        this.reactionRoles = reactionRoles != null ? reactionRoles : List.of();
        
        // Build emoji -> role mapping and roleId -> config mapping
        for (RoleReactionConfig roleConfig : this.reactionRoles) {
            emojiToRoleMap.put(roleConfig.getEmojiId(), roleConfig);
            roleIdToConfigMap.put(roleConfig.getDiscordRoleId(), roleConfig);
        }
    }
    
    /**
     * Initialize and start the Discord bot
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Starting Discord bot...");
                
                jda = JDABuilder.createDefault(botToken)
                        .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.DIRECT_MESSAGES
                        )
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .addEventListeners(this)
                        .build();
                
                jda.awaitReady();
                LOGGER.info("Discord bot connected successfully!");
                LOGGER.info("Bot is in " + jda.getGuilds().size() + " guild(s)");
                
                // Setup reaction roles if enabled
                if (enableReactionRoles) {
                    LOGGER.info("Reaction roles enabled, setting up...");
                    LOGGER.info("Reaction role channel ID: " + reactionRoleChannelId);
                    LOGGER.info("Number of reaction roles configured: " + (reactionRoles != null ? reactionRoles.size() : 0));
                    setupReactionRoles();
                } else {
                    LOGGER.info("Reaction roles are disabled in config");
                }
                
            } catch (Exception e) {
                LOGGER.severe("Failed to start Discord bot: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Shutdown the Discord bot
     */
    public void shutdown() {
        if (jda != null) {
            LOGGER.info("Shutting down Discord bot...");
            jda.shutdown();
        }
    }
    
    /**
     * Handle incoming Discord messages
     */
    @Override
    public void onMessageReceived(@SuppressWarnings("null") MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) return;
        
        // Handle DMs for verification
        if (event.getChannelType() == ChannelType.PRIVATE) {
            handlePrivateMessage(event);
            return;
        }
        
        // Chat bridge - forward Discord messages to Hytale
        if (event.getChannel().getId().equals(chatBridgeChannelId)) {
            String discordUserId = event.getAuthor().getId();
            String discordUser = event.getAuthor().getName();
            String message = event.getMessage().getContentDisplay();
            
            // Check if user has the linkedRoleId - required to chat
            if (!hasLinkedRole(event.getMember())) {
                return;
            }
            
            // Always get role prefix from Discord roles (reaction roles)
            String rolePrefix = getPlayerRolePrefix(discordUserId);
            
            // Check if user is verified - if so, use their Hytale name
            if (verificationManager != null) {
                String hytaleUuid = verificationManager.getHytaleUuidByDiscordId(discordUserId);
                if (hytaleUuid != null) {
                    LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(hytaleUuid);
                    if (link != null) {
                        // Use Hytale name for verified users
                        discordUser = link.hytaleName;
                    }
                }
            }
            
            // Format display name with role prefix
            String displayName = rolePrefix.isEmpty() ? discordUser : rolePrefix + " " + discordUser;
            
            LOGGER.info("[Discord -> Hytale] " + displayName + ": " + message);
            
            // Callback to send message to Hytale server
            if (chatCallback != null) {
                chatCallback.onDiscordMessage(displayName, message);
            }
        }
    }
    
    /**
     * Handle private messages (DMs) for verification
     */
    private void handlePrivateMessage(MessageReceivedEvent event) {
        User user = event.getAuthor();
        String message = event.getMessage().getContentRaw().trim();
        String discordUsername = user.getName();
        String discordUserId = user.getId();
        
        LOGGER.info("Received DM from " + discordUsername + ": " + message);
        
        // Check if message looks like an auth code (6 digits)
        if (message.matches("\\d{6}") && verificationManager != null) {
            // Try to verify
            LinkVerificationManager.VerificationResult result = 
                verificationManager.verifyCode(discordUserId, discordUsername, message);
            
            if (result.success) {
                // Send success message
                @SuppressWarnings("null")
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ Account Linked Successfully!")
                    .setDescription("Your Discord account is now linked to Hytale.")
                    .setColor(Color.GREEN)
                    .addField("Hytale Player", result.hytaleName, true)
                    .addField("Discord User", discordUsername, true)
                    .setFooter("Your chat messages will now sync between Discord and Hytale!")
                    .setTimestamp(Instant.now());
                
                user.openPrivateChannel().queue(channel -> 
                    channel.sendMessageEmbeds(embed.build()).queue()
                );
                
                LOGGER.info("Successfully verified link: " + result.hytaleName + " <-> " + discordUsername);
                
                // Assign linked role
                assignLinkedRole(discordUserId);
                
                // Notify callback if set
                if (verificationCallback != null) {
                    verificationCallback.onVerified(result.hytaleUuid, discordUserId, discordUsername);
                }
            } else {
                // Send error message
                user.openPrivateChannel().queue(channel -> 
                    channel.sendMessage("❌ " + result.message).queue()
                );
            }
        } else {
            // Not a valid code, send help message
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔗 Hytale Account Linking")
                .setColor(Color.BLUE)
                .setDescription("To link your account, follow these steps:")
                .addField("Step 1", "In-game, type `/link " + discordUsername + "`", false)
                .addField("Step 2", "You'll receive a 6-digit code", false)
                .addField("Step 3", "Send me that code as a DM", false)
                .setFooter("Codes expire after 10 minutes");
            
            user.openPrivateChannel().queue(channel -> 
                channel.sendMessageEmbeds(embed.build()).queue()
            );
        }
    }
    
    /**
     * Handle button interactions for role selection
     * Roles are EXCLUSIVE - clicking a button removes all other configured roles
     */
    @SuppressWarnings("null")
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        // Check if this is a role button (format: "role_<roleId>")
        if (!buttonId.startsWith("role_")) return;
        
        String roleId = buttonId.substring(5); // Remove "role_" prefix
        RoleReactionConfig selectedRoleConfig = roleIdToConfigMap.get(roleId);
        
        if (selectedRoleConfig == null) {
            event.reply("❌ This role is no longer available.").setEphemeral(true).queue();
            return;
        }
        
        Member member = event.getMember();
        if (member == null) {
            event.reply("❌ Could not find your member information.").setEphemeral(true).queue();
            return;
        }
        
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ Could not find the server.").setEphemeral(true).queue();
            return;
        }
        
        Role selectedRole = guild.getRoleById(roleId);
        if (selectedRole == null) {
            event.reply("❌ The role could not be found.").setEphemeral(true).queue();
            return;
        }
        
        // Check if user already has this role
        boolean alreadyHasRole = member.getRoles().contains(selectedRole);
        
        if (alreadyHasRole) {
            // Remove the role (toggle off)
            guild.removeRoleFromMember(member, selectedRole).queue(
                success -> {
                    LOGGER.info("Removed role " + selectedRoleConfig.getRoleName() + " from " + member.getEffectiveName());
                    event.reply("✅ Removed role: **" + selectedRoleConfig.getRoleName() + "**\n" +
                               "You no longer have an in-game prefix.").setEphemeral(true).queue();
                },
                error -> {
                    LOGGER.warning("Failed to remove role: " + error.getMessage());
                    event.reply("❌ Failed to remove role: " + error.getMessage()).setEphemeral(true).queue();
                }
            );
        } else {
            // EXCLUSIVE: Remove all other configured roles first
            List<Role> rolesToRemove = new ArrayList<>();
            for (RoleReactionConfig otherConfig : reactionRoles) {
                if (!otherConfig.getDiscordRoleId().equals(roleId)) {
                    Role otherRole = guild.getRoleById(otherConfig.getDiscordRoleId());
                    if (otherRole != null && member.getRoles().contains(otherRole)) {
                        rolesToRemove.add(otherRole);
                    }
                }
            }
            
            // Remove other roles first, then add new role
            if (!rolesToRemove.isEmpty()) {
                for (Role roleToRemove : rolesToRemove) {
                    guild.removeRoleFromMember(member, roleToRemove).queue();
                    LOGGER.info("Removed old role " + roleToRemove.getName() + " from " + member.getEffectiveName());
                }
            }
            
            // Add the new role
            guild.addRoleToMember(member, selectedRole).queue(
                success -> {
                    LOGGER.info("Assigned role " + selectedRoleConfig.getRoleName() + " to " + member.getEffectiveName());
                    String removedInfo = rolesToRemove.isEmpty() ? "" : 
                        "\n*(Previous role was removed)*";
                    event.reply("✅ You now have the role: **" + selectedRoleConfig.getRoleName() + "**\n" +
                               "Your in-game prefix: `" + selectedRoleConfig.getInGamePrefix() + "`" + removedInfo)
                        .setEphemeral(true).queue();
                },
                error -> {
                    LOGGER.warning("Failed to assign role: " + error.getMessage());
                    event.reply("❌ Failed to assign role: " + error.getMessage()).setEphemeral(true).queue();
                }
            );
        }
    }
    
    /**
     * Handle reaction added to messages (legacy - kept for backwards compatibility)
     */
    @SuppressWarnings("null")
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Ignore bot reactions
        if (event.getUser() != null && event.getUser().isBot()) return;
        
        // Only handle reactions on the reaction role message
        if (!event.getMessageId().equals(reactionRoleMessageId)) return;
        
        String emojiName = getEmojiIdentifier(event);
        RoleReactionConfig roleConfig = emojiToRoleMap.get(emojiName);
        
        if (roleConfig != null && event.getMember() != null) {
            Guild guild = event.getGuild();
            Role role = guild.getRoleById(roleConfig.getDiscordRoleId());
            
            if (role != null) {
                guild.addRoleToMember(event.getMember(), role).queue(
                    success -> LOGGER.info("Assigned role " + roleConfig.getRoleName() + " to " + event.getMember().getEffectiveName()),
                    error -> LOGGER.warning("Failed to assign role: " + error.getMessage())
                );
            }
        }
    }
    
    /**
     * Handle reaction removed from messages (legacy - kept for backwards compatibility)
     */
    @SuppressWarnings("null")
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        // Only handle reactions on the reaction role message
        if (!event.getMessageId().equals(reactionRoleMessageId)) return;
        
        String emojiName = getEmojiIdentifier(event);
        RoleReactionConfig roleConfig = emojiToRoleMap.get(emojiName);
        
        if (roleConfig != null && event.getMember() != null) {
            Guild guild = event.getGuild();
            Role role = guild.getRoleById(roleConfig.getDiscordRoleId());
            
            if (role != null) {
                guild.removeRoleFromMember(event.getMember(), role).queue(
                    success -> LOGGER.info("Removed role " + roleConfig.getRoleName() + " from " + event.getMember().getEffectiveName()),
                    error -> LOGGER.warning("Failed to remove role: " + error.getMessage())
                );
            }
        }
    }
    
    /**
     * Setup reaction role message
     * Will search for existing message first and reuse it on restart
     */
    @SuppressWarnings("null")
    private void setupReactionRoles() {
        LOGGER.info("Setting up reaction roles...");
        
        if (reactionRoleChannelId == null || reactionRoleChannelId.isEmpty()) {
            LOGGER.warning("Reaction role channel ID not configured - set 'reactionRoleChannelId' in config.json");
            return;
        }
        
        if (reactionRoles == null || reactionRoles.isEmpty()) {
            LOGGER.warning("No reaction roles configured - add roles to 'reactionRoles' array in config.json");
            return;
        }
        
        LOGGER.info("Looking for reaction role channel with ID: " + reactionRoleChannelId);
        
        // Try TextChannel first
        GuildMessageChannel channel = jda.getTextChannelById(reactionRoleChannelId);
        
        // If not found, try NewsChannel (announcement channel)
        if (channel == null) {
            channel = jda.getNewsChannelById(reactionRoleChannelId);
            if (channel != null) {
                LOGGER.info("Found announcement/news channel for reaction roles: #" + channel.getName());
            }
        }
        
        if (channel == null) {
            LOGGER.severe("Reaction role channel not found: " + reactionRoleChannelId);
            LOGGER.severe("Available channels:");
            for (Guild guild : jda.getGuilds()) {
                LOGGER.info("  Text Channels:");
                for (TextChannel tc : guild.getTextChannels()) {
                    LOGGER.info("    - #" + tc.getName() + " (ID: " + tc.getId() + ")");
                }
                LOGGER.info("  Announcement/News Channels:");
                for (NewsChannel nc : guild.getNewsChannels()) {
                    LOGGER.info("    - #" + nc.getName() + " (ID: " + nc.getId() + ") [ANNOUNCEMENT]");
                }
            }
            return;
        }
        
        LOGGER.info("Found reaction role channel: #" + channel.getName() + " in guild: " + channel.getGuild().getName());
        
        // Search for existing reaction role message from the bot
        final GuildMessageChannel finalChannel = channel;
        LOGGER.info("Searching for existing reaction role message...");
        
        channel.getHistory().retrievePast(50).queue(messages -> {
            net.dv8tion.jda.api.entities.Message existingMessage = null;
            
            // Look for our reaction role message
            for (net.dv8tion.jda.api.entities.Message msg : messages) {
                // Check if message is from our bot and has embeds with our title
                if (msg.getAuthor().getId().equals(jda.getSelfUser().getId()) 
                    && !msg.getEmbeds().isEmpty()) {
                    
                    net.dv8tion.jda.api.entities.MessageEmbed embed = msg.getEmbeds().get(0);
                    if (embed.getTitle() != null && embed.getTitle().equals(reactionRoleMessageTitle)) {
                        existingMessage = msg;
                        LOGGER.info("✓ Found existing reaction role message! ID: " + msg.getId());
                        break;
                    }
                }
            }
            
            if (existingMessage != null) {
                // Reuse existing message - check if it has buttons
                this.reactionRoleMessageId = existingMessage.getId();
                LOGGER.info("✓ Reusing existing role message (ID: " + reactionRoleMessageId + ")");
                
                // Check if the message has buttons (new style) or reactions (old style)
                if (!existingMessage.getButtons().isEmpty()) {
                    LOGGER.info("✓ Existing message already has buttons");
                } else {
                    // Old message with reactions - edit to add buttons
                    LOGGER.info("Updating old reaction message to use buttons...");
                    updateMessageWithButtons(existingMessage);
                }
            } else {
                // Create new message
                LOGGER.info("No existing role message found, creating new one with buttons...");
                createRoleButtonMessage(finalChannel);
            }
        }, error -> {
            LOGGER.warning("Failed to search message history: " + error.getMessage());
            // Fall back to creating new message
            createRoleButtonMessage(finalChannel);
        });
    }
    
    /**
     * Update an existing message to use buttons instead of reactions
     */
    @SuppressWarnings("null")
    private void updateMessageWithButtons(net.dv8tion.jda.api.entities.Message message) {
        // Build buttons for roles
        List<Button> buttons = createRoleButtons();
        
        if (buttons.isEmpty()) {
            LOGGER.warning("No buttons to add - check role configuration");
            return;
        }
        
        // Edit message to add buttons (up to 5 per row, max 5 rows)
        List<ActionRow> actionRows = createActionRows(buttons);
        
        message.editMessageComponents(actionRows).queue(
            success -> LOGGER.info("✓ Updated message with buttons"),
            error -> LOGGER.warning("Failed to update message with buttons: " + error.getMessage())
        );
    }
    
    /**
     * Create role buttons from configuration
     */
    private List<Button> createRoleButtons() {
        List<Button> buttons = new ArrayList<>();
        
        // Define button colors to cycle through for variety
        ButtonStyle[] styles = {ButtonStyle.PRIMARY, ButtonStyle.SUCCESS, ButtonStyle.SECONDARY, ButtonStyle.DANGER};
        
        int styleIndex = 0;
        for (RoleReactionConfig roleConfig : reactionRoles) {
            String buttonId = "role_" + roleConfig.getDiscordRoleId();
            ButtonStyle style = styles[styleIndex % styles.length];
            
            @SuppressWarnings("null")
            Button button = Button.of(style, buttonId, roleConfig.getRoleName());
            
            // Add emoji if configured
            if (roleConfig.getEmojiId() != null && !roleConfig.getEmojiId().isEmpty()) {
                try {
                    Emoji emoji = parseEmoji(roleConfig.getEmojiId());
                    button = button.withEmoji(emoji);
                } catch (Exception e) {
                    LOGGER.warning("Could not parse emoji for button: " + roleConfig.getEmojiId());
                }
            }
            
            buttons.add(button);
            styleIndex++;
        }
        
        return buttons;
    }
    
    /**
     * Create action rows from buttons (max 5 buttons per row)
     */
    @SuppressWarnings("null")
    private List<ActionRow> createActionRows(List<Button> buttons) {
        List<ActionRow> rows = new ArrayList<>();
        
        for (int i = 0; i < buttons.size(); i += 5) {
            int end = Math.min(i + 5, buttons.size());
            rows.add(ActionRow.of(buttons.subList(i, end)));
        }
        
        return rows;
    }
    
    /**
     * Create a new role selection message with buttons
     */
    @SuppressWarnings("null")
    private void createRoleButtonMessage(GuildMessageChannel channel) {
        // Build embed with role descriptions
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(reactionRoleMessageTitle)
                .setDescription(reactionRoleMessageDescription)
                .setColor(Color.BLUE)
                .setTimestamp(Instant.now());
        
        // Add field for each role
        StringBuilder rolesText = new StringBuilder();
        for (RoleReactionConfig roleConfig : reactionRoles) {
            LOGGER.info("Adding role: " + roleConfig.getRoleName() + " with prefix: " + roleConfig.getInGamePrefix());
            rolesText.append("• **")
                     .append(roleConfig.getRoleName())
                     .append("** → `")
                     .append(roleConfig.getInGamePrefix())
                     .append("`\n");
        }
        
        embed.addField("Available Roles", rolesText.toString(), false);
        embed.addField("ℹ️ Note", "You can only have **one** role at a time.\nClick a button to select, click again to remove.", false);
        embed.setFooter("Roles will appear in-game with the shown prefix");
        
        // Create buttons
        List<Button> buttons = createRoleButtons();
        List<ActionRow> actionRows = createActionRows(buttons);
        
        // Send message with buttons
        LOGGER.info("Sending role button message to channel: " + channel.getName());
        channel.sendMessageEmbeds(embed.build())
            .setComponents(actionRows)
            .queue(message -> {
                this.reactionRoleMessageId = message.getId();
                LOGGER.info("✓ Role button message posted successfully! Message ID: " + message.getId());
            }, error -> {
                LOGGER.severe("✗ Failed to send role button message: " + error.getMessage());
                error.printStackTrace();
            });
    }
    
    /**
     * Get emoji identifier from reaction event
     */
    private String getEmojiIdentifier(MessageReactionAddEvent event) {
        if (event.getEmoji().getType() == Emoji.Type.UNICODE) {
            return event.getEmoji().getName();
        } else {
            return event.getEmoji().asCustom().getId();
        }
    }
    
    private String getEmojiIdentifier(MessageReactionRemoveEvent event) {
        if (event.getEmoji().getType() == Emoji.Type.UNICODE) {
            return event.getEmoji().getName();
        } else {
            return event.getEmoji().asCustom().getId();
        }
    }
    
    /**
     * Parse emoji from config string
     */
    private Emoji parseEmoji(String emojiId) {
        // Check if it's a custom emoji (ID) or unicode
        if (emojiId.matches("\\d+")) {
            // Custom emoji ID
            return Emoji.fromCustom("custom", Long.parseLong(emojiId), false);
        } else {
            // Unicode emoji
            return Emoji.fromUnicode(emojiId);
        }
    }
    
    /**
     * Get player's Discord role prefix
     */
    public String getPlayerRolePrefix(String discordUserId) {
        if (jda == null || reactionRoles == null || reactionRoles.isEmpty()) {
            return "";
        }
        
        for (Guild guild : jda.getGuilds()) {
            @SuppressWarnings("null")
            Member member = guild.getMemberById(discordUserId);
            if (member != null) {
                // Check which reaction roles the member has
                for (RoleReactionConfig roleConfig : reactionRoles) {
                    @SuppressWarnings("null")
                    Role role = guild.getRoleById(roleConfig.getDiscordRoleId());
                    if (role != null && member.getRoles().contains(role)) {
                        return roleConfig.getInGamePrefix();
                    }
                }
            }
        }
        
        return "";
    }
    
    /**
     * Check if a member has the linked role (required for chat bridge)
     */
    public boolean hasLinkedRole(Member member) {
        if (member == null || linkedRoleId == null || linkedRoleId.isEmpty()) {
            return false;
        }
        
        @SuppressWarnings("null")
        Role linkedRole = member.getGuild().getRoleById(linkedRoleId);
        if (linkedRole == null) {
            LOGGER.warning("Linked role not found: " + linkedRoleId);
            return false;
        }
        
        return member.getRoles().contains(linkedRole);
    }
    
    /**
     * Check if a Discord user ID has the linked role
     */
    public boolean hasLinkedRole(String discordUserId) {
        if (jda == null || linkedRoleId == null || linkedRoleId.isEmpty()) {
            return false;
        }
        
        for (Guild guild : jda.getGuilds()) {
            @SuppressWarnings("null")
            Member member = guild.getMemberById(discordUserId);
            if (member != null) {
                return hasLinkedRole(member);
            }
        }
        
        return false;
    }
    
    /**
     * Get Discord username by Discord ID
     */
    public String getDiscordUsername(String discordUserId) {
        if (jda == null) return null;
        
        for (Guild guild : jda.getGuilds()) {
            @SuppressWarnings("null")
            Member member = guild.getMemberById(discordUserId);
            if (member != null) {
                return member.getEffectiveName();
            }
        }
        
        return null;
    }
    
    /**
     * Send player join notification
     */
    @SuppressWarnings("null")
    public void sendPlayerJoinNotification(String playerName, int totalPlayers) {
        GuildMessageChannel channel = getNotificationChannel();
        if (channel == null) return;     
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Player Joined")
                .setDescription("**" + playerName + "** has joined the server!")
                .setColor(Color.GREEN)
                .addField("👤 Player", playerName, true)
                .addField("👥 Players Online", "**" + totalPlayers + "**", true)
                .addField("🎮 Server", serverName, false)
                .addField("📍 Server Address", "```" + serverAddress + "```", false)
                .setTimestamp(Instant.now());
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            embed.addField("ℹ️ About", serverDescription, false);
        }
        
        channel.sendMessageEmbeds(embed.build()).queue(
            success -> LOGGER.info("Join notification sent for " + playerName),
            error -> LOGGER.warning("Failed to send join notification: " + error.getMessage())
        );
    }
    
    /**
     * Send player leave notification
     */
    @SuppressWarnings("null")
    public void sendPlayerLeaveNotification(String playerName, int totalPlayers) {
        GuildMessageChannel channel = getNotificationChannel();
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Player Left")
                .setDescription("**" + playerName + "** has left the server.")
                .setColor(Color.RED)
                .addField("👤 Player", playerName, true)
                .addField("👥 Players Online", "**" + totalPlayers + "**", true)
                .addField("🎮 Server", serverName, false)
                .addField("📍 Server Address", "```" + serverAddress + "```", false)
                .setTimestamp(Instant.now());
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            embed.addField("ℹ️ About", serverDescription, false);
        }
        
        channel.sendMessageEmbeds(embed.build()).queue(
            success -> LOGGER.info("Leave notification sent for " + playerName),
            error -> LOGGER.warning("Failed to send leave notification: " + error.getMessage())
        );
    }
    
    /**
     * Send server start notification
     */
    @SuppressWarnings("null")
    public void sendServerStartNotification(int totalPlayers) {
        GuildMessageChannel channel = getNotificationChannel();
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🟢 Server Started")
                .setDescription("**" + serverName + "** is now online!")
                .setColor(Color.GREEN)
                .addField("🎮 Server", serverName, true)
                .addField("👥 Players Online", "**" + totalPlayers + "**", true)
                .addField("📍 Server Address", "```" + serverAddress + "```", false)
                .setTimestamp(Instant.now());
        
        if (serverDescription != null && !serverDescription.isEmpty()) {
            embed.addField("ℹ️ About", serverDescription, false);
        }
        
        channel.sendMessageEmbeds(embed.build()).queue(
            success -> LOGGER.info("Server start notification sent"),
            error -> LOGGER.warning("Failed to send server start notification: " + error.getMessage())
        );
    }
    
    /**
     * Send server stop notification
     */
    public void sendServerStopNotification(int totalPlayers) {
        GuildMessageChannel channel = getNotificationChannel();
        if (channel == null) return;
        
        @SuppressWarnings("null")
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔴 Server Stopped")
                .setDescription("**" + serverName + "** is now offline.")
                .setColor(Color.RED)
                .addField("🎮 Server", serverName, true)
                .addField("👥 Final Player Count", "**" + totalPlayers + "**", true)
                .setTimestamp(Instant.now());
        
        channel.sendMessageEmbeds(embed.build()).queue(
            success -> LOGGER.info("Server stop notification sent"),
            error -> LOGGER.warning("Failed to send server stop notification: " + error.getMessage())
        );
    }
    
    /**
     * Send Hytale chat message to Discord
     */
    @SuppressWarnings("null")
    public void sendHytaleChatToDiscord(String playerName, String message) {
        if (chatBridgeChannelId == null || chatBridgeChannelId.isEmpty()) {
            LOGGER.warning("Chat bridge channel ID not configured");
            return;
        }
        
        // Try TextChannel first
        GuildMessageChannel channel = jda.getTextChannelById(chatBridgeChannelId);
        
        // If not found, try NewsChannel (announcement channel)
        if (channel == null) {
            channel = jda.getNewsChannelById(chatBridgeChannelId);
        }
        
        if (channel == null) {
            LOGGER.warning("Chat bridge channel not found: " + chatBridgeChannelId);
            return;
        }
        
        String formattedMessage = "**[Hytale]** `" + playerName + "`: " + message;
        channel.sendMessage(formattedMessage).queue(
            success -> LOGGER.info("Chat message sent to Discord: " + playerName + ": " + message),
            error -> LOGGER.warning("Failed to send chat to Discord: " + error.getMessage())
        );
    }
    
    /**
     * Assign role to Discord user by Discord ID
     */
    @SuppressWarnings({ "null" })
    public void assignRole(String discordUserId, String roleName) {
        if (jda == null) return;
        
        jda.getGuilds().forEach(guild -> {

            Member member = guild.getMemberById(discordUserId);
            if (member != null) {

                List<Role> roles = guild.getRolesByName(roleName, true);
                if (!roles.isEmpty()) {
                    guild.addRoleToMember(member, roles.get(0)).queue(
                        success -> LOGGER.info("Assigned role " + roleName + " to " + member.getEffectiveName()),
                        error -> LOGGER.warning("Failed to assign role: " + error.getMessage())
                    );
                }
            }
        });
    }
    
    /**
     * Remove role from Discord user
     */
    @SuppressWarnings({"null"})
    public void removeRole(String discordUserId, String roleName) {
        if (jda == null) return;
        
        jda.getGuilds().forEach(guild -> {
         Member member = guild.getMemberById(discordUserId);
            if (member != null) {
                List<Role> roles = guild.getRolesByName(roleName, true);
                if (!roles.isEmpty()) {
                    guild.removeRoleFromMember(member, roles.get(0)).queue(
                        success -> LOGGER.info("Removed role " + roleName + " from " + member.getEffectiveName()),
                        error -> LOGGER.warning("Failed to remove role: " + error.getMessage())
                    );
                }
            }
        });
    }
    
    /**
     * Assign linked role to player when they join
     */
    public void assignLinkedRole(String discordUserId) {
        if (linkedRoleId == null || linkedRoleId.isEmpty()) return;
        
        if (jda == null) return;
        
        jda.getGuilds().forEach(guild -> {
            @SuppressWarnings("null")
            Member member = guild.getMemberById(discordUserId);
            @SuppressWarnings("null")
            Role role = guild.getRoleById(linkedRoleId);
            
            if (member != null && role != null) {
                guild.addRoleToMember(member, role).queue(
                    success -> LOGGER.info("Assigned linked role to " + member.getEffectiveName()),
                    error -> LOGGER.warning("Failed to assign linked role: " + error.getMessage())
                );
            }
        });
    }
    
    /**
     * Set callback for Discord messages
     */
    public void setChatCallback(ChatMessageCallback callback) {
        this.chatCallback = callback;
    }
    
    /**
     * Set verification manager for link verification
     */
    public void setVerificationManager(LinkVerificationManager manager) {
        this.verificationManager = manager;
    }
    
    /**
     * Set callback for when verification succeeds
     */
    public void setVerificationCallback(VerificationCallback callback) {
        this.verificationCallback = callback;
    }
    
    /**
     * Send a DM to a Discord user asking them to verify their link
     * @param discordUsername The Discord username to find
     * @param hytaleName The Hytale player name linking
     * @param authCode The auth code to verify with
     */
    public void sendLinkRequestDM(String discordUsername, String hytaleName, String authCode) {
        if (jda == null) return;
        
        // Find user in guilds
        for (Guild guild : jda.getGuilds()) {
            @SuppressWarnings("null")
            List<Member> members = guild.getMembersByName(discordUsername, true);
            if (!members.isEmpty()) {
                Member member = members.get(0);
                User user = member.getUser();
                
                // Update the pending verification with the Discord user ID
                if (verificationManager != null) {
                    verificationManager.updatePendingDiscordUserId(authCode, user.getId());
                }
                
                @SuppressWarnings("null")
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🔗 Hytale Account Link Request")
                    .setColor(Color.ORANGE)
                    .setDescription("A Hytale player wants to link their account to your Discord!")
                    .addField("Hytale Player", hytaleName, true)
                    .addField("Server", serverName, true)
                    .addField("", "", false)
                    .addField("Your Verification Code", "```" + authCode + "```", false)
                    .addField("To complete linking:", "Type this code in the **Hytale game chat**", false)
                    .setFooter("⚠️ Only do this if you requested the link! Code expires in 10 minutes.")
                    .setTimestamp(Instant.now());
                
                user.openPrivateChannel().queue(
                    channel -> channel.sendMessageEmbeds(embed.build()).queue(
                        success -> LOGGER.info("Sent link request DM to " + discordUsername),
                        error -> LOGGER.warning("Failed to send DM: " + error.getMessage())
                    ),
                    error -> LOGGER.warning("Failed to open DM channel: " + error.getMessage())
                );
                
                return;
            }
        }
        
        LOGGER.info("Could not find Discord user: " + discordUsername + " - they may need to DM the bot first");
    }
    
    /**
     * Get role prefix for a verified player based on their Discord roles
     * @param hytaleUuid The Hytale player UUID
     * @return Role prefix string or empty string
     */
    public String getVerifiedPlayerRolePrefix(String hytaleUuid) {
        if (verificationManager == null || jda == null) return "";
        
        LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(hytaleUuid);
        if (link == null) return "";
        
        return getPlayerRolePrefix(link.discordUserId);
    }
    
    /**
     * Get Discord username for a verified player
     * @param hytaleUuid The Hytale player UUID
     * @return Discord username or null
     */
    public String getVerifiedPlayerDiscordName(String hytaleUuid) {
        if (verificationManager == null) return null;
        
        LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(hytaleUuid);
        if (link == null) return null;
        
        return link.discordUsername;
    }
    
    /**
     * Get notification channel (supports both TextChannel and NewsChannel/Announcement channels)
     */
    @SuppressWarnings("null")
    private GuildMessageChannel getNotificationChannel() {
        if (jda == null) {
            LOGGER.warning("Cannot get notification channel - JDA not initialized");
            return null;
        }
        
        if (notificationChannelId == null || notificationChannelId.isEmpty()) {
            LOGGER.warning("Cannot get notification channel - notificationChannelId not configured in config.json");
            return null;
        }
        
        // Try TextChannel first
        GuildMessageChannel channel = jda.getTextChannelById(notificationChannelId);
        
        // If not found, try NewsChannel (announcement channel)
        if (channel == null) {
            channel = jda.getNewsChannelById(notificationChannelId);
            if (channel != null) {
                LOGGER.info("Found announcement/news channel: #" + channel.getName());
            }
        }
        
        if (channel == null) {
            LOGGER.severe("Notification channel NOT FOUND with ID: " + notificationChannelId);
            LOGGER.severe("Make sure the bot is in the server and has access to the channel!");
            LOGGER.info("Available channels the bot can see:");
            for (Guild guild : jda.getGuilds()) {
                LOGGER.info("  Guild: " + guild.getName() + " (ID: " + guild.getId() + ")");
                LOGGER.info("  Text Channels:");
                for (TextChannel tc : guild.getTextChannels()) {
                    LOGGER.info("    - #" + tc.getName() + " (ID: " + tc.getId() + ")");
                }
                LOGGER.info("  Announcement/News Channels:");
                for (NewsChannel nc : guild.getNewsChannels()) {
                    LOGGER.info("    - #" + nc.getName() + " (ID: " + nc.getId() + ") [ANNOUNCEMENT]");
                }
            }
            if (jda.getGuilds().isEmpty()) {
                LOGGER.severe("Bot is not in ANY servers! Invite the bot to your Discord server first.");
            }
        }
        
        return channel;
    }
    
    /**
     * Check if bot is ready
     */
    public boolean isReady() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }
    
    /**
     * Callback interface for Discord chat messages
     */
    @FunctionalInterface
    public interface ChatMessageCallback {
        void onDiscordMessage(String discordUser, String message);
    }
    
    /**
     * Callback interface for when a player is verified
     */
    @FunctionalInterface
    public interface VerificationCallback {
        void onVerified(String hytaleUuid, String discordUserId, String discordUsername);
    }
}
