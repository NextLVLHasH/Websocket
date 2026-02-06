package com.NextLVLHasH.Websockets;

import com.hypixel.hytale.server.core.Message;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting and filtering chat messages
 * Handles Discord -> Hytale and Hytale -> Discord message conversion
 * Uses Hytale's Message API with java.awt.Color for text coloring
 */
public class MessageFormatter {
    
    // Regex patterns for filtering
    private static final Pattern DISCORD_MENTION_PATTERN = Pattern.compile("<@!?(\\d+)>");
    private static final Pattern DISCORD_ROLE_MENTION_PATTERN = Pattern.compile("<@&(\\d+)>");
    private static final Pattern DISCORD_CHANNEL_MENTION_PATTERN = Pattern.compile("<#(\\d+)>");
    private static final Pattern DISCORD_CUSTOM_EMOJI_PATTERN = Pattern.compile("<a?:(\\w+):(\\d+)>");
    private static final Pattern DISCORD_BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern DISCORD_ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*|_(.+?)_");
    private static final Pattern DISCORD_UNDERLINE_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern DISCORD_STRIKETHROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
    private static final Pattern DISCORD_CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern DISCORD_SPOILER_PATTERN = Pattern.compile("\\|\\|(.+?)\\|\\|");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
    
    private final NotificationConfig config;
    
    public MessageFormatter(NotificationConfig config) {
        this.config = config;
    }
    
