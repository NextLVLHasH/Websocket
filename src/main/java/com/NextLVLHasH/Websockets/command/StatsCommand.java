package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.PlayerStatistics;
import com.NextLVLHasH.Websockets.PlayerStatisticsManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Command to view player statistics
 * /stats - View your own stats
 * /stats <player> - View another player's stats
 * /stats top - View top players by playtime
 */
public class StatsCommand extends CommandBase {
    
    private final WebsocketNotificationMod plugin;
    private static final Color HEADER_COLOR = Color.decode("#5865F2"); // Discord blurple
    private static final Color LABEL_COLOR = Color.decode("#99AAB5");  // Gray
    private static final Color VALUE_COLOR = Color.decode("#57F287");  // Green
    private static final Color GOLD_COLOR = Color.decode("#FFD700");   // Gold
    private static final Color SILVER_COLOR = Color.decode("#C0C0C0"); // Silver
    private static final Color BRONZE_COLOR = Color.decode("#CD7F32"); // Bronze
    
    // Define arguments
    private final OptionalArg<String> targetArg;

    public StatsCommand(WebsocketNotificationMod plugin) {
        super("stats", "websockets.commands.stats.desc");
        this.plugin = plugin;
        
        // Register optional target argument (player name or "top")
        this.targetArg = this.withOptionalArg(
            "player",
            "websockets.commands.stats.player.desc",
            ArgTypes.STRING
        );
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        // Check if sender is a player
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return;
        }
        
        Player player = context.senderAs(Player.class);
        @SuppressWarnings("removal")
        PlayerRef sender = player.getPlayerRef();
        
        PlayerStatisticsManager statsManager = plugin.getStatisticsManager();
        if (statsManager == null) {
            sender.sendMessage(Message.raw("Statistics tracking is not enabled."));
            return;
        }
        
        String senderUuid = sender.getUuid().toString();
        
        // Get optional argument
        String target = this.targetArg.get(context);
        
