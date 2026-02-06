package com.NextLVLHasH.Websockets.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import com.NextLVLHasH.Websockets.ui.PartyHUD;

public class PartyManager {
    private static PartyManager instance;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerToParty = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final File persistenceFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private PartyManager(File persistenceFile) {
        this.persistenceFile = persistenceFile;
        load();
    }

    public static synchronized PartyManager init(File persistenceFile) {
        if (instance == null) instance = new PartyManager(persistenceFile);
        return instance;
    }

    public static PartyManager getInstance() {
        if (instance == null) throw new IllegalStateException("PartyManager not initialized");
        return instance;
    }

    public synchronized Party createParty(UUID leader, String name, int maxSize) {
        UUID id = UUID.randomUUID();
        Party p = new Party(id, leader, name, maxSize);
        parties.put(id, p);
        playerToParty.put(leader, id);
        save();
        try { PartyHUD.updateParty(p); } catch (Exception ignored) {}
        return p;
    }

    /**
     * Create an invite for a target player to join the given party.
     * @return true if invite created, false if already invited or invalid
     */
    public synchronized boolean invitePlayer(UUID partyId, UUID inviter, UUID target) {
        Party p = parties.get(partyId);
        if (p == null) return false;
        if (!p.getMembers().contains(inviter)) return false;
        if (p.getMembers().contains(target)) return false;
        if (pendingInvites.containsKey(target)) return false;
        pendingInvites.put(target, partyId);
        return true;
    }

    public synchronized boolean hasPendingInvite(UUID target) {
        return pendingInvites.containsKey(target);
    }

    public synchronized boolean acceptInvite(UUID target) {
        UUID pid = pendingInvites.remove(target);
        if (pid == null) return false;
        return addMember(pid, target);
    }

    public synchronized boolean declineInvite(UUID target) {
        return pendingInvites.remove(target) != null;
    }

    public synchronized boolean addMember(UUID partyId, UUID player) {
        Party p = parties.get(partyId);
        if (p == null) return false;
        boolean ok = p.addMember(player);
        if (ok) playerToParty.put(player, partyId);
        save();
        try { PartyHUD.updateParty(p); } catch (Exception ignored) {}
        return ok;
    }

    public synchronized boolean removeMember(UUID player) {
        UUID pid = playerToParty.get(player);
        if (pid == null) return false;
        Party p = parties.get(pid);
        if (p == null) return false;
        boolean ok = p.removeMember(player);
        playerToParty.remove(player);
        if (p.getMembers().isEmpty()) {
            parties.remove(pid);
        }
        save();
        try { if (p != null) PartyHUD.updateParty(p); } catch (Exception ignored) {}
        return ok;
    }

    public synchronized Party getParty(UUID partyId) { return parties.get(partyId); }

    public synchronized Party getPartyByPlayer(UUID player) {
        UUID pid = playerToParty.get(player);
        if (pid == null) return null;
        return parties.get(pid);
    }

    public synchronized Collection<Party> getAllParties() { return parties.values(); }

    private synchronized void load() {
        try {
            if (!persistenceFile.exists()) return;
            Type listType = new TypeToken<List<Party>>(){}.getType();
            try (FileReader fr = new FileReader(persistenceFile)) {
                List<Party> loaded = gson.fromJson(fr, listType);
                if (loaded == null) return;
                parties.clear();
                playerToParty.clear();
                for (Party p : loaded) {
                    parties.put(p.getId(), p);
                    for (UUID m : p.getMembers()) playerToParty.put(m, p.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void save() {
        try {
            File parent = persistenceFile.getParentFile();
            if (!parent.exists()) parent.mkdirs();
            List<Party> toSave = new ArrayList<>(parties.values());
            try (FileWriter fw = new FileWriter(persistenceFile)) {
                gson.toJson(toSave, fw);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}