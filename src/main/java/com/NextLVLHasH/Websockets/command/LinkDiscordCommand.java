package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.LinkVerificationManager;

import javax.annotation.Nonnull;

/**
 * Command to link Discord account to Hytale player
 * Usage: /link <discord_username>
 * 
 * Flow:
 * 1. Player runs /link DiscordUser#1234
 * 2. Player receives auth code in-game
 * 3. Player DMs the bot with the auth code
 * 4. Bot verifies and links the accounts
 */
public class LinkDiscordCommand extends CommandBase {
    
    private final WebsocketNotificationMod plugin;
    
    // Define the discord username argument using Hytale's argument system
    private final RequiredArg<String> discordUsernameArg;
     public LinkDiscordCommand(WebsocketNotificationMod plugin) {
        super("link", "websockets.commands.link.desc");
        this.plugin = plugin;
        
        // Register the required discord username argument
        this.discordUsernameArg = this.withRequiredArg(
            "discord_username",
            "websockets.commands.link.discord_username.desc",
            ArgTypes.STRING
        );
    }
    
    @Override
    protected boolean canGeneratePermission() {
        // Allow everyone to use this command - no permission required
        return false;
    }
    
    protected void executeSync(@Nonnull CommandContext context) {
        plugin.getLogger().atInfo().log("Link command executed");
        
        // Check if sender is a player
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }
        
        // Get player info
        Player player = context.senderAs(Player.class);
        @SuppressWarnings("removal")
        String playerUuid = player.getPlayerRef().getUuid().toString();
        String playerName = player.getDisplayName();
        
        plugin.getLogger().atInfo().log("Link command from player: " + playerName + " (" + playerUuid + ")");
        
        // Check if player is already verified
        LinkVerificationManager verificationManager = plugin.getVerificationManager();
        if (verificationManager.isVerified(playerUuid)) {
            LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(playerUuid);
            context.sendMessage(Message.raw("You are already linked to Discord user: " + link.discordUsername));
            context.sendMessage(Message.raw("Use /unlink to remove your link first."));
            return;
        }
        
        // Check if Discord bot is ready
        if (plugin.getDiscordBot() == null || !plugin.getDiscordBot().isReady()) {
            context.sendMessage(Message.raw("Discord bot is not available. Please try again later."));
            plugin.getLogger().atWarning().log("Discord bot not ready for link command");
            return;
        }
        
        // Get Discord username from the proper argument system
        String discordUsername = discordUsernameArg.get(context);
        plugin.getLogger().atInfo().log("Discord username from arg: " + discordUsername);
        
        if (discordUsername == null || discordUsername.isEmpty()) {
            context.sendMessage(Message.raw("=== Discord Account Linking ==="));
            context.sendMessage(Message.raw("Link your Discord account to show your Discord roles in chat!"));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("Usage: /link <discord_username>"));
            context.sendMessage(Message.raw("Example: /link CoolPlayer123"));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("After running this command, DM the bot with the auth code."));
            return;
        }
        
        // Generate auth code
        String authCode = verificationManager.generateAuthCode(playerUuid, playerName, discordUsername);
        
        plugin.getLogger().atInfo().log("Generated auth code for " + playerName + " -> " + discordUsername);
        
        // Try to send DM to the Discord user with the code
        boolean dmSent = plugin.getDiscordBot().sendLinkRequestDM(discordUsername, playerName, authCode);
        
        if (dmSent) {
            // Send instructions to player
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("=== Discord Link Request ==="));
            context.sendMessage(Message.raw("Linking to Discord user: " + discordUsername));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("A verification code has been sent to your Discord DM."));
            context.sendMessage(Message.raw("Check your Discord messages from the bot."));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("Once you have the code, type it in THIS game chat."));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("Code expires in 10 minutes"));
            context.sendMessage(Message.raw(" "));
            
            plugin.getLogger().atInfo().log("Link request created for " + playerName + " -> " + discordUsername);
        } else {
            // Failed to find Discord user - give helpful feedback
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("=== Discord Link Failed ==="));
            context.sendMessage(Message.raw("Could not find Discord user: " + discordUsername));
            context.sendMessage(Message.raw(" "));
            context.sendMessage(Message.raw("Please make sure:"));
            context.sendMessage(Message.raw("1. The Discord username is spelled correctly"));
            context.sendMessage(Message.raw("2. The user is a member of the linked Discord server"));
            context.sendMessage(Message.raw("3. Try using their exact Discord display name"));
            context.sendMessage(Message.raw(" "));
            
            plugin.getLogger().atWarning().log("Failed to find Discord user: " + discordUsername + " for " + playerName);
        }
    }
}
