# WebSocket & Discord Notification Mod for Hytale

A comprehensive Hytale server plugin that sends notifications for player events to multiple destinations:
- **Discord Webhooks** with rich embeds
- **Website WebSockets** with JSON messages
- Support for **multiple WebSocket endpoints**

## ✨ Features

### Dual Notification System
- 🎨 Discord webhook integration with rich colored embeds
- 🌐 WebSocket support for real-time web applications
- 📡 Multiple WebSocket endpoints support

### Event Notifications
- ✅ Player join notifications
- ❌ Player leave notifications
- 🔄 Periodic server status polling (configurable interval)

### Configuration
- 📝 JSON configuration file with auto-generation
- 🔧 Enable/disable each notification type independently
- ⚙️ Configure server information (name, address, description)
- 🔗 Support for multiple WebSocket URLs

### In-Game Commands
- `/notificationstatus` - View current notification status
- `/notificationreload` - Reload configuration without restart

## 📦 Installation

1. **Build the mod:**
   ```bash
   gradlew build
   ```

2. **Copy** `build/libs/WebsocketNotificationMod-1.0.jar` to your Hytale server's `plugins/` folder

3. **Start the server** - a default `config.json` will be created in `plugins/WebsocketNotificationMod/`

4. **Configure** - Edit `plugins/WebsocketNotificationMod/config.json`

5. **Reload** - Use `/notificationreload` in-game or restart server

## ⚙️ Configuration

Configuration file: `plugins/WebsocketNotificationMod/config.json`

```json
{
  "enableDiscordWebhook": true,
  "discordWebhookUrl": "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN",
  "discordBotName": "Hytale Server Bot",
  "discordAvatarUrl": "",
  
  "enableWebsiteWebsocket": true,
  "websocketUrls": [
    "ws://localhost:8080/notifications",
    "ws://api.yoursite.com:8080/hytale"
  ],
  
  "enablePlayerJoinNotifications": true,
  "enablePlayerLeaveNotifications": true,
  "enablePeriodicPolling": true,
  "pollingIntervalMinutes": 5,
  
  "serverName": "My Hytale Server",
  "serverAddress": "play.myserver.com:5520",
  "serverDescription": "A friendly survival server"
}
```

## 🎮 Discord Setup

1. Open Discord server settings
2. Go to **Integrations** → **Webhooks** → **New Webhook**
3. Copy the webhook URL
4. Paste into `discordWebhookUrl` in config.json
5. Run `/notificationreload`

### Discord Message Examples

**Player Join** (🟢 Green)
```
✅ Player Joined
**Steve** joined the server
```

**Player Leave** (🔴 Red)
```
❌ Player Left
**Steve** left the server
```

**Status Poll** (🔵 Blue)
```
🔄 Server Status
Server: **My Hytale Server**
Players Online: **5**
```

## 🌐 WebSocket Integration

### Message Format (JSON)

#### Player Join
```json
{
  "type": "PLAYER_JOIN",
  "player": "Steve",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "server": "My Hytale Server",
  "timestamp": 1234567890123
}
```

#### Player Leave
```json
{
  "type": "PLAYER_LEAVE",
  "player": "Steve",
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "server": "My Hytale Server",
  "timestamp": 1234567890123
}
```

#### Periodic Poll
```json
{
  "type": "PERIODIC_POLL",
  "server": "My Hytale Server",
  "players": 5,
  "timestamp": 1234567890123
}
```

### Example WebSocket Server (Node.js)

```javascript
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080, path: '/notifications' });

wss.on('connection', (ws) => {
  console.log('✅ Hytale server connected');
  
  ws.on('message', (message) => {
    const data = JSON.parse(message);
    console.log('Received:', data);
    
    // Update database, broadcast to website, etc.
  });
});
```

## 🎯 Commands

### `/notificationstatus`
View current notification configuration and connection status.

**Permission:** None (all players)

### `/notificationreload`
Reload configuration file.

**Permission:** `websockets.admin.reload`

## 🔧 Troubleshooting

### Discord Webhook Not Working
✓ Verify webhook URL is correct  
✓ Check `enableDiscordWebhook` is `true`  
✓ Use `/notificationstatus` to check connection  
✓ Review server console for errors

### WebSocket Connection Failed
✓ Ensure WebSocket server is running  
✓ Verify URL format: `ws://` or `wss://`  
✓ Check firewall settings  
✓ Review server logs

### No Notifications Sent
✓ Verify event notifications are enabled  
✓ Check at least one notification method is enabled  
✓ Use `/notificationstatus` to diagnose

## 📚 Requirements

- Hytale Server (January 2026+)
- Java 21+
- Gradle 8.5+ (for building)

## 📄 License

MIT License - Free to use and modify
