# WebSocket & Discord Notification Mod for Hytale

A comprehensive Hytale server plugin that integrates Discord with your game server, providing real-time notifications, chat bridging, player linking, and role management.

---

## ✨ Features

### 🤖 Discord Bot Integration
- Full Discord bot with JDA (Java Discord API)
- Rich embedded notifications for player events
- Bi-directional chat bridge between Discord and Hytale
- Reaction-based role assignment system
- Discord account linking with verification

### 🔗 Player Account Linking
- Secure verification code system
- Link Hytale accounts to Discord accounts
- Automatic role assignment on link
- Display Discord names and roles in-game
- UI-based and command-based linking options

### 💬 Chat System
- **Chat Bridge**: Sync messages between Discord ↔ Hytale
- **Private Messages**: `/msg` command with Discord username lookup
- **Custom Chat UI**: In-game settings panel (`/chat`)
- **Chat Opacity**: Configurable transparency (0-100%)
- **Hide Chat**: Option to hide the native chat HUD

### 📊 WebSocket Integration
- Real-time player count updates
- JSON message format for web applications
- Configurable WebSocket server connection
- Player join/leave event streaming

### 🎭 Role Management
- Reaction role system in Discord
- Custom in-game prefixes per role
- Display Discord roles in Hytale chat
- Configurable role hierarchy

### 💾 Data Persistence
- Player settings saved to JSON files
- Discord links persisted across restarts
- Private message logging with history
- Per-player data files in `player_data/` directory

---

## 📦 Installation

