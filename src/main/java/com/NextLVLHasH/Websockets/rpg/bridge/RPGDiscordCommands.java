package com.NextLVLHasH.Websockets.rpg.bridge;

import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.character.Race;
import com.NextLVLHasH.Websockets.rpg.character.Profession;
import com.NextLVLHasH.Websockets.rpg.character.CharacterManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillTree;
import com.NextLVLHasH.Websockets.rpg.skills.SkillNode;
import com.NextLVLHasH.Websockets.rpg.skills.Perk;
import com.NextLVLHasH.Websockets.rpg.skills.PerkManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillCategory;
import com.NextLVLHasH.Websockets.LinkVerificationManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.function.Function;

/**
 * Handles RPG-related Discord slash commands.
 * 
 * Commands:
 * - /rpg character [player] - View character stats
 * - /rpg skills [player] - View skill progress
 * - /rpg classes - List available classes
 * - /rpg races - List available races
 * - /rpg leaderboard [type] - View leaderboards
 */
@SuppressWarnings("unused")
public class RPGDiscordCommands {
    
    private static final Logger LOGGER = Logger.getLogger("RPGDiscordCommands");
    
    private final RPGManager rpgManager;
    private LinkVerificationManager verificationManager;
    private Function<String, UUID> playerNameResolver; // Resolves player name to UUID
    
    public RPGDiscordCommands(RPGManager rpgManager) {
        this.rpgManager = rpgManager;
    }
    
    public void setVerificationManager(LinkVerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }
    
    public void setPlayerNameResolver(Function<String, UUID> resolver) {
        this.playerNameResolver = resolver;
    }
    
    /**
     * Get the slash command definitions for RPG commands
     */
    public List<SlashCommandData> getCommandDefinitions() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        // Main /rpg command with subcommands
        SlashCommandData rpgCommand = Commands.slash("rpg", "RPG system commands")
                .addSubcommands(
                        new SubcommandData("character", "View your or another player's character")
                                .addOption(OptionType.STRING, "player", "Player name", false),
                        new SubcommandData("skills", "View skill tree progress")
                                .addOption(OptionType.STRING, "player", "Player name", false),
                        new SubcommandData("classes", "List available character classes"),
                        new SubcommandData("races", "List available races"),
                        new SubcommandData("professions", "List available professions"),
                        new SubcommandData("perks", "List all available perks")
                                .addOption(OptionType.STRING, "class", "Filter by class (warrior, mage, etc.)", false),
                        new SubcommandData("myperks", "View your unlocked perks")
                                .addOption(OptionType.STRING, "player", "Player name", false),
                        new SubcommandData("leaderboard", "View RPG leaderboards")
                                .addOption(OptionType.STRING, "type", "Leaderboard type (level, xp, kills)", false)
                );
        
