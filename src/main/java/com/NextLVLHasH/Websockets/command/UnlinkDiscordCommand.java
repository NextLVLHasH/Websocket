package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.LinkVerificationManager;

import javax.annotation.Nonnull;

/**
 * Command to unlink Discord account from Hytale player
 * Usage: /unlink
 */
public class UnlinkDiscordCommand extends CommandBase {
    
    private final WebsocketNotificationMod plugin;
    
    public UnlinkDiscordCommand(WebsocketNotificationMod plugin) {
        super("unlink", "websockets.commands.unlink.desc");
        this.plugin = plugin;
    }
    
    @Override
    protected boolean canGeneratePermission() {
        // Allow everyone to use this command - no permission required
        return false;
    }
    
    protected void executeSync(@Nonnull CommandContext context) {
        plugin.getLogger().atInfo().log("Unlink command executed");
        
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
        
        plugin.getLogger().atInfo().log("Unlink command from player: " + playerName);
        
        // Check if player is linked
        LinkVerificationManager verificationManager = plugin.getVerificationManager();
        if (!verificationManager.isVerified(playerUuid)) {
            context.sendMessage(Message.raw("You don't have a linked Discord account."));
            context.sendMessage(Message.raw("Use /link <discord_username> to link your account."));
            return;
        }
        
        // Get link info before removing
        LinkVerificationManager.VerifiedLink link = verificationManager.getVerifiedLink(playerUuid);
        String discordUsername = link != null ? link.discordUsername : "Unknown";
        
        // Unlink
        verificationManager.unlink(playerUuid);
        
        context.sendMessage(Message.raw("Successfully unlinked your Discord account!"));
        context.sendMessage(Message.raw("Previously linked to: " + discordUsername));
        
        plugin.getLogger().atInfo().log("Player " + playerName + " unlinked from Discord: " + discordUsername);
    }
}