        if (target == null || target.isEmpty()) {
            // Show own stats
            showPlayerStats(sender, senderUuid, sender.getUsername());
        } else if (target.equalsIgnoreCase("top")) {
            // Show leaderboard
            showTopPlayers(sender, statsManager);
        } else {
            // Try to find player by name
            PlayerStatistics targetStats = findPlayerByName(statsManager, target);
            
            if (targetStats != null) {
                showPlayerStats(sender, targetStats.getPlayerUuid(), targetStats.getPlayerName());
            } else {
                sender.sendMessage(Message.raw("Player not found: " + target).color(Color.RED));
            }
        }
    }
    
    private void showPlayerStats(PlayerRef sender, String playerUuid, String playerName) {
        PlayerStatisticsManager statsManager = plugin.getStatisticsManager();
        PlayerStatistics stats = statsManager.getStats(playerUuid);
        
        if (stats == null) {
            sender.sendMessage(Message.raw("No statistics found for " + playerName).color(Color.RED));
            return;
        }
        
        // Build stats message
        Message header = Message.empty();
        header.insert(Message.raw("======= ").color(HEADER_COLOR));
        header.insert(Message.raw("Stats: " + playerName).color(GOLD_COLOR));
        header.insert(Message.raw(" =======").color(HEADER_COLOR));
        sender.sendMessage(header);
        
        // Playtime
        int rank = statsManager.getPlaytimeRank(playerUuid);
        String rankStr = rank > 0 ? " (#" + rank + ")" : "";
        sendStatLine(sender, "Playtime", stats.getFormattedPlaytime() + rankStr);
        
        // Current session
        if (stats.isInSession()) {
            sendStatLine(sender, "Current Session", stats.getFormattedSessionTime());
        }
        
        // Sessions
        sendStatLine(sender, "Total Sessions", String.valueOf(stats.getTotalSessions()));
        
        // Blocks broken
        sendStatLine(sender, "Blocks Broken", String.valueOf(stats.getBlocksBroken()));
        
        // Mobs killed
        sendStatLine(sender, "Mobs Killed", String.valueOf(stats.getMobsKilled()));
        
        // Messages sent
        sendStatLine(sender, "Messages Sent", String.valueOf(stats.getMessagesSent()));
        
        // First seen
        String firstSeen = formatTimestamp(stats.getFirstSeenTimestamp());
        sendStatLine(sender, "First Seen", firstSeen);
        
        // Last seen
        String lastSeen = stats.isInSession() ? "Online now" : formatTimestamp(stats.getLastSeenTimestamp());
        sendStatLine(sender, "Last Seen", lastSeen);
        
        // Footer
        sender.sendMessage(Message.raw("==========================").color(HEADER_COLOR));
    }
    
    private void showTopPlayers(PlayerRef sender, PlayerStatisticsManager statsManager) {
        List<PlayerStatistics> top = statsManager.getTopByPlaytime(10);
        
        if (top.isEmpty()) {
            sender.sendMessage(Message.raw("No player statistics recorded yet.").color(LABEL_COLOR));
            return;
        }
        
        // Header
        Message header = Message.empty();
        header.insert(Message.raw("======= ").color(HEADER_COLOR));
        header.insert(Message.raw("Top Players by Playtime").color(GOLD_COLOR));
        header.insert(Message.raw(" =======").color(HEADER_COLOR));
        sender.sendMessage(header);
        
        // List top players
        for (int i = 0; i < top.size(); i++) {
            PlayerStatistics stats = top.get(i);
            int rank = i + 1;
            
            Message line = Message.empty();
            
            // Rank with medal colors for top 3
            Color rankColor = switch (rank) {
                case 1 -> GOLD_COLOR;
                case 2 -> SILVER_COLOR;
                case 3 -> BRONZE_COLOR;
                default -> LABEL_COLOR;
            };
            
            String medal = switch (rank) {
                case 1 -> "#1 ";
                case 2 -> "#2 ";
                case 3 -> "#3 ";
                default -> "#" + rank + " ";
            };
            
            line.insert(Message.raw(medal).color(rankColor));
            line.insert(Message.raw(stats.getPlayerName()).color(Color.WHITE));
            line.insert(Message.raw(" - ").color(LABEL_COLOR));
            line.insert(Message.raw(stats.getFormattedPlaytime()).color(VALUE_COLOR));
            
            // Show if online
            if (stats.isInSession()) {
                line.insert(Message.raw(" (online)").color(Color.decode("#57F287")));
            }
            
            sender.sendMessage(line);
        }
        
        // Server totals
        sender.sendMessage(Message.raw(""));
        
        long totalPlaytime = statsManager.getTotalServerPlaytime();
        int uniquePlayers = statsManager.getUniquePlayerCount();
        
        sendStatLine(sender, "Total Server Playtime", PlayerStatistics.formatPlaytime(totalPlaytime));
        sendStatLine(sender, "Unique Players", String.valueOf(uniquePlayers));
        
        // Footer
        sender.sendMessage(Message.raw("==================================").color(HEADER_COLOR));
    }
    
    private void sendStatLine(PlayerRef sender, String label, String value) {
        Message line = Message.empty();
        line.insert(Message.raw("  " + label + ": ").color(LABEL_COLOR));
        line.insert(Message.raw(value).color(VALUE_COLOR));
        sender.sendMessage(line);
    }
    
    private String formatTimestamp(long epochSeconds) {
        if (epochSeconds == 0) return "Unknown";
        
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(epochSeconds), 
            ZoneId.systemDefault()
        );
        
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
    }
    
    private PlayerStatistics findPlayerByName(PlayerStatisticsManager statsManager, String name) {
        // Search through all stats for matching name (case-insensitive)
        for (PlayerStatistics stats : statsManager.getAllStats().values()) {
            if (stats.getPlayerName().equalsIgnoreCase(name)) {
                return stats;
            }
        }
        return null;
    }
}
