package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.skills.SkillTree;
import com.NextLVLHasH.Websockets.rpg.ui.CharacterSheetUI.CharacterSheetPage;
import com.NextLVLHasH.Websockets.rpg.ui.SkillTreeUI.SkillTreePage;
import com.NextLVLHasH.Websockets.rpg.ui.CharacterSelectionUI.CharacterSelectionPage;
import com.NextLVLHasH.Websockets.rpg.ui.CombatLogUI.CombatLogPage;
import com.NextLVLHasH.Websockets.rpg.ui.SkillTreeUI;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Main RPG command for accessing RPG system features.
 * Uses AbstractCommandCollection with addSubCommand() for proper registration.
 * Subcommands use AbstractPlayerCommand to get ECS context for opening UI pages.
 * 
 * Commands:
 *   /rpg - Show help
 *   /rpg sheet - Open character sheet UI
 *   /rpg skills - Open skill tree UI
 *   /rpg create - Open character creation UI
 *   /rpg log - Toggle combat log visibility
 *   /rpg info - Show current character stats in chat
 */
public class RPGCommand extends AbstractCommandCollection {

    public RPGCommand(com.NextLVLHasH.Websockets.WebsocketNotificationMod plugin) {
        super("rpg", "RPG system commands");
        
        // Register subcommands
        this.addSubCommand(new SheetCommand());
        this.addSubCommand(new SkillsCommand());
        this.addSubCommand(new CreateCommand());
        this.addSubCommand(new LogCommand());
        this.addSubCommand(new InfoCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
    
    // Helper to get player data safely
    protected static PlayerRPGData getPlayerData(UUID playerId) {
        return RPGManager.getInstance().getPlayerData(playerId);
    }

    /**
     * /rpg sheet - Open character sheet using AbstractPlayerCommand for ECS access
     */
    public static class SheetCommand extends AbstractPlayerCommand {
        
        public SheetCommand() {
            super("sheet", "Open character sheet UI");
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                              @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            
            PlayerRPGData data = getPlayerData(playerId);
            if (data == null) {
                context.sendMessage(Message.raw("You don't have a character yet! Use /rpg create"));
                return;
            }
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Error: Could not access player"));
                return;
            }
            
            CharacterSheetPage page = new CharacterSheetPage(playerRef, data);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    /**
     * /rpg skills - Open skill tree using AbstractPlayerCommand for ECS access
     */
    public static class SkillsCommand extends AbstractPlayerCommand {
        
        public SkillsCommand() {
            super("skills", "Open skill tree UI");
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                              @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            
            PlayerRPGData data = getPlayerData(playerId);
            if (data == null) {
                context.sendMessage(Message.raw("You don't have a character yet! Use /rpg create"));
                return;
            }
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Error: Could not access player"));
                return;
            }
            
            // Resolve the correct skill tree for the player's selected class
            String selectedClass = data.getSelectedClass();
            String treeId;
            if (selectedClass != null) {
                SkillTree classTree = com.NextLVLHasH.Websockets.rpg.skills.SkillManager.getInstance().getSkillTreeByClass(selectedClass);
                treeId = classTree != null ? classTree.getId() : selectedClass;
            } else {
                treeId = "default";
            }

            com.NextLVLHasH.Websockets.rpg.skills.SkillManager sm = com.NextLVLHasH.Websockets.rpg.skills.SkillManager.getInstance();
            SkillTree tree = sm.getSkillTree(treeId);
            if (tree == null) {
                context.sendMessage(Message.raw("No skill tree found: " + treeId));
                return;
            }

            SkillTreeUI ui = new SkillTreeUI(playerId, data);
            ui.attachTree(tree);
            SkillTreePage page = new SkillTreePage(ui, playerRef, tree, data);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    /**
     * /rpg create - Open character creation using AbstractPlayerCommand for ECS access
     */
    public static class CreateCommand extends AbstractPlayerCommand {
        
        public CreateCommand() {
            super("create", "Create a new character");
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                              @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            
            PlayerRPGData existingData = getPlayerData(playerId);
            // Only block if character has been properly created (class and race are set)
            if (existingData != null && existingData.isCharacterCreated() && 
                existingData.getSelectedClass() != null && existingData.getSelectedRace() != null) {
                context.sendMessage(Message.raw("You already have a character!"));
                context.sendMessage(Message.raw("Class: " + existingData.getSelectedClass() + 
                        " | Race: " + existingData.getSelectedRace() + 
                        " | Level: " + existingData.getLevel()));
                return;
            }
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Error: Could not access player"));
                return;
            }
            
            CharacterSelectionPage page = new CharacterSelectionPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    /**
     * /rpg log - Toggle combat log using AbstractPlayerCommand for ECS access
     */
    public static class LogCommand extends AbstractPlayerCommand {
        
        public LogCommand() {
            super("log", "Toggle combat log visibility");
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                              @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Error: Could not access player"));
                return;
            }
            
            PlayerRPGData data = getPlayerData(playerId);
            CombatLogPage page = new CombatLogPage(playerRef, data);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }

    /**
     * /rpg info - Show character info page (opens character sheet)
     */
    public static class InfoCommand extends AbstractPlayerCommand {
        
        public InfoCommand() {
            super("info", "Show character information");
        }
        
        @Override
        protected boolean canGeneratePermission() {
            return false;
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, 
                              @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            
            PlayerRPGData data = getPlayerData(playerId);
            if (data == null) {
                context.sendMessage(Message.raw("You don't have a character yet! Use /rpg create"));
                return;
            }
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                context.sendMessage(Message.raw("Error: Could not access player"));
                return;
            }
            
            // Open the character sheet as the info page
            CharacterSheetPage page = new CharacterSheetPage(playerRef, data);
            player.getPageManager().openCustomPage(ref, store, page);
        }
    }
}