### Prerequisites
- Hytale Server with plugin support
- Java 17 or higher
- Discord Bot Token (from [Discord Developer Portal](https://discord.com/developers/applications))

### Build & Install

1. **Clone the repository:**
   ```bash
   git clone https://github.com/NextLVLHasH/WebsocketNotificationMod.git
   cd WebsocketNotificationMod
   ```

2. **Build the mod:**
   ```bash
   # Windows
   gradlew.bat build
   
   # Linux/Mac
   ./gradlew build
   ```

3. **Copy to server:**
   ```bash
   cp build/libs/WebsocketNotificationMod-1.0-all.jar /path/to/hytale/server/plugins/
   ```

4. **Start the server** - A default `config.json` will be created

5. **Configure** - Edit `plugins/WebsocketNotificationMod/config.json`

6. **Restart or reload** - Use `/notificationreload` in-game

---

## ⚙️ Configuration

Configuration file location: `plugins/WebsocketNotificationMod/config.json`

### Full Configuration Example

```json
{
  "enableDiscordBot": true,
  "discordBotToken": "YOUR_BOT_TOKEN_HERE",
  "notificationChannelId": "1234567890123456789",
  "chatBridgeChannelId": "1234567890123456789",
  "linkedRoleId": "1234567890123456789",
  
  "enableReactionRoles": true,
  "reactionRoleChannelId": "1234567890123456789",
  "reactionRoleMessageTitle": "Get your in-game roles below!",
  "reactionRoleMessageDescription": "Click the reactions to get roles that will display in-game",
  "reactionRoles": [
    {
      "emojiId": "⚔️",
      "discordRoleId": "1234567890123456789",
      "roleName": "Knight",
      "inGamePrefix": "[Knight]"
    },
    {
      "emojiId": "🏹",
      "discordRoleId": "9876543210987654321",
      "roleName": "Archer",
      "inGamePrefix": "[Archer]"
    },
    {
      "emojiId": "🔮",
      "discordRoleId": "1111222233334444555",
      "roleName": "Mage",
      "inGamePrefix": "[Mage]"
    }
  ],
  
  "showDiscordNameInGame": true,
  "showRolePrefixInGame": true,
  
  "enableChatBridge": true,
  "syncHytaleToDiscord": true,
  "syncDiscordToHytale": true,
  
  "enablePlayerJoinNotifications": true,
  "enablePlayerLeaveNotifications": true,
  
  "serverName": "My Hytale Server",
  "serverAddress": "play.myserver.com:5520",
  "serverDescription": "A friendly Hytale server",
  
  "WSServerURL": "ws://localhost:8080"
}
```

### Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `enableDiscordBot` | boolean | Enable/disable the Discord bot |
| `discordBotToken` | string | Your Discord bot token |
| `notificationChannelId` | string | Channel for join/leave notifications |
| `chatBridgeChannelId` | string | Channel for chat bridge messages |
| `linkedRoleId` | string | Role to assign when player links account |
| `enableReactionRoles` | boolean | Enable reaction-based role assignment |
| `reactionRoleChannelId` | string | Channel for reaction role message |
| `showDiscordNameInGame` | boolean | Display Discord names in Hytale chat |
| `showRolePrefixInGame` | boolean | Display role prefixes in Hytale chat |
| `enableChatBridge` | boolean | Enable Discord ↔ Hytale chat sync |
| `syncHytaleToDiscord` | boolean | Send Hytale messages to Discord |
| `syncDiscordToHytale` | boolean | Send Discord messages to Hytale |
| `enablePlayerJoinNotifications` | boolean | Send join notifications |
| `enablePlayerLeaveNotifications` | boolean | Send leave notifications |
| `serverName` | string | Server name for embeds |
| `serverAddress` | string | Server address for embeds |
| `WSServerURL` | string | WebSocket server URL for player count |

---

## 🎮 Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/chat` | Open the chat settings UI panel |
| `/link <discord_username>` | Link your Hytale account to Discord |
| `/unlink` | Remove your Discord account link |
| `/msg <player\|discord_name> <message>` | Send a private message |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/notificationstatus` | View current notification system status |
| `/notificationreload` | Reload configuration without restart |

---

## 🔗 Discord Account Linking

### Method 1: In-Game Command
1. Type `/link YourDiscordUsername` in-game
2. Check your Discord DMs for a verification code
3. Type the 6-digit code in-game chat
4. You're linked! ✓

### Method 2: Chat Settings UI
1. Type `/chat` to open the settings panel
2. Enter your Discord username in the text field
3. Click the **LINK** button
4. Check your Discord DMs for a verification code
5. Type the 6-digit code in-game chat
6. You're linked! ✓

### After Linking
- Your Discord name appears in Hytale chat (if enabled)
- Your Discord roles show as prefixes (if enabled)
- You receive the "Linked" role in Discord
- Private messages can find you by Discord name

---

## 💬 Chat Bridge

When enabled, the chat bridge syncs messages between Discord and Hytale:

### Hytale → Discord
```
[Steve] Hello everyone!
```
Appears in Discord as an embedded message with the player's name.

### Discord → Hytale
```
[Discord] Username: Hello from Discord!
```
Appears in-game with a Discord prefix.

### Linked Players
If a Discord user is linked to a Hytale account:
```
[Archer] DiscordName (PlayerName): Hello!
```

---

## 🎭 Reaction Roles

Set up reaction-based role assignment:

1. Configure `reactionRoles` in config.json
2. Set `reactionRoleChannelId` to your desired channel
3. Reload config with `/notificationreload`
4. The bot posts a message with reaction buttons
5. Users click reactions to get/remove roles
6. Linked players see their role prefixes in-game

---

## 📁 Data Files

The plugin creates the following data structure:

```
plugins/WebsocketNotificationMod/
├── config.json           # Main configuration
├── discord_links.json    # All verified Discord links
├── player_data/          # Per-player data files
│   ├── <uuid>.json       # Player's Discord link + PM history
│   └── ...
└── private_messages/     # Daily PM logs
    ├── pm_2026-02-04.log
    └── ...
```

### Player Data JSON Format
```json
{
  "discordUsername": "User#1234",
  "discordUserId": "123456789012345678",
  "linkedAt": "2026-02-04 12:30:00",
  "privateMessages": [
    {
      "timestamp": "2026-02-04 12:35:00",
      "direction": "sent",
      "otherPlayer": "OtherPlayer",
      "otherUuid": "uuid-here",
      "message": "Hello!",
      "lookupType": "PLAYER_NAME"
    }
  ]
}
```

---

## 🔧 Discord Bot Setup

### Creating Your Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **New Application** and give it a name
3. Go to **Bot** tab → **Add Bot**
4. Copy the **Token** (keep this secret!)
5. Enable these **Privileged Gateway Intents**:
   - Presence Intent
   - Server Members Intent
   - Message Content Intent

### Inviting the Bot

1. Go to **OAuth2** → **URL Generator**
2. Select scopes: `bot`, `applications.commands`
3. Select permissions:
   - Send Messages
   - Embed Links
   - Add Reactions
   - Manage Roles (for reaction roles)
   - Read Message History
   - Use External Emojis
4. Copy the generated URL and open it in browser
5. Select your server and authorize

### Required Bot Permissions
- `VIEW_CHANNEL` - See channels
- `SEND_MESSAGES` - Send messages
- `EMBED_LINKS` - Send rich embeds
- `ADD_REACTIONS` - Add reaction role buttons
- `MANAGE_ROLES` - Assign roles to users
- `READ_MESSAGE_HISTORY` - For reaction roles

---

## 🎨 Chat Settings UI

Access with `/chat` command:

| Setting | Description |
|---------|-------------|
| **Chat Opacity** | Adjust chat transparency (0%, 20%, 40%, 60%, 80%, 100%) |
| **Hide Default Chat** | Toggle native chat HUD visibility |
| **Discord Link** | Link/view Discord account status |

---

## 📊 WebSocket Integration

The plugin can send player count data to a WebSocket server:

### Message Format
```json
{
  "type": "playerCount",
  "serverName": "My Server",
  "playerCount": 5,
  "playerNames": ["Steve", "Alex", "Player3"],
  "timestamp": 1706918400000
}
```

### Event Types
- `playerJoin` - When a player joins
- `playerLeave` - When a player leaves
- `playerCount` - Periodic status update

---

## 🔍 Troubleshooting

### Bot Not Responding
1. Check `discordBotToken` is correct
2. Verify bot has required intents enabled
3. Check bot is in the correct server
4. Review server console for errors

### Chat Bridge Not Working
1. Ensure `enableChatBridge` is `true`
2. Check `chatBridgeChannelId` is correct
3. Verify bot has permission to read/send in that channel

### Linking Not Working
1. Ensure player's Discord DMs are open
2. Check bot has permission to send DMs
3. Verify the Discord username is spelled correctly
4. Check if user is in the linked Discord server

### Reaction Roles Not Appearing
1. Ensure `enableReactionRoles` is `true`
2. Check `reactionRoleChannelId` is set
3. Verify bot has `MANAGE_ROLES` permission
4. Reload config with `/notificationreload`

### /chat Command Not Opening UI
1. Check server console for UI parse errors
2. Verify `chatsettings_test.ui` file exists and is valid
3. Ensure no syntax errors in the UI definition

---

## 📚 Requirements

- Hytale Server (January 2026+)
- Java 17+
- Gradle 8.5+ (for building)

---

## 📄 License

This project is proprietary software developed by NextLVLHasH.

---

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

---

## 📞 Support

- **Discord**: Join our Discord server for help
- **Issues**: Open a GitHub issue for bugs

---

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

---

**Made with ❤️ for the Hytale community by NextLVLHasH**
