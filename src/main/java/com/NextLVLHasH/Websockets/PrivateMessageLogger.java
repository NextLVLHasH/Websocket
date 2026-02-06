package com.NextLVLHasH.Websockets;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Logs private messages between players to files.
 * Messages are queued and written asynchronously to avoid blocking.
 */
public class PrivateMessageLogger {
    
    private static final Logger LOGGER = Logger.getLogger(PrivateMessageLogger.class.getName());
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter JSON_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path logDirectory;
    private final Path playerDataDirectory;
    private final ConcurrentLinkedQueue<LogEntry> pendingLogs = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor;
    
    public PrivateMessageLogger(Path dataDirectory) {
        this.logDirectory = dataDirectory.resolve("private_messages");
        this.playerDataDirectory = dataDirectory.resolve("player_data");
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(logDirectory);
            Files.createDirectories(playerDataDirectory);
        } catch (IOException e) {
            LOGGER.warning("Failed to create private message directories: " + e.getMessage());
        }
        
        // Start async log writer
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PrivateMessageLogger");
            t.setDaemon(true);
            return t;
        });
        
        // Flush logs every second
        executor.scheduleAtFixedRate(this::flushLogs, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Log a private message
     * @param senderName Sender's display name
     * @param senderUuid Sender's UUID
     * @param recipientName Recipient's display name (or Discord name if sent via Discord)
     * @param recipientUuid Recipient's UUID (may be null if Discord-only)
     * @param message The message content
     * @param lookupType How the recipient was found (PLAYER_NAME, DISCORD_NAME)
     */
    public void logMessage(String senderName, String senderUuid, 
                           String recipientName, String recipientUuid,
                           String message, LookupType lookupType) {
        LogEntry entry = new LogEntry(
            LocalDateTime.now(),
            senderName,
            senderUuid,
            recipientName,
            recipientUuid,
            message,
            lookupType
        );
        pendingLogs.add(entry);
    }
    
    private void flushLogs() {
        if (pendingLogs.isEmpty()) {
            return;
        }
        
        String today = LocalDateTime.now().format(FILE_DATE_FORMAT);
        Path logFile = logDirectory.resolve("pm_" + today + ".log");
        
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            LogEntry entry;
            while ((entry = pendingLogs.poll()) != null) {
                String line = formatLogEntry(entry);
                writer.write(line);
                writer.newLine();
                
                // Also save to player-specific JSON files
                saveToPlayerJson(entry);
            }
            
        } catch (IOException e) {
            LOGGER.warning("Failed to write private message log: " + e.getMessage());
        }
    }
    
    /**
     * Save message to both sender and recipient player JSON files
     */
    private void saveToPlayerJson(LogEntry entry) {
        // Save to sender's file
        saveMessageToPlayerFile(entry.senderUuid, entry, true);
        
        // Save to recipient's file (if they have a UUID)
        if (entry.recipientUuid != null) {
            saveMessageToPlayerFile(entry.recipientUuid, entry, false);
        }
    }
    
    /**
     * Save a message entry to a player's JSON file
     */
    private void saveMessageToPlayerFile(String playerUuid, LogEntry entry, boolean isSender) {
        Path playerFile = playerDataDirectory.resolve(playerUuid + ".json");
        
        try {
            // Load existing data
            PlayerData data;
            if (Files.exists(playerFile)) {
                String json = Files.readString(playerFile);
                data = GSON.fromJson(json, PlayerData.class);
                if (data == null) {
                    data = new PlayerData();
                }
            } else {
                data = new PlayerData();
            }
            
            // Add message
            MessageRecord record = new MessageRecord();
            record.timestamp = entry.timestamp.format(JSON_TIME_FORMAT);
            record.direction = isSender ? "sent" : "received";
            record.otherPlayer = isSender ? entry.recipientName : entry.senderName;
            record.otherUuid = isSender ? entry.recipientUuid : entry.senderUuid;
            record.message = entry.message;
            record.lookupType = entry.lookupType.toString();
            
            if (data.privateMessages == null) {
                data.privateMessages = new ArrayList<>();
            }
            data.privateMessages.add(record);
            
            // Save back to file
            String json = GSON.toJson(data);
            Files.writeString(playerFile, json);
            
        } catch (IOException e) {
            LOGGER.warning("Failed to save message to player JSON " + playerUuid + ": " + e.getMessage());
        }
    }
    
    private String formatLogEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.timestamp.format(LOG_TIME_FORMAT)).append("] ");
        sb.append(entry.senderName).append(" (").append(entry.senderUuid).append(") ");
        sb.append("-> ");
        sb.append(entry.recipientName);
        if (entry.recipientUuid != null) {
            sb.append(" (").append(entry.recipientUuid).append(")");
        }
        sb.append(" [").append(entry.lookupType).append("]: ");
        sb.append(entry.message);
        return sb.toString();
    }
    
    /**
     * Shutdown the logger, flushing any pending logs
     */
    public void shutdown() {
        flushLogs();
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public enum LookupType {
        PLAYER_NAME,    // Found by in-game player name
        DISCORD_NAME    // Found by Discord username
    }
    
    /**
     * Player data structure for JSON storage
     */
    public static class PlayerData {
        public String discordUsername;
        public String discordUserId;
        public String linkedAt;
        public List<MessageRecord> privateMessages;
    }
    
    /**
     * Private message record for JSON storage
     */
    public static class MessageRecord {
        public String timestamp;
        public String direction;  // "sent" or "received"
        public String otherPlayer;
        public String otherUuid;
        public String message;
        public String lookupType;
    }
    
    private static class LogEntry {
        final LocalDateTime timestamp;
        final String senderName;
        final String senderUuid;
        final String recipientName;
        final String recipientUuid;
        final String message;
        final LookupType lookupType;
        
        LogEntry(LocalDateTime timestamp, String senderName, String senderUuid,
                 String recipientName, String recipientUuid, String message, LookupType lookupType) {
            this.timestamp = timestamp;
            this.senderName = senderName;
            this.senderUuid = senderUuid;
            this.recipientName = recipientName;
            this.recipientUuid = recipientUuid;
            this.message = message;
            this.lookupType = lookupType;
        }
    }
}
