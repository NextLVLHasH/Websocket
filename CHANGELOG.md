# Changelog

All notable changes to the **WebSocket & Discord Integration Mod** for Hytale.

---

## [2.0.0] - 2026-02-03 - **Full Discord Integration Release**

### 🎉 Major Features Added

#### Discord Bot (JDA Integration)
- **Full Discord Bot** - Replaced simple webhook with a complete Discord bot using JDA 5.0.0-beta.20
- **Rich Embed Notifications** - Server start/stop, player join/leave with colored embeds
- **Role Prefix Display** - Shows player's highest Discord role color as prefix in Hytale chat

#### Bidirectional Chat Bridge
- **Discord → Hytale** - Messages from Discord chat channel appear in-game
- **Hytale → Discord** - In-game chat messages appear in Discord
- **Linked Role Requirement** - Both directions require linked accounts for security
- **Role Prefixes** - Discord roles displayed with messages in both directions

#### Account Linking System
- **`/link <discord_username>`** - Start linking process (available to everyone)
- **`/unlink`** - Remove Discord link (available to everyone)
- **DM Verification Flow** - 6-digit code sent to Discord DM, player types in game chat
- **Automatic Linked Role** - Assigns configured role when account is verified
- **10-Minute Code Expiry** - Secure temporary verification codes

#### Button-Based Reaction Roles
- **Role Selection Buttons** - Clean button interface instead of reactions
- **Exclusive Roles** - Only one role at a time (automatically removes others)
- **In-Game Role Display** - Selected role shows as prefix in Hytale chat
- **Configurable Roles** - Set up multiple roles with custom emojis and colors

#### WebSocket Optimization
- **5-Minute Pulse System** - Efficient player count updates instead of per-event sends
- **Exponential Backoff Reconnection** - Smart reconnection on disconnect
- **Immediate Pulse on Events** - Instant updates for join/leave events

### 🔧 Technical Improvements

#### JDA Class Loading Fix
- Custom `IClassMap` implementation for Hytale's plugin class loader
- Proper CGLIB/ByteBuddy compatibility

#### Configuration Expansion
```json
{
  "enableDiscordBot": true,
  "discordBotToken": "YOUR_BOT_TOKEN",
  "discordNotificationChannelId": "CHANNEL_ID",
  "discordChatBridgeChannelId": "CHANNEL_ID",
  "discordLinkedRoleId": "ROLE_ID",
  "enableChatBridge": true,
  "syncHytaleToDiscord": true,
  "syncDiscordToHytale": true,
  "enableReactionRoles": true,
  "reactionRoleChannelId": "CHANNEL_ID",
  "reactionRoles": [
    {
      "emojiId": "⚔️",
      "discordRoleId": "ROLE_ID",
      "roleName": "Warrior",
      "roleColor": "#FF0000"
    }
  ]
}
```

#### Command System
- Proper Hytale `RequiredArg<String>` argument system
- `canGeneratePermission()` override for public commands
- Commands work for all players without admin permissions

---

## [1.5.0] - Player Count WebSocket

### Added
- **WebSocket Client** - Real-time player count to external services
- **Multiple Endpoint Support** - Connect to multiple WebSocket servers
- **JSON Message Format** - Structured player count data
- **Auto-Reconnection** - Handles connection drops gracefully

### Configuration
```json
{
  "enableWebsiteWebsocket": true,
  "websocketUrls": [
    "ws://localhost:8080/notifications",
    "ws://api.yoursite.com/hytale"
  ]
}
```

---

## [1.2.0] - Discord Webhook Embeds

### Added
- **Rich Discord Embeds** - Colored embeds for all notifications
- **Player Join** (🟢 Green) - Shows player name and timestamp
- **Player Leave** (🔴 Red) - Shows player name and timestamp  
- **Server Status** (🔵 Blue) - Periodic player count updates
- **Custom Bot Identity** - Configurable bot name and avatar

### Configuration
```json
{
  "enableDiscordWebhook": true,
  "discordWebhookUrl": "https://discord.com/api/webhooks/...",
  "discordBotName": "Hytale Server Bot",
  "discordAvatarUrl": "https://..."
}
```

---

## [1.1.0] - Event Notifications

### Added
- **Player Join Events** - Detect and notify on player join
- **Player Leave Events** - Detect and notify on player disconnect
- **Periodic Polling** - Configurable interval for status updates
- **Duplicate Prevention** - 2-second window to prevent spam

### Commands
- `/notificationstatus` - View current notification configuration
- `/notificationreload` - Reload config without server restart

---

## [1.0.0] - Initial Release

### Added
- **Basic Plugin Structure** - Hytale JavaPlugin implementation
- **Configuration System** - JSON-based config with auto-generation
- **Logging** - Comprehensive logging for debugging
- **Plugin Data Directory** - Proper file storage location

### Project Structure
```
WebsocketNotificationMod/
├── src/main/java/com/NextLVLHasH/Websockets/
│   ├── WebsocketNotificationMod.java    # Main plugin
│   ├── ConfigManager.java               # Config loading/saving
│   ├── NotificationConfig.java          # Config data class
│   └── command/
│       ├── NotificationReloadCommand.java
│       └── NotificationStatusCommand.java
└── src/main/Resources/
    └── manifest.json
```

---

## Feature Summary

| Feature | Version | Status |
|---------|---------|--------|
| Basic Plugin | 1.0.0 | ✅ |
| Event Notifications | 1.1.0 | ✅ |
| Discord Webhooks | 1.2.0 | ✅ |
| WebSocket Client | 1.5.0 | ✅ |
| Full Discord Bot | 2.0.0 | ✅ |
| Chat Bridge | 2.0.0 | ✅ |
| Account Linking | 2.0.0 | ✅ |
| Reaction Roles | 2.0.0 | ✅ |
| Role Prefixes | 2.0.0 | ✅ |

---

## Dependencies

### Current (v2.0.0)
- **JDA 5.0.0-beta.20** - Discord API
- **Hytale Server API** - Plugin framework
- **Java 21** - Runtime
- **Gradle 9.0** - Build system
- **Shadow Plugin 8.1.8** - Fat JAR packaging

### Build Command
```bash
./gradlew shadowJar
```

Output: `build/libs/WebsocketNotificationMod-1.0-all.jar`

---

## Migration Guide

### From 1.x to 2.0
1. Add Discord bot token to config (create bot at Discord Developer Portal)
2. Enable required Gateway Intents: `MESSAGE_CONTENT`, `GUILD_MEMBERS`, `GUILD_MESSAGE_REACTIONS`
3. Configure channel IDs for notifications and chat bridge
4. Set up linked role for chat bridge access
5. Optionally configure reaction roles

### Bot Permissions Required
- Send Messages
- Embed Links
- Manage Roles
- Add Reactions
- Read Message History
- View Channels
