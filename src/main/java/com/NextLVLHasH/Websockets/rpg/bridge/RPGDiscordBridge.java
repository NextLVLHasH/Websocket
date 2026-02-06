package com.NextLVLHasH.Websockets.rpg.bridge;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.combat.DamageType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * Bridge between the RPG system and Discord.
 * 
 * Sends RPG events and data to Discord channels:
 * - Level-up announcements
 * - Boss kills
 * - Death notifications
 * - Achievement unlocks
 * - Real-time stat updates
 */
public class RPGDiscordBridge {
    
    private static final Logger LOGGER = Logger.getLogger("RPGDiscordBridge");
    
    private JDA jda;
    private String rpgChannelId;
    private String levelUpChannelId;
    private String pvpChannelId;
    private boolean enabled = false;
    
    // Event thresholds
    private int minLevelForAnnouncement = 5;
    private long minXPForKillAnnouncement = 100;
    private boolean announcePvPKills = true;
    private boolean announceBossKills = true;
    private boolean announceLevelUps = true;
    private boolean announceDeaths = false; // Usually disabled to reduce spam
    
    // Callback for sending messages to Hytale
    private Consumer<String> hytaleMessageCallback;
    
    public RPGDiscordBridge() {
        // Default constructor - will be initialized later
    }
    
    /**
     * Initialize the bridge with JDA instance and channel IDs
     */
    public void initialize(JDA jda, String rpgChannelId, String levelUpChannelId, String pvpChannelId) {
        this.jda = jda;
        this.rpgChannelId = rpgChannelId;
        this.levelUpChannelId = levelUpChannelId != null ? levelUpChannelId : rpgChannelId;
        this.pvpChannelId = pvpChannelId != null ? pvpChannelId : rpgChannelId;
        this.enabled = jda != null && rpgChannelId != null && !rpgChannelId.isEmpty();
        
        if (enabled) {
            LOGGER.info("RPGDiscordBridge initialized - RPG channel: " + rpgChannelId);
        } else {
            LOGGER.warning("RPGDiscordBridge not fully configured - events will be logged only");
        }
    }
    
    /**
     * Set callback for sending messages back to Hytale
     */
    public void setHytaleMessageCallback(Consumer<String> callback) {
        this.hytaleMessageCallback = callback;
    }
    
    // ==================== Configuration ====================
    
    public void setMinLevelForAnnouncement(int level) {
        this.minLevelForAnnouncement = level;
    }
    
    public void setAnnounceLevelUps(boolean enabled) {
        this.announceLevelUps = enabled;
    }
    
    public void setAnnounceDeaths(boolean enabled) {
        this.announceDeaths = enabled;
    }
    
    public void setAnnouncePvPKills(boolean enabled) {
        this.announcePvPKills = enabled;
    }
    
    // ==================== Event Handlers ====================
    
    /**
     * Called when a player levels up
     */
    public void onPlayerLevelUp(UUID playerId, String playerName, int newLevel, PlayerRPGData rpgData) {
        if (!enabled || !announceLevelUps) return;
        
        // Only announce significant levels
        if (newLevel < minLevelForAnnouncement && newLevel % 5 != 0) {
            LOGGER.fine("Skipping level-up announcement for " + playerName + " (level " + newLevel + ")");
            return;
        }
        
        LOGGER.info("Broadcasting level-up: " + playerName + " reached level " + newLevel);
        
        // Build embed
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎉 Level Up!")
                .setDescription("**" + playerName + "** has reached **Level " + newLevel + "**!")
                .setColor(getLevelColor(newLevel))
                .setThumbnail(getClassIcon(rpgData.getSelectedClass()))
                .addField("Class", formatClass(rpgData.getSelectedClass()), true)
                .addField("Race", formatRace(rpgData.getSelectedRace()), true)
                .addField("Total XP", formatNumber(rpgData.getCurrentXP()), true)
                .setTimestamp(Instant.now())
                .setFooter("Hytale RPG System");
        
        // Add milestone achievements
        if (newLevel == 10 || newLevel == 25 || newLevel == 50 || newLevel == 100) {
            embed.addField("🏆 Milestone!", "Reached level " + newLevel + "!", false);
        }
        
        sendEmbed(levelUpChannelId, embed.build());
        
        // Broadcast to Hytale server chat
        if (hytaleMessageCallback != null) {
            hytaleMessageCallback.accept("[RPG] " + playerName + " reached Level " + newLevel + "!");
        }
    }
    
