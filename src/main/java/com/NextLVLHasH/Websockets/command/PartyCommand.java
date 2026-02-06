package com.NextLVLHasH.Websockets.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.NextLVLHasH.Websockets.WebsocketNotificationMod;
import com.NextLVLHasH.Websockets.party.Party;
import com.NextLVLHasH.Websockets.party.PartyManager;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.Map;

public class PartyCommand extends AbstractCommandCollection {

    private final WebsocketNotificationMod plugin;

    public PartyCommand(WebsocketNotificationMod plugin) {
        super("group", "Group system commands");
        this.plugin = plugin;
        addSubCommand(new Create());
        addSubCommand(new Leave());
        addSubCommand(new Info());
        addSubCommand(new Invite());
        addSubCommand(new Accept());
        addSubCommand(new Decline());
    }

    public static class Create extends AbstractPlayerCommand {
        public Create() { super("create", "Create a party"); }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            PartyManager pm = PartyManager.getInstance();
            if (pm.getPartyByPlayer(playerId) != null) {
                context.sendMessage(Message.raw("You are already in a group."));
                return;
            }
            Party p = pm.createParty(playerId, playerRef.getUsername(), 8);
            if (p != null) context.sendMessage(Message.raw("Group created: " + p.getName()));
            else context.sendMessage(Message.raw("Failed to create group."));
        }
    }

    public static class Leave extends AbstractPlayerCommand {
        public Leave() { super("leave", "Leave your party"); }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            PartyManager pm = PartyManager.getInstance();
            boolean ok = pm.removeMember(playerId);
            if (ok) context.sendMessage(Message.raw("Left group."));
            else context.sendMessage(Message.raw("You are not in a group."));
        }
    }

    public static class Info extends AbstractPlayerCommand {
        public Info() { super("info", "Show party info"); }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            UUID playerId = playerRef.getUuid();
            PartyManager pm = PartyManager.getInstance();
            Party p = pm.getPartyByPlayer(playerId);
            if (p == null) { context.sendMessage(Message.raw("You are not in a group.")); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("Group: ").append(p.getName()).append(" (").append(p.getMembers().size()).append("/").append(p.getMaxSize()).append(")\n");
            sb.append("Leader: ").append(p.getLeader().toString()).append("\n");
            sb.append("Members: ");
            for (UUID m : p.getMembers()) sb.append(m.toString()).append(" ");
            context.sendMessage(Message.raw(sb.toString()));
        }
    }

    // Invite command implemented as CommandBase to support arguments
    public class Invite extends CommandBase {
        private final RequiredArg<String> targetArg;
        public Invite() {
            super("invite", "Invite a player to your party");
            this.targetArg = this.withRequiredArg("target", "Target player name", ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) { context.sendMessage(Message.raw("Only players can invite")); return; }
            Player sender = context.senderAs(Player.class);
            @SuppressWarnings("removal")
            PlayerRef senderRef = sender.getPlayerRef();
            UUID inviterId = senderRef.getUuid();
            String targetName = targetArg.get(context);
            if (targetName == null || targetName.isBlank()) { context.sendMessage(Message.raw("Usage: /group invite <player>")); return; }

            Map<String, PlayerRef> online = plugin.getOnlinePlayers();
            PlayerRef targetRef = null;
            String targetUuid = null;
            // Exact then partial match
            for (Map.Entry<String, PlayerRef> e : online.entrySet()) {
                if (e.getValue().getUsername().equalsIgnoreCase(targetName)) { targetRef = e.getValue(); targetUuid = e.getKey(); break; }
            }
            if (targetRef == null) {
                String low = targetName.toLowerCase();
                for (Map.Entry<String, PlayerRef> e : online.entrySet()) {
                    if (e.getValue().getUsername().toLowerCase().startsWith(low)) { targetRef = e.getValue(); targetUuid = e.getKey(); break; }
                }
            }

            if (targetRef == null) { context.sendMessage(Message.raw("Player not found online: " + targetName)); return; }

            PartyManager pm = PartyManager.getInstance();
            Party p = pm.getPartyByPlayer(inviterId);
                if (p == null) {
                    p = pm.createParty(inviterId, senderRef.getUsername(), 8);
                    context.sendMessage(Message.raw("Created group and inviting " + targetRef.getUsername()));
            }
            if (!p.getLeader().equals(inviterId)) { context.sendMessage(Message.raw("Only the party leader can invite.")); return; }

            boolean ok = pm.invitePlayer(p.getId(), inviterId, java.util.UUID.fromString(targetUuid));
              if (!ok) { context.sendMessage(Message.raw("Failed to send invite (already invited or player already in group).")); return; }

              context.sendMessage(Message.raw("Invite sent to " + targetRef.getUsername()));
              plugin.sendMessageToPlayer(targetUuid, "[Group] " + senderRef.getUsername() + " has invited you to join their group. Use /group accept to join or /group decline to decline.");
        }
    }

    public class Accept extends CommandBase {
        public Accept() { super("accept", "Accept a party invite"); }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override protected void executeSync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) { context.sendMessage(Message.raw("Only players can accept invites")); return; }
            Player sender = context.senderAs(Player.class);
            @SuppressWarnings("removal")
            PlayerRef senderRef = sender.getPlayerRef();
            UUID playerId = senderRef.getUuid();
            PartyManager pm = PartyManager.getInstance();
            if (!pm.hasPendingInvite(playerId)) { context.sendMessage(Message.raw("You have no pending invites.")); return; }
            boolean ok = pm.acceptInvite(playerId);
            if (ok) context.sendMessage(Message.raw("You have joined the party."));
            else context.sendMessage(Message.raw("Failed to accept invite."));
        }
    }

    public class Decline extends CommandBase {
        public Decline() { super("decline", "Decline a party invite"); }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override protected void executeSync(@Nonnull CommandContext context) {
            if (!context.isPlayer()) { context.sendMessage(Message.raw("Only players can decline invites")); return; }
            Player sender = context.senderAs(Player.class);
            @SuppressWarnings("removal")
            PlayerRef senderRef = sender.getPlayerRef();
            UUID playerId = senderRef.getUuid();
            PartyManager pm = PartyManager.getInstance();
            boolean ok = pm.declineInvite(playerId);
            if (ok) context.sendMessage(Message.raw("Invite declined."));
            else context.sendMessage(Message.raw("You have no pending invites."));
        }
    }
}
