package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.Message;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;

import javax.annotation.Nonnull;

/**
 * Command to reload the notification configuration
 * Usage: /notifications reload
 */
public class NotificationReloadCommand extends CommandBase {
    
    private final WebsocketNotificationMod plugin;

    public NotificationReloadCommand(WebsocketNotificationMod plugin) {
        super("notificationreload", "websockets.commands.reload.desc");
        this.plugin = plugin;
    }

    protected void executeSync(@Nonnull CommandContext context) {
        // Check permission
        if (!context.sender().hasPermission("websockets.admin.reload")) {
            context.sendMessage(Message.raw("You don't have permission to use this command!"));
            return;
        }

        try {
            // Reload configuration
            plugin.reloadConfiguration();
            
            context.sendMessage(Message.raw("Configuration reloaded successfully!"));
            context.sendMessage(Message.raw("Discord Bot: " + 
                (plugin.getConfig().isEnableDiscordBot() ? "ENABLED" : "DISABLED")));
            context.sendMessage(Message.raw("Note: Restart the server for changes to take full effect"));
            
        } catch (Exception e) {
            context.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()));
            plugin.getLogger().atSevere().log("Failed to reload config", e);
        }
    }
}