    /**
     * Called when a player kills an entity
     */
    public void onPlayerKill(UUID playerId, String playerName, String killedType, 
                            long xpGained, boolean leveledUp, PlayerRPGData rpgData) {
        if (!enabled) return;
        
        // Check if this is a PvP kill
        boolean isPvPKill = killedType.equalsIgnoreCase("player");
        boolean isBossKill = killedType.toLowerCase().contains("boss") || 
                            killedType.toLowerCase().contains("dragon") ||
                            killedType.toLowerCase().contains("chieftain") ||
                            killedType.toLowerCase().contains("queen");
        
        // Only announce significant kills
        if (!isPvPKill && !isBossKill && xpGained < minXPForKillAnnouncement) {
            return;
        }
        
        if (isPvPKill && announcePvPKills) {
            // PvP kill announcement
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("⚔️ PvP Kill!")
                    .setDescription("**" + playerName + "** defeated another player!")
                    .setColor(Color.RED)
                    .addField("XP Gained", "+" + formatNumber(xpGained), true)
                    .addField("Current Level", String.valueOf(rpgData.getLevel()), true)
                    .setTimestamp(Instant.now());
            
            sendEmbed(pvpChannelId, embed.build());
        } else if (isBossKill && announceBossKills) {
            // Boss kill announcement
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("👑 Boss Slain!")
                    .setDescription("**" + playerName + "** has defeated **" + formatMobName(killedType) + "**!")
                    .setColor(Color.ORANGE)
                    .addField("XP Gained", "+" + formatNumber(xpGained), true)
                    .addField("Class", formatClass(rpgData.getSelectedClass()), true)
                    .addField("Level", String.valueOf(rpgData.getLevel()), true)
                    .setTimestamp(Instant.now())
                    .setFooter("First to slay this boss? Legendary!");
            
            sendEmbed(rpgChannelId, embed.build());
            
            // Server-wide announcement
            if (hytaleMessageCallback != null) {
                hytaleMessageCallback.accept("[BOSS] " + playerName + " has slain " + formatMobName(killedType) + "!");
            }
        }
    }
    
    /**
     * Called when a player dies
     */
    public void onPlayerDeath(UUID playerId, String playerName, int level, String deathCause, long xpLost) {
        if (!enabled || !announceDeaths) return;
        
        // Only announce deaths for higher level players or significant XP loss
        if (level < 10 && xpLost < 100) return;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💀 Player Death")
                .setDescription("**" + playerName + "** has fallen!")
                .setColor(Color.DARK_GRAY)
                .addField("Level", String.valueOf(level), true)
                .addField("Cause", deathCause, true);
        
        if (xpLost > 0) {
            embed.addField("XP Lost", "-" + formatNumber(xpLost), true);
        }
        
        embed.setTimestamp(Instant.now());
        
        sendEmbed(rpgChannelId, embed.build());
    }
    
    /**
     * Called when a player takes significant damage
     */
    public void onPlayerDamaged(UUID playerId, String playerName, double damage, 
                               DamageType damageType, PlayerRPGData rpgData) {
        // Only log significant damage locally, don't spam Discord
        LOGGER.fine(String.format("Player %s took %.1f %s damage (HP: %.0f/%.0f)",
                playerName, damage, damageType.getDisplayName(),
                rpgData.getCurrentHealth(), rpgData.getMaxHealth()));
    }
    
