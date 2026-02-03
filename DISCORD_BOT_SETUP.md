# Discord Bot Setup Guide

## Overview
This plugin is now a **full Discord bot** with advanced features:
- ✅ Rich embed notifications (join/leave/start/stop)
- 💬 Bidirectional chat bridge (Discord ↔ Hytale)
- 🎭 Role management and assignment
- 🔗 Player linking system
- 👥 Username management

---

## Step 1: Create Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"** and name it (e.g., "Hytale Server Bot")
3. Go to **"Bot"** tab and click **"Add Bot"**
4. Under **"Privileged Gateway Intents"**, enable:
   - ✅ Presence Intent
   - ✅ Server Members Intent
   - ✅ Message Content Intent
5. Click **"Reset Token"** and copy your bot token (keep it secret!)

---

## Step 2: Invite Bot to Your Server

1. Go to **"OAuth2" → "URL Generator"**
2. Select scopes:
   - ✅ `bot`
3. Select bot permissions:
   - ✅ Send Messages
   - ✅ Embed Links
   - ✅ Read Message History
   - ✅ Manage Roles
   - ✅ View Channels
4. Copy the generated URL and open it in your browser
5. Select your Discord server and authorize

---

## Step 3: Get Channel & Role IDs

### Enable Developer Mode:
1. Open Discord Settings → Advanced
2. Enable **"Developer Mode"**

### Get Channel IDs:
1. Right-click your notification channel → **"Copy Channel ID"**
2. Right-click your chat bridge channel → **"Copy Channel ID"**

### Get Role ID:
1. Server Settings → Roles
2. Right-click your "Linked Player" role → **"Copy Role ID"**

---

## Step 4: Configure the Plugin

Edit `config.json` in your plugin data directory:

```json
{
    "enableDiscordBot": true,
    "discordBotToken": "YOUR_BOT_TOKEN_HERE",
    "notificationChannelId": "1234567890123456789",
    "chatBridgeChannelId": "9876543210987654321",
    "linkedRoleId": "1122334455667788990",
    
    "enableChatBridge": true,
    "syncHytaleToDiscord": true,
    "syncDiscordToHytale": true,
    
    "enablePlayerJoinNotifications": true,
    "enablePlayerLeaveNotifications": true,
    
    "serverName": "HasH_Net Hytale Server",
    "serverAddress": "srv1280237.hstgr.cloud:5520",
    "serverDescription": "HasH_Net's official Hytale server"
}
```

---

## Step 5: Link Player Accounts

Players can link their Discord accounts using:

```
/linkdiscord <discord_user_id>
```

**To find Discord User ID:**
1. In Discord, right-click your username
2. Click **"Copy User ID"**

---

## Features

### 🎮 Server Notifications
- **Server Start**: Green embed with server info
- **Server Stop**: Red embed with final player count
- **Player Join**: Green embed with player name and count
- **Player Leave**: Red embed with updated count

### 💬 Chat Bridge
- Hytale chat appears in Discord with `[Hytale]` prefix
- Discord messages appear in Hytale with `[Discord]` prefix
- Configurable sync directions

### 🎭 Role Management
- Automatically assign roles to linked players
- Manage roles via bot commands
- Track linked accounts

### ⚙️ Commands
- `/linkdiscord <id>` - Link your Discord account
- `/notificationstatus` - Check bot status
- `/notificationreload` - Reload configuration

---

## Troubleshooting

### Bot not connecting?
- ✓ Check bot token is correct
- ✓ Verify all intents are enabled
- ✓ Ensure bot has permission to view channels

### Notifications not sending?
- ✓ Verify channel IDs are correct
- ✓ Check bot has "Send Messages" and "Embed Links" permissions
- ✓ Ensure bot can see the notification channel

### Chat bridge not working?
- ✓ Enable "Message Content Intent" in bot settings
- ✓ Verify chat channel ID is correct
- ✓ Check `enableChatBridge` is `true` in config

### Roles not assigning?
- ✓ Bot must have "Manage Roles" permission
- ✓ Bot's role must be HIGHER than the role being assigned
- ✓ Verify role ID is correct

---

## Security Notes

⚠️ **NEVER share your bot token!**
- Anyone with your token can control your bot
- If leaked, immediately reset it in the Developer Portal
- Do not commit config.json with tokens to public repositories

---

## Comparison: Webhook vs Bot

| Feature | Webhook (Old) | Bot (New) |
|---------|---------------|-----------|
| Send notifications | ✅ | ✅ |
| Receive messages | ❌ | ✅ |
| Chat bridge | ❌ | ✅ |
| Role management | ❌ | ✅ |
| Username management | ❌ | ✅ |
| Persistent connection | ❌ | ✅ |

---

## Need Help?

Check logs in `logs/latest.log` for error messages.

Common log messages:
- `"Discord bot connected successfully!"` - Bot is working
- `"Failed to start Discord bot"` - Check token and intents
- `"Notification channel not found"` - Check channel IDs
