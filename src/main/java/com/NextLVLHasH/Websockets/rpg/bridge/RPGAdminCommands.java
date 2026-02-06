package com.NextLVLHasH.Websockets.rpg.bridge;

import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.character.CharacterClass;
import com.NextLVLHasH.Websockets.rpg.character.Race;
import com.NextLVLHasH.Websockets.rpg.character.Profession;
import com.NextLVLHasH.Websockets.rpg.character.CharacterManager;
import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.skills.Perk;
import com.NextLVLHasH.Websockets.rpg.skills.PerkManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillCategory;
import com.NextLVLHasH.Websockets.LinkVerificationManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Handles admin Discord slash commands for managing custom RPG content.
 * 
 * Commands:
 * - /rpgadmin class create - Create a new custom class
 * - /rpgadmin class edit - Edit an existing class
 * - /rpgadmin class delete - Delete a class
 * - /rpgadmin class list - List all classes
 * - /rpgadmin race create - Create a new custom race
 * - /rpgadmin race edit - Edit an existing race
 * - /rpgadmin race delete - Delete a race
 * - /rpgadmin race list - List all races
 * - /rpgadmin perk create - Create a new custom perk
 * - /rpgadmin perk edit - Edit an existing perk
 * - /rpgadmin perk delete - Delete a perk
 * - /rpgadmin perk list - List all perks
 * - /rpgadmin player give-perk - Give a perk to a player
 * - /rpgadmin player give-points - Give perk points to a player
 * - /rpgadmin reload - Reload configuration
 * 
 * All commands require ADMINISTRATOR permission.
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
@SuppressWarnings("unused")
public class RPGAdminCommands {
    
    private static final Logger LOGGER = Logger.getLogger("RPGAdminCommands");
    
    private final RPGManager rpgManager;
    private LinkVerificationManager verificationManager;
    
    // Admin role IDs that can use these commands (in addition to ADMINISTRATOR permission)
    private final Set<String> adminRoleIds = new HashSet<>();
    
    public RPGAdminCommands(RPGManager rpgManager) {
        this.rpgManager = rpgManager;
    }
    
    public void setVerificationManager(LinkVerificationManager verificationManager) {
        this.verificationManager = verificationManager;
    }
    
    public void addAdminRoleId(String roleId) {
        adminRoleIds.add(roleId);
    }
    
    public void removeAdminRoleId(String roleId) {
        adminRoleIds.remove(roleId);
    }
    
