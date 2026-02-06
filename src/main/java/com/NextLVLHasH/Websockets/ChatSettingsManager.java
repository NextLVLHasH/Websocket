package com.NextLVLHasH.Websockets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSettingsManager {

    public static class Settings {
        public String color = "#ffffff";
        public int opacityPercent = 100;
        public boolean hidden = false;
        // Party system settings
        public boolean partyHudEnabled = false;
        public boolean mapMarkersEnabled = true;
        public boolean pingBeaconEnabled = true;
        public boolean partyChatGuiEnabled = true;
        public boolean partyChatChatEnabled = true;
        public boolean invitePopupEnabled = true;
        public boolean pvpProtectionBetweenMembers = true;
        public boolean partyPersistence = true;

        // UI and language
        public String language = "EN"; // EN, DE, ES, BR, HU, RU, FR

        // Stats tracking
        public boolean trackKills = true;
        public boolean trackDamage = true;
        public boolean trackBlocks = false;
        public boolean trackDistance = true;
        public boolean trackTime = true;
        public boolean leaderboardEnabled = true;

        // Stats reset configuration (0 = disabled)
        public int statsResetDays = 0;
        public int statsResetMonths = 0;

        // UI helpers
        public boolean playerSearchInGui = true;

        // Invite behavior: "NORMAL", "AUTO_ACCEPT", "AUTO_DECLINE"
        public String inviteMode = "NORMAL";

        // Cooldowns (seconds)
        public int inviteCooldownSeconds = 30;
        public int teleportCooldownSeconds = 60;
        public int pingCooldownSeconds = 10;
    }

    private final Map<String, Settings> map = new ConcurrentHashMap<>();

    public Settings getSettings(String playerUuid) {
        return map.computeIfAbsent(playerUuid, k -> new Settings());
    }

    public void setSettings(String playerUuid, String color, int opacityPercent, boolean hidden) {
        Settings s = getSettings(playerUuid);
        s.color = color;
        s.opacityPercent = opacityPercent;
        s.hidden = hidden;
        map.put(playerUuid, s);
    }

    /**
     * Update extended settings in bulk. Any null parameter will leave the setting unchanged.
     */
    public void updateExtendedSettings(String playerUuid,
                                       Boolean partyHudEnabled,
                                       Boolean mapMarkersEnabled,
                                       Boolean pingBeaconEnabled,
                                       Boolean partyChatGuiEnabled,
                                       Boolean partyChatChatEnabled,
                                       Boolean invitePopupEnabled,
                                       Boolean pvpProtectionBetweenMembers,
                                       Boolean partyPersistence,
                                       String language,
                                       Boolean trackKills,
                                       Boolean trackDamage,
                                       Boolean trackBlocks,
                                       Boolean trackDistance,
                                       Boolean trackTime,
                                       Boolean leaderboardEnabled,
                                       Integer statsResetDays,
                                       Integer statsResetMonths,
                                       Boolean playerSearchInGui,
                                       String inviteMode,
                                       Integer inviteCooldownSeconds,
                                       Integer teleportCooldownSeconds,
                                       Integer pingCooldownSeconds) {
        Settings s = getSettings(playerUuid);
        if (partyHudEnabled != null) s.partyHudEnabled = partyHudEnabled;
        if (mapMarkersEnabled != null) s.mapMarkersEnabled = mapMarkersEnabled;
        if (pingBeaconEnabled != null) s.pingBeaconEnabled = pingBeaconEnabled;
        if (partyChatGuiEnabled != null) s.partyChatGuiEnabled = partyChatGuiEnabled;
        if (partyChatChatEnabled != null) s.partyChatChatEnabled = partyChatChatEnabled;
        if (invitePopupEnabled != null) s.invitePopupEnabled = invitePopupEnabled;
        if (pvpProtectionBetweenMembers != null) s.pvpProtectionBetweenMembers = pvpProtectionBetweenMembers;
        if (partyPersistence != null) s.partyPersistence = partyPersistence;
        if (language != null) s.language = language;
        if (trackKills != null) s.trackKills = trackKills;
        if (trackDamage != null) s.trackDamage = trackDamage;
        if (trackBlocks != null) s.trackBlocks = trackBlocks;
        if (trackDistance != null) s.trackDistance = trackDistance;
        if (trackTime != null) s.trackTime = trackTime;
        if (leaderboardEnabled != null) s.leaderboardEnabled = leaderboardEnabled;
        if (statsResetDays != null) s.statsResetDays = Math.max(0, statsResetDays);
        if (statsResetMonths != null) s.statsResetMonths = Math.max(0, statsResetMonths);
        if (playerSearchInGui != null) s.playerSearchInGui = playerSearchInGui;
        if (inviteMode != null) s.inviteMode = inviteMode;
        if (inviteCooldownSeconds != null) s.inviteCooldownSeconds = Math.max(0, inviteCooldownSeconds);
        if (teleportCooldownSeconds != null) s.teleportCooldownSeconds = Math.max(0, teleportCooldownSeconds);
        if (pingCooldownSeconds != null) s.pingCooldownSeconds = Math.max(0, pingCooldownSeconds);
        map.put(playerUuid, s);
    }

}