    /**
     * Filter a Discord message for display in Hytale
     * @param message The raw Discord message
     * @return Filtered message safe for Hytale display
     */
    public String filterDiscordMessage(String message) {
        if (message == null) return "";
        
        String result = message;
        
        // Filter @mentions (converts <@123456> to @user)
        if (config.isFilterDiscordMentions()) {
            result = DISCORD_MENTION_PATTERN.matcher(result).replaceAll("@user");
            result = DISCORD_ROLE_MENTION_PATTERN.matcher(result).replaceAll("@role");
            result = DISCORD_CHANNEL_MENTION_PATTERN.matcher(result).replaceAll("#channel");
        }
        
        // Filter custom emojis (converts <:name:123> to :name:)
        if (config.isFilterCustomEmojis()) {
            result = DISCORD_CUSTOM_EMOJI_PATTERN.matcher(result).replaceAll(":$1:");
        }
        
        // Filter Discord formatting
        if (config.isFilterDiscordFormatting()) {
            result = DISCORD_BOLD_PATTERN.matcher(result).replaceAll("$1");
            result = DISCORD_UNDERLINE_PATTERN.matcher(result).replaceAll("$1");
            result = DISCORD_STRIKETHROUGH_PATTERN.matcher(result).replaceAll("$1");
            result = DISCORD_CODE_PATTERN.matcher(result).replaceAll("$1");
            result = DISCORD_SPOILER_PATTERN.matcher(result).replaceAll("[spoiler]");
            // Handle italic (which has two groups due to * or _)
            Matcher italicMatcher = DISCORD_ITALIC_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (italicMatcher.find()) {
                String replacement = italicMatcher.group(1) != null ? italicMatcher.group(1) : italicMatcher.group(2);
                italicMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            italicMatcher.appendTail(sb);
            result = sb.toString();
        }
        
        // Filter URLs
        if (config.isFilterUrls()) {
            result = URL_PATTERN.matcher(result).replaceAll("[link]");
        }
        
        return result.trim();
    }
    
    /**
     * Format a message for Discord display (Hytale -> Discord)
     * @param playerName Player name
     * @param playerUuid Player UUID (for avatar)
     * @param message The message content
     * @return Formatted message string
     */
    public String formatHytaleToDiscord(String playerName, String message) {
        String format = config.getHytaleToDiscordFormat();
        if (format == null || format.isEmpty()) {
            format = "**[Hytale]** `{player}`: {message}";
        }
        
        return format
            .replace("{player}", playerName)
            .replace("{message}", message);
    }
    
    /**
     * Format a message for Hytale display (Discord -> Hytale)
     * @param discordName Discord display name
     * @param rolePrefix Role prefix (e.g., "[Knight]")
     * @param roleColor Role color as hex (reserved for future use)
     * @param message The message content (already filtered)
     * @return Formatted message string
     */
    public String formatDiscordToHytale(String discordName, String rolePrefix, String roleColor, String message) {
        String format = config.getDiscordToHytaleFormat();
        if (format == null || format.isEmpty()) {
            format = "[Discord] {role}{name}: {message}";
        }
        
        String displayRole = (rolePrefix != null && !rolePrefix.isEmpty()) 
            ? rolePrefix + " " 
            : "";
        
        return format
            .replace("{role}", displayRole)
            .replace("{roleColor}", "") // Reserved for future Hytale color support
            .replace("{name}", discordName)
            .replace("{message}", message);
    }
    
    /**
     * Format an in-game chat message with role prefix
     * @param playerName Player display name
     * @param rolePrefix Role prefix (e.g., "[Knight]")
     * @param roleColor Role color as hex string (reserved for future use)
     * @param message The message content
     * @return Formatted message string
     */
    public String formatInGameChat(String playerName, String rolePrefix, String roleColor, String message) {
        String format = config.getInGameChatFormat();
        if (format == null || format.isEmpty()) {
            format = "{rolePrefix} {name}: {message}";
        }
        
        String displayPrefix = (rolePrefix != null && !rolePrefix.isEmpty()) ? rolePrefix : "";
        
        return format
            .replace("{roleColor}", "") // Reserved for future Hytale color support
            .replace("{rolePrefix}", displayPrefix)
            .replace("{name}", playerName)
            .replace("{message}", message);
    }
    
    /**
     * Get player avatar URL using the configured template
     * @param playerUuid Player's UUID
     * @param playerName Player's name (fallback)
     * @return Avatar URL string
     */
    public String getPlayerAvatarUrl(String playerUuid, String playerName) {
        String template = config.getPlayerAvatarUrlTemplate();
        if (template == null || template.isEmpty()) {
            template = "https://mc-heads.net/avatar/{uuid}/64";
        }
        
        // Clean UUID (remove dashes for some avatar services)
        String cleanUuid = playerUuid != null ? playerUuid.replace("-", "") : "";
        
        return template
            .replace("{uuid}", playerUuid != null ? playerUuid : "")
            .replace("{uuid_nodash}", cleanUuid)
            .replace("{name}", playerName != null ? playerName : "");
    }
    
    /**
     * Get hex color string (pass-through for future Hytale color support)
     * @param hexColor Hex color string (e.g., "#FF5555")
     * @return The hex color string as-is, or empty string if null
     */
    public static String getHexColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return "";
        }
        return hexColor;
    }
    
    /**
     * Convert Discord role color (integer) to hex string
     * @param colorInt Discord color as integer
     * @return Hex color string (e.g., "#FF5555")
     */
    public static String discordColorToHex(int colorInt) {
        if (colorInt == 0) return null; // No color set
        return String.format("#%06X", colorInt);
    }
    
    /**
     * Parse a hex color string to java.awt.Color
     * @param hexColor Hex color string (e.g., "#FF5555" or "FF5555")
     * @return Color object, or null if invalid
     */
    public static Color parseHexColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return null;
        }
        try {
            // Ensure it starts with #
            if (!hexColor.startsWith("#")) {
                hexColor = "#" + hexColor;
            }
            return Color.decode(hexColor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // Default colors for chat formatting
    public static final Color DEFAULT_DISCORD_LABEL_COLOR = Color.decode("#5865F2"); // Discord blurple
    public static final Color DEFAULT_ROLE_COLOR = Color.decode("#99AAB5"); // Discord gray
    public static final Color DEFAULT_CONTENT_COLOR = Color.WHITE;
    
    /**
     * Build a Hytale Message for Discord -> Hytale chat with proper colors
     * @param discordLabel The Discord label (e.g., "[Discord]")
     * @param labelColor Color for the label (hex string or null for default)
     * @param roleName Role name to display (e.g., "Knight")
     * @param roleColor Color for the role (hex string or null for default)
     * @param username Discord username
     * @param usernameColor Color for username (hex string or null for role color)
     * @param content Message content
     * @param contentColor Color for content (hex string or null for white)
     * @return A Hytale Message object with colored segments
     */
    public Message buildDiscordToHytaleMessage(
            String discordLabel,
            String labelColor,
            String roleName,
            String roleColor,
            String username,
            String usernameColor,
            String content,
            String contentColor) {
        
        Message root = Message.empty();
        
        // Parse colors with defaults
        Color labelClr = labelColor != null ? parseHexColor(labelColor) : DEFAULT_DISCORD_LABEL_COLOR;
        Color roleClr = roleColor != null ? parseHexColor(roleColor) : DEFAULT_ROLE_COLOR;
        @SuppressWarnings("unused")
        Color userClr = usernameColor != null ? parseHexColor(usernameColor) : roleClr; // Default to role color
        Color contentClr = contentColor != null ? parseHexColor(contentColor) : DEFAULT_CONTENT_COLOR;
        
        // Build message segments
        // [Discord]
        if (discordLabel != null && !discordLabel.isEmpty()) {
            root.insert(Message.raw(discordLabel + " ").color(labelClr != null ? labelClr : DEFAULT_DISCORD_LABEL_COLOR));
        }
        
        // [RoleName] if present - BOLD and COLORED with role color
        if (roleName != null && !roleName.isEmpty()) {
            root.insert(Message.raw("[" + roleName + "] ")
                .color(roleClr != null ? roleClr : DEFAULT_ROLE_COLOR)
                .bold(true));
        }
        
        // Username: - BOLD and WHITE
        if (username != null && !username.isEmpty()) {
            root.insert(Message.raw(username + ": ")
                .color(Color.WHITE)
                .bold(true));
        }
        
        // Message content
        if (content != null && !content.isEmpty()) {
            root.insert(Message.raw(content).color(contentClr != null ? contentClr : DEFAULT_CONTENT_COLOR));
        }
        
        return root;
    }
    
    /**
     * Build a Hytale Message for in-game chat with role prefix and colors
     * @param rolePrefix Role prefix (e.g., "[Knight]")
     * @param roleColor Color for the prefix (hex string or null)
     * @param playerName Player name
     * @param nameColor Color for player name (hex string or null for role color)
     * @param message Chat message
     * @param messageColor Color for message (hex string or null for white)
     * @return A Hytale Message object with colored segments
     */
    public Message buildInGameChatMessage(
            String rolePrefix,
            String roleColor,
            String playerName,
            String nameColor,
            String message,
            String messageColor) {
        
        Message root = Message.empty();
        
        // Parse colors with defaults
        Color roleClr = roleColor != null ? parseHexColor(roleColor) : null;
        Color nameClr = nameColor != null ? parseHexColor(nameColor) : roleClr; // Default to role color
        Color msgClr = messageColor != null ? parseHexColor(messageColor) : DEFAULT_CONTENT_COLOR;
        
        // [RolePrefix] if present and has color
        if (rolePrefix != null && !rolePrefix.isEmpty()) {
            if (roleClr != null) {
                root.insert(Message.raw(rolePrefix + " ").color(roleClr));
            } else {
                root.insert(Message.raw(rolePrefix + " "));
            }
        }
        
        // Player name
        if (playerName != null && !playerName.isEmpty()) {
            if (nameClr != null) {
                root.insert(Message.raw(playerName + ": ").color(nameClr));
            } else {
                root.insert(Message.raw(playerName + ": "));
            }
        }
        
        // Message content
        if (message != null && !message.isEmpty()) {
            if (msgClr != null) {
                root.insert(Message.raw(message).color(msgClr));
            } else {
                root.insert(Message.raw(message));
            }
        }
        
        return root;
    }
    
    /**
     * Build a simple colored message
     * @param text The text content
     * @param hexColor Hex color string (e.g., "#FF5555")
     * @return A Hytale Message object
     */
    public static Message buildColoredMessage(String text, String hexColor) {
        Color color = parseHexColor(hexColor);
        if (color != null) {
            return Message.raw(text).color(color);
        }
        return Message.raw(text);
    }
    
    /**
     * Build a message with a colored prefix
     * @param prefix The prefix text
     * @param prefixColor Hex color for prefix
     * @param content The main content
     * @param contentColor Hex color for content (null for default white)
     * @return A Hytale Message object
     */
    public static Message buildPrefixedMessage(String prefix, String prefixColor, String content, String contentColor) {
        Message root = Message.empty();
        
        Color prefixClr = parseHexColor(prefixColor);
        Color contentClr = contentColor != null ? parseHexColor(contentColor) : DEFAULT_CONTENT_COLOR;
        
        if (prefix != null && !prefix.isEmpty()) {
            if (prefixClr != null) {
                root.insert(Message.raw(prefix + " ").color(prefixClr));
            } else {
                root.insert(Message.raw(prefix + " "));
            }
        }
        
        if (content != null && !content.isEmpty()) {
            if (contentClr != null) {
                root.insert(Message.raw(content).color(contentClr));
            } else {
                root.insert(Message.raw(content));
            }
        }
        
        return root;
    }
}
