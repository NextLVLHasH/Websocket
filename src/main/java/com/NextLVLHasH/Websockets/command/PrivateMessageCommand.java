package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.PrivateMessageLogger;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Private message command that supports both player names and Discord usernames.
 * Usage: /msg <playername|discordname> <message>
 * 
 * For multi-word messages, use quotes: /msg player "hello world"
 * 
 * The command will first try to find an online player by their in-game name,
 * then fall back to searching by linked Discord username.
 */
public class PrivateMessageCommand extends CommandBase {

    private final WebsocketNotificationMod plugin;
    
    // Define arguments
    private final RequiredArg<String> targetArg;
    private final RequiredArg<String> messageArg;
    public PrivateMessageCommand(WebsocketNotificationMod plugin) {
        super("msg", "websockets.commands.msg.desc");
        this.plugin = plugin;
        
        // Register target argument (player name or discord name)
        this.targetArg = this.withRequiredArg(
            "target",
            "websockets.commands.msg.target.desc",
            ArgTypes.STRING
        );
        
        // Register message argument (use quotes for multi-word messages)
        this.messageArg = this.withRequiredArg(
            "message",
            "websockets.commands.msg.message.desc",
            ArgTypes.STRING
        );
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    protected void executeSync(@Nonnull CommandContext context) {
        // Check if sender is a player
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }
        
        Player sender = context.senderAs(Player.class);
        @SuppressWarnings("removal")
        PlayerRef senderRef = sender.getPlayerRef();
        
        // Get target and message from arguments
        String targetName = targetArg.get(context);
        String message = messageArg.get(context);
        
        if (targetName == null || targetName.isEmpty()) {
            context.sendMessage(Message.raw("Usage: /msg <playername|discordname> <message>"));
            context.sendMessage(Message.raw("For multi-word messages, use quotes: /msg player \"hello world\""));
            return;
        }
        
        if (message == null || message.isEmpty()) {
            context.sendMessage(Message.raw("Usage: /msg <playername|discordname> <message>"));
            context.sendMessage(Message.raw("For multi-word messages, use quotes: /msg player \"hello world\""));
            return;
        }
        
        // Try to find the target player
        RecipientResult result = findRecipient(targetName);
        
        if (result == null) {
            context.sendMessage(Message.raw("Player '" + targetName + "' not found online (checked player names and Discord names)"));
            return;
        }
        
        // Send the message to the recipient
        sendPrivateMessage(sender, senderRef, result, message);
        
        // Confirm to sender
        String lookupIndicator = result.lookupType == PrivateMessageLogger.LookupType.DISCORD_NAME ? " (Discord)" : "";
        context.sendMessage(Message.raw("[PM] To " + result.displayName + lookupIndicator + ": " + message));
    }
    
    /**
     * Find a recipient by player name or Discord username
     */
    private RecipientResult findRecipient(String targetName) {
        Map<String, PlayerRef> onlinePlayers = plugin.getOnlinePlayers();
        String targetLower = targetName.toLowerCase();
        
        // First, try exact match on player name
        for (Map.Entry<String, PlayerRef> entry : onlinePlayers.entrySet()) {
            PlayerRef playerRef = entry.getValue();
            if (playerRef.getUsername().equalsIgnoreCase(targetName)) {
                return new RecipientResult(
                    entry.getKey(),
                    playerRef,
                    playerRef.getUsername(),
                    PrivateMessageLogger.LookupType.PLAYER_NAME
                );
            }
        }
        
        // Second, try partial match on player name
        for (Map.Entry<String, PlayerRef> entry : onlinePlayers.entrySet()) {
            PlayerRef playerRef = entry.getValue();
            if (playerRef.getUsername().toLowerCase().startsWith(targetLower)) {
                return new RecipientResult(
                    entry.getKey(),
                    playerRef,
                    playerRef.getUsername(),
                    PrivateMessageLogger.LookupType.PLAYER_NAME
                );
            }
        }
        
        // Third, try Discord username lookup
        for (Map.Entry<String, PlayerRef> entry : onlinePlayers.entrySet()) {
            String uuid = entry.getKey();
            String discordUsername = plugin.getLinkManager().getDiscordUsername(uuid);
            
            if (discordUsername != null) {
                // Check exact match
                if (discordUsername.equalsIgnoreCase(targetName)) {
                    return new RecipientResult(
                        uuid,
                        entry.getValue(),
                        entry.getValue().getUsername() + " (" + discordUsername + ")",
                        PrivateMessageLogger.LookupType.DISCORD_NAME
                    );
                }
                // Check partial match (username without discriminator)
                if (discordUsername.toLowerCase().startsWith(targetLower)) {
                    return new RecipientResult(
                        uuid,
                        entry.getValue(),
                        entry.getValue().getUsername() + " (" + discordUsername + ")",
                        PrivateMessageLogger.LookupType.DISCORD_NAME
                    );
                }
            }
        }
        
        return null;
    }
    
    /**
     * Send the private message to the recipient
     */
    private void sendPrivateMessage(Player sender, PlayerRef senderRef, RecipientResult recipient, String message) {
        String senderName = sender.getDisplayName();
        String senderUuid = senderRef.getUuid().toString();
        
        // Format the message for the recipient
        String lookupIndicator = recipient.lookupType == PrivateMessageLogger.LookupType.DISCORD_NAME ? " (Discord)" : "";
        String formattedMessage = "[PM] From " + senderName + lookupIndicator + ": " + message;
        
        // Send to recipient via PlayerRef
        plugin.sendMessageToPlayer(recipient.uuid, formattedMessage);
        
        // Log the message
        PrivateMessageLogger logger = plugin.getPrivateMessageLogger();
        if (logger != null) {
            logger.logMessage(
                senderName,
                senderUuid,
                recipient.displayName,
                recipient.uuid,
                message,
                recipient.lookupType
            );
        }
    }
    
    /**
     * Result of finding a recipient
     */
    private static class RecipientResult {
        final String uuid;
        @SuppressWarnings("unused")
        final PlayerRef playerRef;
        final String displayName;
        final PrivateMessageLogger.LookupType lookupType;
        
        RecipientResult(String uuid, PlayerRef playerRef, String displayName, 
                       PrivateMessageLogger.LookupType lookupType) {
            this.uuid = uuid;
            this.playerRef = playerRef;
            this.displayName = displayName;
            this.lookupType = lookupType;
        }
    }
}
