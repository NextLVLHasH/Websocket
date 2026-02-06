package com.NextLVLHasH.Websockets.ui;

import com.NextLVLHasH.Websockets.party.Party;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lightweight Party HUD integration: sends simple party status messages to online members.
 * Uses suppliers to get online players map (string UUID -> PlayerRef).
 */
public class PartyHUD {
    private static Supplier<Map<String, PlayerRef>> onlinePlayersSupplier;

    public static void setOnlinePlayersSupplier(Supplier<Map<String, PlayerRef>> supplier) {
        onlinePlayersSupplier = supplier;
    }

    public static void updateParty(Party party) {
        if (party == null) return;
        if (onlinePlayersSupplier == null) return;
        Map<String, PlayerRef> online = onlinePlayersSupplier.get();
        if (online == null) return;

        String header = "[Group] " + party.getName() + " (" + party.getMembers().size() + "/" + party.getMaxSize() + ") Leader: " + party.getLeader();
        StringBuilder members = new StringBuilder();
        for (UUID m : party.getMembers()) {
            members.append(m.toString().substring(0, 8)).append(" ");
        }

        for (UUID m : party.getMembers()) {
            PlayerRef pr = online.get(m.toString());
            if (pr != null) {
                try {
                    pr.sendMessage(Message.raw(header));
                    pr.sendMessage(Message.raw("Members: " + members.toString() + " | Use /group info to view details"));
                } catch (Exception ignored) {}
            }
        }
    }
}