    /**
     * Get the slash command definitions for admin commands
     */
    public List<SlashCommandData> getCommandDefinitions() {
        List<SlashCommandData> commands = new ArrayList<>();
        
        // Main /rpgadmin command with subcommand groups
        SlashCommandData adminCommand = Commands.slash("rpgadmin", "RPG administration commands")
                // Class management group
                .addSubcommandGroups(
                        new SubcommandGroupData("class", "Manage character classes")
                                .addSubcommands(
                                        new SubcommandData("create", "Create a new custom class")
                                                .addOption(OptionType.STRING, "id", "Unique class ID (no spaces)", true)
                                                .addOption(OptionType.STRING, "name", "Display name", true)
                                                .addOption(OptionType.STRING, "description", "Class description", true)
                                                .addOption(OptionType.STRING, "primary", "Primary attribute (strength, dexterity, etc.)", true)
                                                .addOption(OptionType.STRING, "secondary", "Secondary attribute", false)
                                                .addOption(OptionType.INTEGER, "health", "Base health (default: 100)", false)
                                                .addOption(OptionType.INTEGER, "mana", "Base mana (default: 50)", false)
                                                .addOption(OptionType.INTEGER, "stamina", "Base stamina (default: 100)", false),
                                        new SubcommandData("edit", "Edit an existing class")
                                                .addOption(OptionType.STRING, "id", "Class ID to edit", true)
                                                .addOption(OptionType.STRING, "name", "New display name", false)
                                                .addOption(OptionType.STRING, "description", "New description", false)
                                                .addOption(OptionType.INTEGER, "health", "New base health", false)
                                                .addOption(OptionType.INTEGER, "mana", "New base mana", false)
                                                .addOption(OptionType.INTEGER, "stamina", "New base stamina", false),
                                        new SubcommandData("delete", "Delete a class")
                                                .addOption(OptionType.STRING, "id", "Class ID to delete", true),
                                        new SubcommandData("list", "List all classes with details")
                                ),
                        
                        // Race management group
                        new SubcommandGroupData("race", "Manage races")
                                .addSubcommands(
                                        new SubcommandData("create", "Create a new custom race")
                                                .addOption(OptionType.STRING, "id", "Unique race ID (no spaces)", true)
                                                .addOption(OptionType.STRING, "name", "Display name", true)
                                                .addOption(OptionType.STRING, "description", "Race description", true)
                                                .addOption(OptionType.STRING, "bonuses", "Attribute bonuses (e.g., strength:2,dexterity:1)", false),
                                        new SubcommandData("edit", "Edit an existing race")
                                                .addOption(OptionType.STRING, "id", "Race ID to edit", true)
                                                .addOption(OptionType.STRING, "name", "New display name", false)
                                                .addOption(OptionType.STRING, "description", "New description", false)
                                                .addOption(OptionType.STRING, "bonuses", "New attribute bonuses", false),
                                        new SubcommandData("delete", "Delete a race")
                                                .addOption(OptionType.STRING, "id", "Race ID to delete", true),
                                        new SubcommandData("list", "List all races with details")
                                ),
                        
                        // Perk management group
                        new SubcommandGroupData("perk", "Manage perks")
                                .addSubcommands(
                                        new SubcommandData("create", "Create a new custom perk")
                                                .addOption(OptionType.STRING, "id", "Unique perk ID (no spaces)", true)
                                                .addOption(OptionType.STRING, "name", "Display name", true)
                                                .addOption(OptionType.STRING, "description", "Perk description", true)
                                                .addOption(OptionType.STRING, "class", "Required class (or 'none' for universal)", true)
                                                .addOption(OptionType.INTEGER, "tier", "Perk tier (0-2)", true)
                                                .addOption(OptionType.INTEGER, "level", "Required level", true)
                                                .addOption(OptionType.INTEGER, "cost", "Perk point cost", true)
                                                .addOption(OptionType.STRING, "category", "Category (combat, utilities, magic)", false)
                                                .addOption(OptionType.STRING, "prereq", "Prerequisite perk ID", false),
                                        new SubcommandData("edit", "Edit an existing perk")
                                                .addOption(OptionType.STRING, "id", "Perk ID to edit", true)
                                                .addOption(OptionType.STRING, "name", "New display name", false)
                                                .addOption(OptionType.STRING, "description", "New description", false)
                                                .addOption(OptionType.INTEGER, "tier", "New tier", false)
                                                .addOption(OptionType.INTEGER, "level", "New required level", false)
                                                .addOption(OptionType.INTEGER, "cost", "New point cost", false),
                                        new SubcommandData("delete", "Delete a perk")
                                                .addOption(OptionType.STRING, "id", "Perk ID to delete", true),
                                        new SubcommandData("list", "List all perks with details")
                                                .addOption(OptionType.STRING, "class", "Filter by class", false)
                                ),
                        
                        // Player management group
                        new SubcommandGroupData("player", "Manage player RPG data")
                                .addSubcommands(
                                        new SubcommandData("give-perk", "Give a perk to a player")
                                                .addOption(OptionType.STRING, "player", "Player name", true)
                                                .addOption(OptionType.STRING, "perk", "Perk ID to give", true),
                                        new SubcommandData("give-points", "Give perk points to a player")
                                                .addOption(OptionType.STRING, "player", "Player name", true)
                                                .addOption(OptionType.INTEGER, "amount", "Points to give", true),
                                        new SubcommandData("set-class", "Set a player's class")
                                                .addOption(OptionType.STRING, "player", "Player name", true)
                                                .addOption(OptionType.STRING, "class", "Class ID", true),
                                        new SubcommandData("set-race", "Set a player's race")
                                                .addOption(OptionType.STRING, "player", "Player name", true)
                                                .addOption(OptionType.STRING, "race", "Race ID", true),
                                        new SubcommandData("info", "Get detailed player info")
                                                .addOption(OptionType.STRING, "player", "Player name", true)
                                )
                )
                // Reload command (standalone subcommand)
                .addSubcommands(
                        new SubcommandData("reload", "Reload all RPG configuration files")
                );
        
        commands.add(adminCommand);
        return commands;
    }
    
    /**
     * Handle admin slash command
     */
    public void handleCommand(SlashCommandInteractionEvent event) {
        // Check permissions
        if (!hasAdminPermission(event)) {
            event.reply("❌ You don't have permission to use admin commands.")
                    .setEphemeral(true).queue();
            return;
        }
        
        String subcommandGroup = event.getSubcommandGroup();
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            event.reply("Unknown admin command").setEphemeral(true).queue();
            return;
        }
        
