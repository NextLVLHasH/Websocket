package com.NextLVLHasH.Websockets;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading and saving of notification configuration
 */
public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static final String CONFIG_FILE = "config.json";
    
    private final Gson gson;
    private final Path dataDirectory;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(new TypeToken<List<RoleReactionConfig>>(){}.getType(), new ReactionRoleListDeserializer())
                .create();
    }

    /**
     * Load configuration from file or create default
     */
    public NotificationConfig loadConfig() {
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            LOGGER.severe("Failed to create data directory: " + e.getMessage());
        }

        // If config doesn't exist, create default
        if (!Files.exists(configPath)) {
            LOGGER.info("Config file not found, creating default configuration...");
            NotificationConfig defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }

        // Load existing config
        try (Reader reader = new FileReader(configPath.toFile())) {
            NotificationConfig config = gson.fromJson(reader, NotificationConfig.class);
            
            // Validate reactionRoles list is not null
            if (config.getReactionRoles() == null) {
                config.setReactionRoles(new ArrayList<>());
            }
            
            LOGGER.info("Configuration loaded successfully");
            return config;
        } catch (JsonSyntaxException e) {
            LOGGER.severe("Failed to parse config file: " + e.getMessage());
            if (e.getMessage().contains("reactionRoles") && e.getMessage().contains("NUMBER")) {
                LOGGER.severe("ERROR: 'reactionRoles' in config.json is incorrect. It must be a list of objects (with emojiId, discordRoleId, etc), not just a list of numbers.");
            }
            LOGGER.info("Creating default configuration due to load error...");
            return createDefaultConfig();
        } catch (Exception e) {
            LOGGER.severe("Failed to load config: " + e.getMessage());
            LOGGER.info("Creating default configuration due to load error...");
            return createDefaultConfig();
        }
    }

    /**
     * Save configuration to file
     */
    public void saveConfig(NotificationConfig config) {
        Path configPath = dataDirectory.resolve(CONFIG_FILE);
        
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            LOGGER.severe("Failed to create data directory: " + e.getMessage());
            return;
        }

        try (Writer writer = new FileWriter(configPath.toFile())) {
            gson.toJson(config, writer);
            LOGGER.info("Configuration saved successfully");
        } catch (IOException e) {
            LOGGER.severe("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Create default configuration with example values
     */
    private NotificationConfig createDefaultConfig() {
        NotificationConfig config = new NotificationConfig();
        
        // Discord bot settings (disabled by default)
        config.setEnableDiscordBot(false);
        config.setDiscordBotToken("YOUR_BOT_TOKEN_HERE");
        config.setNotificationChannelId("YOUR_NOTIFICATION_CHANNEL_ID");
        config.setChatBridgeChannelId("YOUR_CHAT_BRIDGE_CHANNEL_ID");
        config.setLinkedRoleId("YOUR_LINKED_ROLE_ID");
        
        // Chat bridge settings
        config.setEnableChatBridge(true);
        config.setSyncHytaleToDiscord(true);
        config.setSyncDiscordToHytale(true);
        
        // Notification settings
        config.setEnablePlayerJoinNotifications(true);
        config.setEnablePlayerLeaveNotifications(true);
        
        // Server info
        config.setServerName("My Hytale Server");
        config.setServerAddress("play.myserver.com:5520");
        config.setServerDescription("A friendly Hytale server");
        
        // Initialize empty list for reaction roles to prevent null issues
        config.setReactionRoles(new ArrayList<>());
        
        return config;
    }

    /**
     * Reload configuration from disk
     */
    public NotificationConfig reloadConfig() {
        LOGGER.info("Reloading configuration...");
        return loadConfig();
    }

    /**
     * Get config file path for display
     */
    public String getConfigPath() {
        return dataDirectory.resolve(CONFIG_FILE).toString();
    }

    /**
     * Custom deserializer to handle both Object and Number/String for reaction roles
     * Allows users to provide just a list of Role IDs, assigning default emojis
     */
    private static class ReactionRoleListDeserializer implements JsonDeserializer<List<RoleReactionConfig>> {
        @Override
        public List<RoleReactionConfig> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<RoleReactionConfig> list = new ArrayList<>();
            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                int index = 0;
                // Default emojis to assign if user only provides IDs
                String[] defaultEmojis = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"};
                
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        list.add(context.deserialize(element, RoleReactionConfig.class));
                    } else if (element.isJsonPrimitive()) {
                        // Handle number/string as Role ID
                        String roleId = element.getAsString();
                        String emoji = (index < defaultEmojis.length) ? defaultEmojis[index] : "❓";
                        
                        RoleReactionConfig config = new RoleReactionConfig();
                        config.setDiscordRoleId(roleId);
                        config.setEmojiId(emoji);
                        // Use last 4 digits of ID for display name to keep it short
                        String shortId = roleId.length() > 4 ? roleId.substring(roleId.length() - 4) : roleId;
                        config.setRoleName("Role " + shortId);
                        config.setInGamePrefix("[Role]");
                        list.add(config);
                        index++;
                    }
                }
            }
            return list;
        }
    }
}
