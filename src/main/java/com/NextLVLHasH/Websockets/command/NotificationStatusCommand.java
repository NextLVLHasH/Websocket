package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;

import javax.annotation.Nonnull;

/**
 * Command to view notification status
 * Usage: /notifications status
 */
public class NotificationStatusCommand extends CommandBase {
    
    private final WebsocketNotificationMod plugin;

    public NotificationStatusCommand(WebsocketNotificationMod plugin) {
        super("notificationstatus", "websockets.commands.status.desc");
        this.plugin = plugin;
    }

    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("=== Notification Status ==="));
        context.sendMessage(Message.raw("Server: " + plugin.getConfig().getServerName()));
        context.sendMessage(Message.raw(" "));
        
        // Discord bot status
        context.sendMessage(Message.raw("Discord Bot:"));
        context.sendMessage(Message.raw("  Enabled: " + 
            (plugin.getConfig().isEnableDiscordBot() ? "YES" : "NO")));
        if (plugin.getConfig().isEnableDiscordBot()) {
            context.sendMessage(Message.raw("  Connected: " + 
                (plugin.getDiscordBot() != null && plugin.getDiscordBot().isReady() ? "YES" : "NO")));
            context.sendMessage(Message.raw("  Chat Bridge: " + 
                (plugin.getConfig().isEnableChatBridge() ? "ENABLED" : "DISABLED")));
        }
        
        context.sendMessage(Message.raw(" "));
        
        // Notification settings
        context.sendMessage(Message.raw("Notifications:"));
        context.sendMessage(Message.raw("  Join: " + 
            (plugin.getConfig().isEnablePlayerJoinNotifications() ? "ENABLED" : "DISABLED")));
        context.sendMessage(Message.raw("  Leave: " + 
            (plugin.getConfig().isEnablePlayerLeaveNotifications() ? "ENABLED" : "DISABLED")));
    }
}
