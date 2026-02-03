# Quick Start Guide - Discord Bot

## 🚀 Fast Setup (5 Minutes)

### 1. Create Discord Bot (2 min)
1. Visit https://discord.com/developers/applications
2. Click "New Application" → Name it
3. Go to "Bot" tab → "Add Bot"
4. Enable these intents:
   - ✅ Presence Intent
   - ✅ Server Members Intent  
   - ✅ Message Content Intent
5. Click "Reset Token" → **Copy token**

### 2. Invite Bot (1 min)
1. "OAuth2" → "URL Generator"
2. Check: `bot`
3. Check permissions: Send Messages, Embed Links, Read Message History, Manage Roles
4. Copy URL → Open in browser → Select your server

### 3. Get Channel IDs (1 min)
Enable Developer Mode: Discord Settings → Advanced → Developer Mode

Right-click channels → "Copy Channel ID":
- Notification channel (e.g., #server-status)
- Chat bridge channel (e.g., #game-chat)

Right-click role → "Copy Role ID":
- Linked player role (e.g., @Player)

### 4. Configure Plugin (1 min)
Edit `config.json`:
```json
{
    "enableDiscordBot": true,
    "discordBotToken": "PASTE_TOKEN_HERE",
    "notificationChannelId": "PASTE_NOTIFICATION_CHANNEL_ID",
    "chatBridgeChannelId": "PASTE_CHAT_CHANNEL_ID",
    "linkedRoleId": "PASTE_ROLE_ID",
    
    "enableChatBridge": true,
    "syncHytaleToDiscord": true,
    "syncDiscordToHytale": true,
    
    "enablePlayerJoinNotifications": true,
    "enablePlayerLeaveNotifications": true,
    
    "serverName": "Your Server Name",
    "serverAddress": "your.server.com:5520",
    "serverDescription": "Your server description"
}
```

### 5. Build & Deploy
```bash
./gradlew build
```

Copy `build/libs/WebsocketNotificationMod-1.0.jar` to your Hytale `plugins/` folder.

Restart server → Check logs for:
```
Discord bot connected successfully!
```

---

## ✅ Test It Works

1. **Join server** → Discord notification appears
2. **Type in Hytale chat** → Message appears in Discord
3. **Type in Discord** → Message appears in Hytale logs
4. **Run `/linkdiscord YOUR_DISCORD_ID`** → Role assigned

---

## 📋 Get Your Discord User ID

1. Right-click your username in Discord
2. Click "Copy User ID"
3. In-game: `/linkdiscord PASTE_ID_HERE`

---

## ⚠️ Common Issues

**Bot offline?**
- Check token is correct
- Verify intents are enabled

**No notifications?**
- Check channel IDs
- Verify bot can see channels
- Check bot has Send Messages permission

**Chat not working?**
- Enable "Message Content Intent"
- Check chat channel ID

**Roles not assigning?**
- Bot needs "Manage Roles" permission
- Bot's role must be HIGHER than role being assigned

---

## 🎯 Quick Commands

- `/linkdiscord <id>` - Link Discord account
- `/notificationstatus` - Check bot status
- `/notificationreload` - Reload config

---

**Need more help?** See `DISCORD_BOT_SETUP.md` for detailed guide.