        event.deferReply().queue();
        
        try {
            // Handle standalone reload command
            if (subcommandGroup == null && "reload".equals(subcommand)) {
                handleReloadCommand(event);
                return;
            }
            
            // Handle subcommand groups
            if (subcommandGroup != null) {
                switch (subcommandGroup) {
                    case "class" -> handleClassCommand(event, subcommand);
                    case "race" -> handleRaceCommand(event, subcommand);
                    case "perk" -> handlePerkCommand(event, subcommand);
                    case "player" -> handlePlayerCommand(event, subcommand);
                    default -> event.getHook().sendMessage("Unknown command group: " + subcommandGroup).queue();
                }
            } else {
                event.getHook().sendMessage("Unknown subcommand: " + subcommand).queue();
            }
        } catch (Exception e) {
            LOGGER.warning("Error handling admin command: " + e.getMessage());
            event.getHook().sendMessage("❌ An error occurred: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Check if user has admin permissions
     */
    @SuppressWarnings("null")
    private boolean hasAdminPermission(SlashCommandInteractionEvent event) {
        // Check for ADMINISTRATOR permission
        if (event.getMember() != null && event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        
        // Check for admin role
        if (event.getMember() != null && !adminRoleIds.isEmpty()) {
            return event.getMember().getRoles().stream()
                    .anyMatch(role -> adminRoleIds.contains(role.getId()));
        }
        
        return false;
    }
    
    // ==================== Class Commands ====================
    
    private void handleClassCommand(SlashCommandInteractionEvent event, String subcommand) {
        CharacterManager charManager = rpgManager.getCharacterManager();
        
        switch (subcommand) {
            case "create" -> handleClassCreate(event, charManager);
            case "edit" -> handleClassEdit(event, charManager);
            case "delete" -> handleClassDelete(event, charManager);
            case "list" -> handleClassList(event, charManager);
            default -> event.getHook().sendMessage("Unknown class command: " + subcommand).queue();
        }
    }
    
    private void handleClassCreate(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase().replaceAll("\\s+", "_");
        @SuppressWarnings("null")
        String name = event.getOption("name").getAsString();
        @SuppressWarnings("null")
        String description = event.getOption("description").getAsString();
        @SuppressWarnings("null")
        String primary = event.getOption("primary").getAsString().toUpperCase();
        
        // Check if class already exists
        if (charManager.getClass(id) != null) {
            event.getHook().sendMessage("❌ Class with ID '" + id + "' already exists.").queue();
            return;
        }
        
        // Parse attribute type
        AttributeType primaryAttr;
        try {
            primaryAttr = AttributeType.valueOf(primary);
        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ Invalid attribute: " + primary + 
                    "\nValid options: " + Arrays.toString(AttributeType.values())).queue();
            return;
        }
        
        // Get optional parameters
        @SuppressWarnings("null")
        String secondary = event.getOption("secondary") != null 
                ? event.getOption("secondary").getAsString().toUpperCase() : null;
        @SuppressWarnings("null")
        int health = event.getOption("health") != null 
                ? event.getOption("health").getAsInt() : 100;
        @SuppressWarnings("null")
        int mana = event.getOption("mana") != null 
                ? event.getOption("mana").getAsInt() : 50;
        @SuppressWarnings("null")
        int stamina = event.getOption("stamina") != null 
                ? event.getOption("stamina").getAsInt() : 100;
        
        // Build the class using the builder pattern
        CharacterClass.Builder builder = CharacterClass.builder(id)
                .displayName(name)
                .description(description)
                .baseAttribute(primaryAttr, 15)  // Primary attribute gets a bonus
                .baseHealth(health)
                .baseMana(mana)
                .baseStamina(stamina);
        
        // Add secondary attribute if specified
        if (secondary != null) {
            try {
                AttributeType secondaryAttr = AttributeType.valueOf(secondary);
                builder.baseAttribute(secondaryAttr, 12);  // Secondary attribute gets a smaller bonus
            } catch (IllegalArgumentException e) {
                // Ignore invalid secondary attribute
            }
        }
        
        CharacterClass newClass = builder.build();
        
        charManager.registerClass(newClass);
        charManager.saveClasses();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Class Created")
                .setColor(Color.GREEN)
                .addField("ID", id, true)
                .addField("Name", name, true)
                .addField("Primary", primaryAttr.getDisplayName(), true)
                .addField("Description", description, false)
                .addField("Stats", String.format("HP: %d | MP: %d | SP: %d", health, mana, stamina), false)
                .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " created class: " + id);
    }
    
    private void handleClassEdit(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        CharacterClass existing = charManager.getClass(id);
        
        if (existing == null) {
            event.getHook().sendMessage("❌ Class not found: " + id).queue();
            return;
        }
        
        // Get new values (use existing if not provided)
        @SuppressWarnings("null")
        String newName = event.getOption("name") != null 
                ? event.getOption("name").getAsString() : existing.getDisplayName();
        @SuppressWarnings("null")
        String newDesc = event.getOption("description") != null 
                ? event.getOption("description").getAsString() : existing.getDescription();
        @SuppressWarnings("null")
        int newHealth = event.getOption("health") != null 
                ? event.getOption("health").getAsInt() : (int) existing.getBaseHealth();
        @SuppressWarnings("null")
        int newMana = event.getOption("mana") != null 
                ? event.getOption("mana").getAsInt() : (int) existing.getBaseMana();
        @SuppressWarnings("null")
        int newStamina = event.getOption("stamina") != null 
                ? event.getOption("stamina").getAsInt() : (int) existing.getBaseStamina();
        
        // Create updated class using builder
        CharacterClass.Builder builder = CharacterClass.builder(id)
                .displayName(newName)
                .description(newDesc)
                .baseHealth(newHealth)
                .baseMana(newMana)
                .baseStamina(newStamina);
        
        // Copy existing attributes
        existing.getBaseAttributes().forEach(builder::baseAttribute);
        
        CharacterClass updatedClass = builder.build();
        
        charManager.registerClass(updatedClass);
        charManager.saveClasses();
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Class Updated")
                        .setColor(Color.ORANGE)
                        .addField("ID", id, true)
                        .addField("Name", newName, true)
                        .addField("Stats", String.format("HP: %d | MP: %d | SP: %d", newHealth, newMana, newStamina), false)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " edited class: " + id);
    }
    