        commands.add(rpgCommand);
        return commands;
    }
    
    /**
     * Handle RPG slash command
     */
    public void handleCommand(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.reply("Unknown RPG command").setEphemeral(true).queue();
            return;
        }
        
        event.deferReply().queue();
        
        try {
            switch (subcommand) {
                case "character" -> handleCharacterCommand(event);
                case "skills" -> handleSkillsCommand(event);
                case "classes" -> handleClassesCommand(event);
                case "races" -> handleRacesCommand(event);
                case "professions" -> handleProfessionsCommand(event);
                case "perks" -> handlePerksCommand(event);
                case "myperks" -> handleMyPerksCommand(event);
                case "leaderboard" -> handleLeaderboardCommand(event);
                default -> event.getHook().sendMessage("Unknown subcommand: " + subcommand).queue();
            }
        } catch (Exception e) {
            LOGGER.warning("Error handling RPG command: " + e.getMessage());
            event.getHook().sendMessage("❌ An error occurred: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Handle /rpg character command
     */
    private void handleCharacterCommand(SlashCommandInteractionEvent event) {
        PlayerRPGData rpgData = resolvePlayerData(event);
        
        if (rpgData == null) {
            sendNoDataError(event);
            return;
        }
        
        String playerName = rpgData.getPlayerName();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚔️ " + playerName + "'s Character")
                .setColor(getClassColor(rpgData.getSelectedClass()))
                .addField("Level", String.valueOf(rpgData.getLevel()), true)
                .addField("Class", formatValue(rpgData.getSelectedClass()), true)
                .addField("Race", formatValue(rpgData.getSelectedRace()), true)
                .addField("Profession", formatValue(rpgData.getSelectedProfession()), true)
                .addField("XP Progress", formatXP(rpgData), true)
                .addField("Skill Points", String.valueOf(rpgData.getSkillPoints()), true)
                .addField("❤️ Health", formatStat(rpgData.getCurrentHealth(), rpgData.getMaxHealth()), true)
                .addField("💙 Mana", formatStat(rpgData.getCurrentMana(), rpgData.getMaxMana()), true)
                .addField("💚 Stamina", formatStat(rpgData.getCurrentStamina(), rpgData.getMaxStamina()), true);
        
        // Add attributes
        StringBuilder attrs = new StringBuilder();
        for (AttributeType type : AttributeType.values()) {
            int baseVal = rpgData.getBaseAttribute(type);
            int currentVal = rpgData.getCurrentAttribute(type);
            String bonus = currentVal != baseVal ? " (+" + (currentVal - baseVal) + ")" : "";
            attrs.append(getAttributeIcon(type)).append(" ")
                 .append(type.getDisplayName()).append(": **")
                 .append(baseVal).append("**").append(bonus).append("\n");
        }
        embed.addField("📊 Attributes", attrs.toString(), false);
        
        // Add regeneration info
        embed.addField("Regeneration",
                String.format("❤️ %.1f/s | 💙 %.1f/s | 💚 %.1f/s",
                        rpgData.getHealthRegen(), rpgData.getManaRegen(), rpgData.getStaminaRegen()),
                false);
        
        embed.setTimestamp(Instant.now())
             .setFooter("Hytale RPG System");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg skills command
     */
    private void handleSkillsCommand(SlashCommandInteractionEvent event) {
        PlayerRPGData rpgData = resolvePlayerData(event);
        
        if (rpgData == null) {
            sendNoDataError(event);
            return;
        }
        
        String playerName = rpgData.getPlayerName();
        Set<String> unlockedSkills = rpgData.getUnlockedSkills();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌳 " + playerName + "'s Skills")
                .setColor(Color.GREEN)
                .addField("Skill Points Available", String.valueOf(rpgData.getSkillPoints()), true)
                .addField("Skills Unlocked", String.valueOf(unlockedSkills.size()), true);
        
        // Get skill trees for player's class
        String className = rpgData.getSelectedClass();
        if (className != null) {
            SkillTree tree = rpgManager.getSkillManager().getSkillTreeByClass(className);
            if (tree != null) {
                StringBuilder skillList = new StringBuilder();
                int count = 0;
                for (SkillNode node : tree.getAllNodes()) {
                    if (count >= 15) {
                        skillList.append("... and more");
                        break;
                    }
                    boolean unlocked = unlockedSkills.contains(node.getId());
                    String icon = unlocked ? "✅" : "🔒";
                    skillList.append(icon).append(" **").append(node.getName()).append("**")
                             .append(" (Lv.").append(node.getRequiredLevel()).append(")")
                             .append("\n");
                    count++;
                }
                embed.addField("Class Skills: " + capitalize(className), skillList.toString(), false);
            }
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Use skill points in-game to unlock skills!");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg classes command
     */
    private void handleClassesCommand(SlashCommandInteractionEvent event) {
        CharacterManager charManager = rpgManager.getCharacterManager();
        Collection<CharacterClass> classes = charManager.getAllClasses();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚔️ Available Character Classes")
                .setColor(Color.BLUE)
                .setDescription("Choose your path!");
        
        for (CharacterClass charClass : classes) {
            StringBuilder desc = new StringBuilder();
            desc.append("*").append(charClass.getDescription()).append("*\n");
            desc.append("Primary: **").append(charClass.getPrimaryAttribute()).append("**\n");
            desc.append("Secondary: **").append(charClass.getSecondaryAttribute()).append("**\n");
            
            // Show base stats
            desc.append("HP: ").append((int) charClass.getBaseHealth())
                .append(" | MP: ").append((int) charClass.getBaseMana())
                .append(" | SP: ").append((int) charClass.getBaseStamina());
            
            embed.addField(getClassEmoji(charClass.getId()) + " " + charClass.getDisplayName(), 
                          desc.toString(), false);
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Select a class when creating your character in-game");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg races command
     */
    private void handleRacesCommand(SlashCommandInteractionEvent event) {
        CharacterManager charManager = rpgManager.getCharacterManager();
        Collection<Race> races = charManager.getAllRaces();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🧬 Available Races")
                .setColor(Color.CYAN)
                .setDescription("Each race provides unique bonuses!");
        
        for (Race race : races) {
            StringBuilder desc = new StringBuilder();
            desc.append("*").append(race.getDescription()).append("*\n");
            
            // Show attribute bonuses
            if (!race.getRacialBonuses().isEmpty()) {
                desc.append("**Bonuses:** ");
                race.getRacialBonuses().forEach((attr, bonus) -> 
                        desc.append(attr.getDisplayName()).append(" +").append(bonus).append(", "));
                desc.setLength(desc.length() - 2); // Remove trailing comma
            }
            
            embed.addField(getRaceEmoji(race.getId()) + " " + race.getDisplayName(), 
                          desc.toString(), false);
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Select a race when creating your character in-game");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg professions command
     */
    private void handleProfessionsCommand(SlashCommandInteractionEvent event) {
        CharacterManager charManager = rpgManager.getCharacterManager();
        Collection<Profession> professions = charManager.getAllProfessions();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔧 Available Professions")
                .setColor(Color.ORANGE)
                .setDescription("Professions provide crafting and gathering bonuses!");
        
        for (Profession prof : professions) {
            StringBuilder desc = new StringBuilder();
            desc.append("*").append(prof.getDescription()).append("*\n");
            desc.append("**Type:** ").append(prof.getId().toUpperCase());
            
            embed.addField(getProfessionEmoji(prof.getId()) + " " + prof.getDisplayName(), 
                          desc.toString(), false);
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Select a profession when creating your character in-game");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg perks command - list all available perks
     */
    @SuppressWarnings("null")
    private void handlePerksCommand(SlashCommandInteractionEvent event) {
        PerkManager perkManager = PerkManager.getInstance();
        
        // Check for class filter
        String classFilter = event.getOption("class") != null 
                ? event.getOption("class").getAsString().toLowerCase() 
                : null;
        
        Collection<Perk> allPerks = perkManager.getAllPerks();
        
        // Filter by class if specified
        List<Perk> filteredPerks = allPerks.stream()
                .filter(p -> classFilter == null || 
                        (p.getRequiredClass() != null && p.getRequiredClass().equalsIgnoreCase(classFilter)))
                .sorted(Comparator.comparing(Perk::getRequiredClass, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(Perk::getTier)
                        .thenComparing(Perk::getId))
                .toList();
        
        if (filteredPerks.isEmpty()) {
            event.getHook().sendMessage("❌ No perks found" + 
                    (classFilter != null ? " for class: " + classFilter : "")).queue();
            return;
        }
        
        String title = classFilter != null 
                ? "⭐ " + capitalize(classFilter) + " Perks" 
                : "⭐ All Available Perks";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(new Color(255, 215, 0))
                .setDescription("Perks provide permanent bonuses when unlocked!");
        
        // Group perks by class for display
        Map<String, List<Perk>> perksByClass = new LinkedHashMap<>();
        for (Perk perk : filteredPerks) {
            String reqClass = perk.getRequiredClass() != null ? perk.getRequiredClass() : "Universal";
            perksByClass.computeIfAbsent(reqClass, k -> new ArrayList<>()).add(perk);
        }
        
        int fieldCount = 0;
        for (Map.Entry<String, List<Perk>> entry : perksByClass.entrySet()) {
            if (fieldCount >= 20) {
                embed.setFooter("Showing first 20 categories. Use /rpg perks class:<name> to filter.");
                break;
            }
            
            StringBuilder perksText = new StringBuilder();
            for (Perk perk : entry.getValue()) {
                if (perksText.length() > 900) {
                    perksText.append("... and more");
                    break;
                }
                String tierStars = "★".repeat(perk.getTier() + 1);
                perksText.append(tierStars).append(" **").append(perk.getDisplayName()).append("**")
                         .append(" (Lv.").append(perk.getRequiredLevel()).append(", ")
                         .append(perk.getSkillPointCost()).append("pts)\n")
                         .append("*").append(truncate(perk.getDescription(), 60)).append("*\n\n");
            }
            
            String className = entry.getKey().equals("Universal") ? "🌟 Universal" 
                    : getClassEmoji(entry.getKey()) + " " + capitalize(entry.getKey());
            embed.addField(className, perksText.toString(), false);
            fieldCount++;
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Total perks: " + filteredPerks.size() + " | Use perk points in-game to unlock!");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg myperks command - view player's unlocked perks
     */
    private void handleMyPerksCommand(SlashCommandInteractionEvent event) {
        PlayerRPGData rpgData = resolvePlayerData(event);
        
        if (rpgData == null) {
            sendNoDataError(event);
            return;
        }
        
        String playerName = rpgData.getPlayerName();
        UUID playerId = rpgData.getPlayerId();
        
        PerkManager perkManager = PerkManager.getInstance();
        Set<String> unlockedPerkIds = perkManager.getUnlockedPerks(playerId);
        List<Perk> unlockedPerks = perkManager.getUnlockedPerkObjects(playerId);
        int perkPoints = perkManager.getPerkPoints(playerId);
        
        // Get available perks (can be unlocked)
        List<Perk> availablePerks = perkManager.getAvailablePerks(playerId, rpgData);
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⭐ " + playerName + "'s Perks")
                .setColor(new Color(148, 0, 211))
                .addField("💎 Perk Points", String.valueOf(perkPoints), true)
                .addField("✅ Unlocked", String.valueOf(unlockedPerkIds.size()), true)
                .addField("🔓 Available", String.valueOf(availablePerks.size()), true);
        
        // Show unlocked perks
        if (!unlockedPerks.isEmpty()) {
            StringBuilder unlockedText = new StringBuilder();
            for (Perk perk : unlockedPerks) {
                if (unlockedText.length() > 900) {
                    unlockedText.append("... and ").append(unlockedPerks.size() - 15).append(" more");
                    break;
                }
                String tierStars = "★".repeat(perk.getTier() + 1);
                unlockedText.append(tierStars).append(" **").append(perk.getDisplayName()).append("**")
                           .append(" (").append(capitalize(perk.getRequiredClass())).append(")\n");
            }
            embed.addField("✅ Unlocked Perks", unlockedText.toString(), false);
        } else {
            embed.addField("✅ Unlocked Perks", "*No perks unlocked yet*", false);
        }
        
        // Show available perks (up to 5)
        if (!availablePerks.isEmpty()) {
            StringBuilder availableText = new StringBuilder();
            int count = 0;
            for (Perk perk : availablePerks) {
                if (count >= 5 || availableText.length() > 800) {
                    availableText.append("... and ").append(availablePerks.size() - count).append(" more available");
                    break;
                }
                availableText.append("🔓 **").append(perk.getDisplayName()).append("**")
                            .append(" (").append(perk.getSkillPointCost()).append("pts")
                            .append(", Lv.").append(perk.getRequiredLevel()).append(")\n");
                count++;
            }
            embed.addField("🔓 Ready to Unlock", availableText.toString(), false);
        }
        
        // Show active bonuses summary
        StringBuilder bonusText = new StringBuilder();
        Map<String, Double> totalModifiers = new HashMap<>();
        for (Perk perk : unlockedPerks) {
            for (Map.Entry<String, Double> mod : perk.getGameplayModifiers().entrySet()) {
                totalModifiers.merge(mod.getKey(), mod.getValue(), Double::sum);
            }
        }
        
        if (!totalModifiers.isEmpty()) {
            int count = 0;
            for (Map.Entry<String, Double> entry : totalModifiers.entrySet()) {
                if (count >= 10) break;
                String modName = formatModifierName(entry.getKey());
                double value = entry.getValue();
                String valueStr = value >= 1.0 ? String.format("+%.0f", value) 
                                               : String.format("+%.0f%%", value * 100);
                bonusText.append("• ").append(modName).append(": **").append(valueStr).append("**\n");
                count++;
            }
            embed.addField("📊 Active Bonuses", bonusText.toString(), false);
        }
        
        embed.setTimestamp(Instant.now())
             .setFooter("Use /rpg perks to see all available perks");
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Handle /rpg leaderboard command
     */
    private void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String type = event.getOption("type") != null 
                ? event.getOption("type").getAsString().toLowerCase() 
                : "level";
        
        // Get all player data and sort
        List<PlayerRPGData> allData = new ArrayList<>(rpgManager.getAllPlayerData().values());
        
        Comparator<PlayerRPGData> comparator = switch (type) {
            case "xp" -> Comparator.comparingLong(PlayerRPGData::getCurrentXP).reversed();
            case "health", "hp" -> Comparator.comparingDouble(PlayerRPGData::getMaxHealth).reversed();
            default -> Comparator.comparingInt(PlayerRPGData::getLevel)
                    .thenComparingLong(PlayerRPGData::getCurrentXP).reversed();
        };
        
        allData.sort(comparator);
        
        String title = switch (type) {
            case "xp" -> "🏆 XP Leaderboard";
            case "health", "hp" -> "❤️ Health Leaderboard";
            default -> "⭐ Level Leaderboard";
        };
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(Color.YELLOW);
        
        StringBuilder board = new StringBuilder();
        int rank = 1;
        for (PlayerRPGData data : allData) {
            if (rank > 10) break;
            
            String medal = switch (rank) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> String.valueOf(rank) + ".";
            };
            
            String value = switch (type) {
                case "xp" -> formatNumber(data.getCurrentXP()) + " XP";
                case "health", "hp" -> String.format("%.0f HP", data.getMaxHealth());
                default -> "Lv." + data.getLevel();
            };
            
            board.append(medal).append(" **").append(data.getPlayerName()).append("** - ")
                 .append(value).append("\n");
            rank++;
        }
        
        if (board.length() == 0) {
            board.append("No players found!");
        }
        
        embed.setDescription(board.toString());
        embed.setTimestamp(Instant.now())
             .setFooter("Total players: " + allData.size());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    // ==================== Helper Methods ====================
    
    private PlayerRPGData resolvePlayerData(SlashCommandInteractionEvent event) {
        var playerOption = event.getOption("player");
        
        if (playerOption == null) {
            // Try to get data for the Discord user
            String discordUserId = event.getUser().getId();
            
            if (verificationManager != null) {
                String hytaleUuid = verificationManager.getHytaleUuidByDiscordId(discordUserId);
                if (hytaleUuid != null) {
                    return rpgManager.getPlayerData(UUID.fromString(hytaleUuid));
                }
            }
            return null;
        }
        
        String playerName = playerOption.getAsString();
        
        // Search by player name
        for (PlayerRPGData data : rpgManager.getAllPlayerData().values()) {
            if (data.getPlayerName() != null && 
                data.getPlayerName().equalsIgnoreCase(playerName)) {
                return data;
            }
        }
        
        // Try name resolver if available
        if (playerNameResolver != null) {
            UUID uuid = playerNameResolver.apply(playerName);
            if (uuid != null) {
                return rpgManager.getPlayerData(uuid);
            }
        }
        
        return null;
    }
    
    private void sendNoDataError(SlashCommandInteractionEvent event) {
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("❌ No Character Found")
                        .setDescription("No RPG data found for this player.\n\n" +
                                "**Options:**\n" +
                                "• Link your account with `/link` in-game\n" +
                                "• Specify a player name: `/rpg character player:Name`\n" +
                                "• Create a character in-game first")
                        .setColor(Color.RED)
                        .build()
        ).queue();
    }
    
    private Color getClassColor(String className) {
        if (className == null) return Color.GRAY;
        return switch (className.toLowerCase()) {
            case "warrior" -> new Color(139, 69, 19);
            case "mage" -> new Color(75, 0, 130);
            case "rogue" -> new Color(47, 79, 79);
            case "ranger" -> new Color(34, 139, 34);
            case "cleric" -> new Color(255, 215, 0);
            case "paladin" -> new Color(255, 255, 224);
            default -> Color.GRAY;
        };
    }
    
    private String getClassEmoji(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> "⚔️";
            case "mage" -> "🔮";
            case "rogue" -> "🗡️";
            case "ranger" -> "🏹";
            case "cleric" -> "✨";
            case "paladin" -> "🛡️";
            default -> "⚡";
        };
    }
    
    private String getRaceEmoji(String raceId) {
        return switch (raceId.toLowerCase()) {
            case "human" -> "👤";
            case "elf" -> "🧝";
            case "dwarf" -> "⛏️";
            case "orc" -> "👹";
            case "feran" -> "🐺";
            default -> "🧬";
        };
    }
    
    private String getProfessionEmoji(String profId) {
        return switch (profId.toLowerCase()) {
            case "blacksmith" -> "🔨";
            case "alchemist" -> "⚗️";
            case "enchanter" -> "✨";
            case "miner" -> "⛏️";
            case "herbalist" -> "🌿";
            case "hunter" -> "🏹";
            default -> "🔧";
        };
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
    
    private String formatValue(String value) {
        return value != null ? capitalize(value) : "None";
    }
    
    private String formatStat(double current, double max) {
        return String.format("%.0f / %.0f", current, max);
    }
    
    private String formatXP(PlayerRPGData data) {
        long current = data.getCurrentXP();
        long next = data.getXpToNextLevel();
        double percent = next > 0 ? (current * 100.0 / next) : 100;
        return String.format("%s / %s (%.1f%%)", 
                formatNumber(current), formatNumber(next), percent);
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    private String formatModifierName(String modKey) {
        if (modKey == null) return "Unknown";
        // Convert snake_case to Title Case
        String[] parts = modKey.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(capitalize(part));
        }
        return result.toString();
    }
}