    /**
     * Send player stats embed to Discord
     */
    public void sendPlayerStats(String channelId, UUID playerId, String playerName, PlayerRPGData rpgData) {
        if (!enabled) return;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 " + playerName + "'s Character")
                .setColor(getClassColor(rpgData.getSelectedClass()))
                .addField("Level", String.valueOf(rpgData.getLevel()), true)
                .addField("Class", formatClass(rpgData.getSelectedClass()), true)
                .addField("Race", formatRace(rpgData.getSelectedRace()), true)
                .addField("Profession", formatProfession(rpgData.getSelectedProfession()), true)
                .addField("XP", formatNumber(rpgData.getCurrentXP()) + " / " + formatNumber(rpgData.getXpToNextLevel()), true)
                .addField("Skill Points", String.valueOf(rpgData.getSkillPoints()), true)
                .addField("❤️ Health", formatStat(rpgData.getCurrentHealth(), rpgData.getMaxHealth()), true)
                .addField("💙 Mana", formatStat(rpgData.getCurrentMana(), rpgData.getMaxMana()), true)
                .addField("💚 Stamina", formatStat(rpgData.getCurrentStamina(), rpgData.getMaxStamina()), true);
        
        // Add attributes
        StringBuilder attrs = new StringBuilder();
        for (AttributeType type : AttributeType.values()) {
            int value = rpgData.getCurrentAttribute(type);
            String icon = getAttributeIcon(type);
            attrs.append(icon).append(" ").append(type.getDisplayName()).append(": **").append(value).append("**\n");
        }
        embed.addField("Attributes", attrs.toString(), false);
        
        embed.setTimestamp(Instant.now())
             .setFooter("Hytale RPG System");
        
        sendEmbed(channelId, embed.build());
    }
    
    /**
     * Send skill tree progress to Discord
     */
    public void sendSkillTreeProgress(String channelId, String playerName, String treeName, 
                                     int unlockedNodes, int totalNodes, int skillPoints) {
        if (!enabled) return;
        
        double progress = totalNodes > 0 ? (unlockedNodes * 100.0 / totalNodes) : 0;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌳 " + playerName + "'s Skill Tree: " + treeName)
                .setColor(Color.GREEN)
                .addField("Progress", String.format("%.1f%% (%d/%d nodes)", progress, unlockedNodes, totalNodes), true)
                .addField("Available Points", String.valueOf(skillPoints), true)
                .setTimestamp(Instant.now());
        
        sendEmbed(channelId, embed.build());
    }
    
    // ==================== Helper Methods ====================
    
    private void sendEmbed(String channelId, MessageEmbed embed) {
        if (jda == null || channelId == null) {
            LOGGER.fine("Would send embed: " + embed.getTitle());
            return;
        }
        
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(embed).queue(
                        success -> LOGGER.fine("Sent embed to Discord: " + embed.getTitle()),
                        error -> LOGGER.warning("Failed to send embed: " + error.getMessage())
                );
            } else {
                LOGGER.warning("Discord channel not found: " + channelId);
            }
        } catch (Exception e) {
            LOGGER.warning("Error sending Discord embed: " + e.getMessage());
        }
    }
    
    private Color getLevelColor(int level) {
        if (level >= 100) return new Color(255, 215, 0); // Gold
        if (level >= 75) return new Color(138, 43, 226); // Purple
        if (level >= 50) return new Color(255, 140, 0);  // Dark Orange
        if (level >= 25) return new Color(0, 191, 255);  // Deep Sky Blue
        if (level >= 10) return new Color(50, 205, 50);  // Lime Green
        return new Color(169, 169, 169); // Gray
    }
    
    private Color getClassColor(String className) {
        if (className == null) return Color.GRAY;
        return switch (className.toLowerCase()) {
            case "warrior" -> new Color(139, 69, 19);    // Brown
            case "mage" -> new Color(75, 0, 130);        // Indigo
            case "rogue" -> new Color(47, 79, 79);       // Dark Slate Gray
            case "ranger" -> new Color(34, 139, 34);     // Forest Green
            case "cleric" -> new Color(255, 215, 0);     // Gold
            case "paladin" -> new Color(255, 255, 224);  // Light Yellow
            default -> Color.GRAY;
        };
    }
    
    private String getClassIcon(String className) {
        // Could be URLs to class icons hosted somewhere
        return null;
    }
    
    private String formatClass(String className) {
        return className != null ? capitalize(className) : "None";
    }
    
    private String formatRace(String raceName) {
        return raceName != null ? capitalize(raceName) : "None";
    }
    
    private String formatProfession(String profName) {
        return profName != null ? capitalize(profName) : "None";
    }
    
    private String formatMobName(String mobType) {
        return capitalize(mobType.replace("_", " "));
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    private String formatStat(double current, double max) {
        return String.format("%.0f / %.0f", current, max);
    }
    
    private String getAttributeIcon(AttributeType type) {
        return switch (type) {
            case STRENGTH -> "💪";
            case DEXTERITY -> "🏃";
            case CONSTITUTION -> "❤️";
            case INTELLIGENCE -> "🧠";
            case WISDOM -> "📚";
            case CHARISMA -> "🗣️";
        };
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