    private void handleClassDelete(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        
        if (charManager.getClass(id) == null) {
            event.getHook().sendMessage("❌ Class not found: " + id).queue();
            return;
        }
        
        charManager.removeClass(id);
        charManager.saveClasses();
        
        event.getHook().sendMessage("✅ Class deleted: " + id).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " deleted class: " + id);
    }
    
    private void handleClassList(SlashCommandInteractionEvent event, CharacterManager charManager) {
        Collection<CharacterClass> classes = charManager.getAllClasses();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 All Character Classes")
                .setColor(Color.BLUE);
        
        if (classes.isEmpty()) {
            embed.setDescription("No classes registered.");
        } else {
            StringBuilder list = new StringBuilder();
            for (CharacterClass c : classes) {
                list.append("**").append(c.getId()).append("** - ")
                    .append(c.getDisplayName())
                    .append(" (").append(c.getPrimaryAttribute()).append(")\n")
                    .append("HP: ").append((int)c.getBaseHealth())
                    .append(" | MP: ").append((int)c.getBaseMana())
                    .append(" | SP: ").append((int)c.getBaseStamina()).append("\n\n");
            }
            embed.setDescription(list.toString());
        }
        
        embed.setFooter("Total: " + classes.size() + " classes")
             .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    // ==================== Race Commands ====================
    
    private void handleRaceCommand(SlashCommandInteractionEvent event, String subcommand) {
        CharacterManager charManager = rpgManager.getCharacterManager();
        
        switch (subcommand) {
            case "create" -> handleRaceCreate(event, charManager);
            case "edit" -> handleRaceEdit(event, charManager);
            case "delete" -> handleRaceDelete(event, charManager);
            case "list" -> handleRaceList(event, charManager);
            default -> event.getHook().sendMessage("Unknown race command: " + subcommand).queue();
        }
    }
    
    private void handleRaceCreate(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase().replaceAll("\\s+", "_");
        @SuppressWarnings("null")
        String name = event.getOption("name").getAsString();
        @SuppressWarnings("null")
        String description = event.getOption("description").getAsString();
        
        // Check if race already exists
        if (charManager.getRace(id) != null) {
            event.getHook().sendMessage("❌ Race with ID '" + id + "' already exists.").queue();
            return;
        }
        
        // Build the race using the builder pattern
        Race.Builder builder = Race.builder(id)
                .displayName(name)
                .description(description);
        
        // Parse bonuses
        if (event.getOption("bonuses") != null) {
            @SuppressWarnings("null")
            String bonusStr = event.getOption("bonuses").getAsString();
            for (String part : bonusStr.split(",")) {
                String[] kv = part.trim().split(":");
                if (kv.length == 2) {
                    try {
                        AttributeType attr = AttributeType.valueOf(kv[0].trim().toUpperCase());
                        int value = Integer.parseInt(kv[1].trim());
                        builder.racialBonus(attr, value);
                    } catch (Exception e) {
                        // Skip invalid bonus
                    }
                }
            }
        }
        
        Race newRace = builder.build();
        charManager.registerRace(newRace);
        charManager.saveRaces();
        
        StringBuilder bonusText = new StringBuilder();
        if (!newRace.getRacialBonuses().isEmpty()) {
            newRace.getRacialBonuses().forEach((attr, val) -> bonusText.append(attr.getDisplayName())
                    .append(": +").append(val).append("\n"));
        } else {
            bonusText.append("None");
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Race Created")
                .setColor(Color.GREEN)
                .addField("ID", id, true)
                .addField("Name", name, true)
                .addField("Description", description, false)
                .addField("Bonuses", bonusText.toString(), false)
                .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " created race: " + id);
    }
    
    private void handleRaceEdit(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        Race existing = charManager.getRace(id);
        
        if (existing == null) {
            event.getHook().sendMessage("❌ Race not found: " + id).queue();
            return;
        }
        
        @SuppressWarnings("null")
        String newName = event.getOption("name") != null 
                ? event.getOption("name").getAsString() : existing.getDisplayName();
        @SuppressWarnings("null")
        String newDesc = event.getOption("description") != null 
                ? event.getOption("description").getAsString() : existing.getDescription();
        
        // Build updated race using builder
        Race.Builder builder = Race.builder(id)
                .displayName(newName)
                .description(newDesc);
        
        // Parse new bonuses or keep existing
        if (event.getOption("bonuses") != null) {
            @SuppressWarnings("null")
            String bonusStr = event.getOption("bonuses").getAsString();
            for (String part : bonusStr.split(",")) {
                String[] kv = part.trim().split(":");
                if (kv.length == 2) {
                    try {
                        AttributeType attr = AttributeType.valueOf(kv[0].trim().toUpperCase());
                        int value = Integer.parseInt(kv[1].trim());
                        builder.racialBonus(attr, value);
                    } catch (Exception e) {
                        // Skip invalid bonus
                    }
                }
            }
        } else {
            builder.racialBonuses(existing.getRacialBonuses());
        }
        
        Race updatedRace = builder.build();
        charManager.registerRace(updatedRace);
        charManager.saveRaces();
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Race Updated")
                        .setColor(Color.ORANGE)
                        .addField("ID", id, true)
                        .addField("Name", newName, true)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " edited race: " + id);
    }
    
    private void handleRaceDelete(SlashCommandInteractionEvent event, CharacterManager charManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        
        if (charManager.getRace(id) == null) {
            event.getHook().sendMessage("❌ Race not found: " + id).queue();
            return;
        }
        
        charManager.removeRace(id);
        charManager.saveRaces();
        
        event.getHook().sendMessage("✅ Race deleted: " + id).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " deleted race: " + id);
    }
    
    private void handleRaceList(SlashCommandInteractionEvent event, CharacterManager charManager) {
        Collection<Race> races = charManager.getAllRaces();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 All Races")
                .setColor(Color.CYAN);
        
        if (races.isEmpty()) {
            embed.setDescription("No races registered.");
        } else {
            StringBuilder list = new StringBuilder();
            for (Race r : races) {
                list.append("**").append(r.getId()).append("** - ").append(r.getDisplayName()).append("\n");
                if (!r.getRacialBonuses().isEmpty()) {
                    r.getRacialBonuses().forEach((attr, val) -> 
                        list.append("  • ").append(attr.getDisplayName()).append(": +").append(val).append("\n"));
                }
                list.append("\n");
            }
            embed.setDescription(list.toString());
        }
        
        embed.setFooter("Total: " + races.size() + " races")
             .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    // ==================== Perk Commands ====================
    
    private void handlePerkCommand(SlashCommandInteractionEvent event, String subcommand) {
        PerkManager perkManager = PerkManager.getInstance();
        
        switch (subcommand) {
            case "create" -> handlePerkCreate(event, perkManager);
            case "edit" -> handlePerkEdit(event, perkManager);
            case "delete" -> handlePerkDelete(event, perkManager);
            case "list" -> handlePerkList(event, perkManager);
            default -> event.getHook().sendMessage("Unknown perk command: " + subcommand).queue();
        }
    }
    
    private void handlePerkCreate(SlashCommandInteractionEvent event, PerkManager perkManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase().replaceAll("\\s+", "_");
        @SuppressWarnings("null")
        String name = event.getOption("name").getAsString();
        @SuppressWarnings("null")
        String description = event.getOption("description").getAsString();
        @SuppressWarnings("null")
        String reqClass = event.getOption("class").getAsString().toLowerCase();
        @SuppressWarnings("null")
        int tier = event.getOption("tier").getAsInt();
        @SuppressWarnings("null")
        int level = event.getOption("level").getAsInt();
        @SuppressWarnings("null")
        int cost = event.getOption("cost").getAsInt();
        
        // Check if perk already exists
        if (perkManager.getPerk(id) != null) {
            event.getHook().sendMessage("❌ Perk with ID '" + id + "' already exists.").queue();
            return;
        }
        
        // Parse optional parameters
        @SuppressWarnings("null")
        String categoryStr = event.getOption("category") != null 
                ? event.getOption("category").getAsString().toUpperCase() : "COMBAT";
        @SuppressWarnings("null")
        String prereq = event.getOption("prereq") != null 
                ? event.getOption("prereq").getAsString() : null;
        
        SkillCategory category;
        try {
            category = SkillCategory.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            category = SkillCategory.COMBAT;
        }
        
        // Build the perk
        Perk.Builder builder = Perk.builder()
                .id(id)
                .displayName(name)
                .description(description)
                .category(category)
                .tier(tier)
                .requiredLevel(level)
                .skillPointCost(cost);
        
        if (!"none".equalsIgnoreCase(reqClass)) {
            builder.requiredClass(reqClass);
        }
        
        if (prereq != null && !prereq.isEmpty()) {
            builder.prerequisite(prereq);
        }
        
        Perk newPerk = builder.build();
        
        // Register the perk and save
        perkManager.registerPerk(newPerk);
        perkManager.savePerks();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Perk Created")
                .setColor(Color.GREEN)
                .addField("ID", id, true)
                .addField("Name", name, true)
                .addField("Class", reqClass.equals("none") ? "Universal" : reqClass, true)
                .addField("Tier", String.valueOf(tier), true)
                .addField("Level Req", String.valueOf(level), true)
                .addField("Cost", cost + " pts", true)
                .addField("Description", description, false)
                .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
        LOGGER.info("Admin " + event.getUser().getName() + " created perk: " + id);
    }
    
    private void handlePerkEdit(SlashCommandInteractionEvent event, PerkManager perkManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        Perk existing = perkManager.getPerk(id);
        
        if (existing == null) {
            event.getHook().sendMessage("❌ Perk not found: " + id).queue();
            return;
        }
        
        // Note: Full perk editing would require rebuilding the perk
        // For now, log the request and notify admin to edit the JSON directly
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("⚠️ Perk Edit")
                        .setColor(Color.YELLOW)
                        .setDescription("Perk editing via Discord is limited.\n\n" +
                                "For full control, edit the `config/perks.json` file directly\n" +
                                "and use `/rpgadmin reload` to apply changes.")
                        .addField("Current Perk", "**" + existing.getDisplayName() + "**\n" + 
                                existing.getDescription(), false)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
    
    private void handlePerkDelete(SlashCommandInteractionEvent event, PerkManager perkManager) {
        @SuppressWarnings("null")
        String id = event.getOption("id").getAsString().toLowerCase();
        
        if (perkManager.getPerk(id) == null) {
            event.getHook().sendMessage("❌ Perk not found: " + id).queue();
            return;
        }
        
        // Note: Need to add removePerk method to PerkManager
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("⚠️ Perk Deletion")
                        .setColor(Color.YELLOW)
                        .setDescription("To delete a perk, remove it from `config/perks.json`\n" +
                                "and use `/rpgadmin reload` to apply changes.")
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
    }
    
    private void handlePerkList(SlashCommandInteractionEvent event, PerkManager perkManager) {
        @SuppressWarnings("null")
        String classFilter = event.getOption("class") != null 
                ? event.getOption("class").getAsString().toLowerCase() : null;
        
        Collection<Perk> allPerks = perkManager.getAllPerks();
        List<Perk> perks = allPerks.stream()
                .filter(p -> classFilter == null || 
                        (p.getRequiredClass() != null && p.getRequiredClass().equalsIgnoreCase(classFilter)))
                .sorted(Comparator.comparing((Perk p) -> p.getRequiredClass() != null ? p.getRequiredClass() : "zzz")
                        .thenComparing(Perk::getTier)
                        .thenComparing(Perk::getId))
                .toList();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 All Perks" + (classFilter != null ? " (" + classFilter + ")" : ""))
                .setColor(new Color(255, 215, 0));
        
        if (perks.isEmpty()) {
            embed.setDescription("No perks found.");
        } else {
            StringBuilder list = new StringBuilder();
            int count = 0;
            for (Perk p : perks) {
                if (count >= 25 || list.length() > 3500) {
                    list.append("... and ").append(perks.size() - count).append(" more");
                    break;
                }
                String tierStars = "★".repeat(p.getTier() + 1);
                list.append(tierStars).append(" **").append(p.getId()).append("** - ")
                    .append(p.getDisplayName())
                    .append(" (").append(p.getRequiredClass() != null ? p.getRequiredClass() : "Universal")
                    .append(", Lv.").append(p.getRequiredLevel())
                    .append(", ").append(p.getSkillPointCost()).append("pts)\n");
                count++;
            }
            embed.setDescription(list.toString());
        }
        
        embed.setFooter("Total: " + perks.size() + " perks")
             .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    // ==================== Player Commands ====================
    
    private void handlePlayerCommand(SlashCommandInteractionEvent event, String subcommand) {
        switch (subcommand) {
            case "give-perk" -> handleGivePerk(event);
            case "give-points" -> handleGivePoints(event);
            case "set-class" -> handleSetClass(event);
            case "set-race" -> handleSetRace(event);
            case "info" -> handlePlayerInfo(event);
            default -> event.getHook().sendMessage("Unknown player command: " + subcommand).queue();
        }
    }
    
    private void handleGivePerk(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String playerName = event.getOption("player").getAsString();
        @SuppressWarnings("null")
        String perkId = event.getOption("perk").getAsString().toLowerCase();
        
        // Find player by name
        UUID playerId = findPlayerByName(playerName);
        if (playerId == null) {
            event.getHook().sendMessage("❌ Player not found: " + playerName).queue();
            return;
        }
        
        PerkManager perkManager = PerkManager.getInstance();
        Perk perk = perkManager.getPerk(perkId);
        
        if (perk == null) {
            event.getHook().sendMessage("❌ Perk not found: " + perkId).queue();
            return;
        }
        
        // Force unlock the perk
        perkManager.forceUnlockPerk(playerId, perkId);
        perkManager.savePlayerPerks(playerId);
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Perk Granted")
                        .setColor(Color.GREEN)
                        .addField("Player", playerName, true)
                        .addField("Perk", perk.getDisplayName(), true)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        
        LOGGER.info("Admin " + event.getUser().getName() + " gave perk " + perkId + " to " + playerName);
    }
    
    private void handleGivePoints(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String playerName = event.getOption("player").getAsString();
        @SuppressWarnings("null")
        int amount = event.getOption("amount").getAsInt();
        
        UUID playerId = findPlayerByName(playerName);
        if (playerId == null) {
            event.getHook().sendMessage("❌ Player not found: " + playerName).queue();
            return;
        }
        
        PerkManager perkManager = PerkManager.getInstance();
        perkManager.addPerkPoints(playerId, amount);
        int newTotal = perkManager.getPerkPoints(playerId);
        perkManager.savePlayerPerks(playerId);
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Perk Points Granted")
                        .setColor(Color.GREEN)
                        .addField("Player", playerName, true)
                        .addField("Added", "+" + amount, true)
                        .addField("New Total", String.valueOf(newTotal), true)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        
        LOGGER.info("Admin " + event.getUser().getName() + " gave " + amount + " perk points to " + playerName);
    }
    
    private void handleSetClass(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String playerName = event.getOption("player").getAsString();
        @SuppressWarnings("null")
        String classId = event.getOption("class").getAsString().toLowerCase();
        
        UUID playerId = findPlayerByName(playerName);
        if (playerId == null) {
            event.getHook().sendMessage("❌ Player not found: " + playerName).queue();
            return;
        }
        
        CharacterClass charClass = rpgManager.getCharacterManager().getClass(classId);
        if (charClass == null) {
            event.getHook().sendMessage("❌ Class not found: " + classId).queue();
            return;
        }
        
        var playerData = rpgManager.getPlayerData(playerId);
        if (playerData == null) {
            event.getHook().sendMessage("❌ No RPG data for player: " + playerName).queue();
            return;
        }
        
        playerData.setSelectedClass(classId);
        rpgManager.savePlayerData(playerId);
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Class Set")
                        .setColor(Color.GREEN)
                        .addField("Player", playerName, true)
                        .addField("Class", charClass.getDisplayName(), true)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        
        LOGGER.info("Admin " + event.getUser().getName() + " set class of " + playerName + " to " + classId);
    }
    
    private void handleSetRace(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String playerName = event.getOption("player").getAsString();
        @SuppressWarnings("null")
        String raceId = event.getOption("race").getAsString().toLowerCase();
        
        UUID playerId = findPlayerByName(playerName);
        if (playerId == null) {
            event.getHook().sendMessage("❌ Player not found: " + playerName).queue();
            return;
        }
        
        Race race = rpgManager.getCharacterManager().getRace(raceId);
        if (race == null) {
            event.getHook().sendMessage("❌ Race not found: " + raceId).queue();
            return;
        }
        
        var playerData = rpgManager.getPlayerData(playerId);
        if (playerData == null) {
            event.getHook().sendMessage("❌ No RPG data for player: " + playerName).queue();
            return;
        }
        
        playerData.setSelectedRace(raceId);
        rpgManager.savePlayerData(playerId);
        
        event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("✅ Race Set")
                        .setColor(Color.GREEN)
                        .addField("Player", playerName, true)
                        .addField("Race", race.getDisplayName(), true)
                        .setTimestamp(Instant.now())
                        .build()
        ).queue();
        
        LOGGER.info("Admin " + event.getUser().getName() + " set race of " + playerName + " to " + raceId);
    }
    
    private void handlePlayerInfo(SlashCommandInteractionEvent event) {
        @SuppressWarnings("null")
        String playerName = event.getOption("player").getAsString();
        
        UUID playerId = findPlayerByName(playerName);
        if (playerId == null) {
            event.getHook().sendMessage("❌ Player not found: " + playerName).queue();
            return;
        }
        
        var playerData = rpgManager.getPlayerData(playerId);
        if (playerData == null) {
            event.getHook().sendMessage("❌ No RPG data for player: " + playerName).queue();
            return;
        }
        
        PerkManager perkManager = PerkManager.getInstance();
        Set<String> unlockedPerks = perkManager.getUnlockedPerks(playerId);
        int perkPoints = perkManager.getPerkPoints(playerId);
        
        // Get Discord link info
        String discordInfo = "Not linked";
        if (verificationManager != null) {
            String discordId = verificationManager.getDiscordIdByHytaleUuid(playerId.toString());
            if (discordId != null) {
                discordInfo = "<@" + discordId + ">";
            }
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 " + playerName + " - Admin View")
                .setColor(Color.MAGENTA)
                .addField("UUID", playerId.toString(), false)
                .addField("Discord", discordInfo, true)
                .addField("Level", String.valueOf(playerData.getLevel()), true)
                .addField("XP", String.valueOf(playerData.getCurrentXP()), true)
                .addField("Class", playerData.getSelectedClass() != null ? playerData.getSelectedClass() : "None", true)
                .addField("Race", playerData.getSelectedRace() != null ? playerData.getSelectedRace() : "None", true)
                .addField("Profession", playerData.getSelectedProfession() != null ? playerData.getSelectedProfession() : "None", true)
                .addField("Perk Points", String.valueOf(perkPoints), true)
                .addField("Unlocked Perks", unlockedPerks.isEmpty() ? "None" : String.join(", ", unlockedPerks), false)
                .setTimestamp(Instant.now());
        
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    
    // ==================== Reload Command ====================
    
    private void handleReloadCommand(SlashCommandInteractionEvent event) {
        try {
            // Reload all managers
            rpgManager.getCharacterManager().reload();
            rpgManager.getSkillManager().reload();
            PerkManager.getInstance().reload();
            
            event.getHook().sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("✅ Configuration Reloaded")
                            .setColor(Color.GREEN)
                            .setDescription("All RPG configuration files have been reloaded:\n" +
                                    "• Classes\n• Races\n• Professions\n• Skills\n• Perks")
                            .setTimestamp(Instant.now())
                            .build()
            ).queue();
            
            LOGGER.info("Admin " + event.getUser().getName() + " reloaded RPG configuration");
            
        } catch (Exception e) {
            event.getHook().sendMessage("❌ Error reloading configuration: " + e.getMessage()).queue();
            LOGGER.warning("Error reloading configuration: " + e.getMessage());
        }
    }
    
    // ==================== Helper Methods ====================
    
    private UUID findPlayerByName(String playerName) {
        // Search in RPG data
        for (var entry : rpgManager.getAllPlayerData().entrySet()) {
            if (entry.getValue().getPlayerName() != null && 
                entry.getValue().getPlayerName().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
