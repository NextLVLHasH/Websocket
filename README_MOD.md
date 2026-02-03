# WebSocket Notification Mod

A Hytale server plugin that sends WebSocket notifications for player events and periodic polling.

## Features

- **Player Join Notifications**: Sends a WebSocket message when a player joins the server
- **Player Leave Notifications**: Sends a WebSocket message when a player leaves the server
- **Periodic Polling**: Automatically sends poll messages every 5 minutes
- **Configurable**: Easy to customize WebSocket endpoint and behavior

## JSON Message Format

### Player Join
```json
{
  "type": "PLAYER_JOIN",
  "player": "PlayerName",
  "uuid": "player-uuid-here",
  "timestamp": 1234567890
}
```

### Player Leave
```json
{
  "type": "PLAYER_LEAVE",
  "player": "PlayerName",
  "uuid": "player-uuid-here",
  "timestamp": 1234567890
}
```

### Periodic Poll
```json
{
  "type": "PERIODIC_POLL",
  "timestamp": 1234567890
}
```

## Configuration

Edit `WebSocketConfig.java` to change:
- `websocketUrl`: WebSocket server endpoint (default: `ws://localhost:8080/notifications`)
- `enablePeriodicPolling`: Enable/disable periodic polling (default: true)
- `pollingIntervalMinutes`: Interval for polling (default: 5 minutes)
- `enablePlayerJoinNotifications`: Enable/disable join notifications (default: true)
- `enablePlayerLeaveNotifications`: Enable/disable leave notifications (default: true)

## Building

```bash
# Windows
gradlew build

# Linux/Mac
./gradlew build
```

## Installation

1. Build the mod using Gradle
2. Copy the generated JAR from `build/libs/` to your Hytale server's `plugins/` folder
3. Start your Hytale server
4. Ensure your WebSocket server is running on the configured endpoint

## WebSocket Server Example

You'll need a WebSocket server listening for these notifications. Here's a simple Node.js example:

```javascript
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080, path: '/notifications' });

wss.on('connection', (ws) => {
  console.log('Hytale server connected');
  
  ws.on('message', (message) => {
    const data = JSON.parse(message);
    console.log('Received:', data);
    
    // Handle different notification types
    switch(data.type) {
      case 'PLAYER_JOIN':
        console.log(`Player ${data.player} joined!`);
        break;
      case 'PLAYER_LEAVE':
        console.log(`Player ${data.player} left!`);
        break;
      case 'PERIODIC_POLL':
        console.log('Periodic poll received');
        break;
    }
  });
});
```

## Architecture

The mod follows Hytale's documentation guidelines:
- Uses `PlayerReadyEvent` for join notifications (recommended over PlayerConnectEvent)
- Uses `PlayerDisconnectEvent` for leave notifications
- Implements proper event registration with `getEventRegistry()`
- Uses Java's native `HttpClient` and `WebSocket` APIs (Java 11+)
- Scheduled executor for periodic polling with proper cleanup

## Troubleshooting

### WebSocket Connection Fails
- Verify the WebSocket server is running
- Check the URL in `WebSocketConfig.java`
- Review server logs for connection errors

### Events Not Firing
- Ensure the mod is loaded (check server startup logs)
- Verify manifest.json is correctly configured
- Check that events are properly registered

## License

MIT License - Feel free to modify and use as needed
