package com.NextLVLHasH.Websockets.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    private final UUID id;
    private final UUID leader;
    private final String name;
    private final List<UUID> members = new ArrayList<>();
    private int maxSize = 8;

    public Party(UUID id, UUID leader, String name, int maxSize) {
        this.id = id;
        this.leader = leader;
        this.name = name;
        this.maxSize = Math.max(2, Math.min(maxSize, 16));
        this.members.add(leader);
    }

    public UUID getId() { return id; }
    public UUID getLeader() { return leader; }
    public String getName() { return name; }
    public List<UUID> getMembers() { return members; }
    public int getMaxSize() { return maxSize; }

    public boolean addMember(UUID player) {
        if (members.size() >= maxSize) return false;
        if (members.contains(player)) return false;
        return members.add(player);
    }

    public boolean removeMember(UUID player) {
        if (!members.contains(player)) return false;
        return members.remove(player);
    }
}
