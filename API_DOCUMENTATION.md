# Websocket Notifications API Documentation

## Overview
The WebSocket Notifications mod now includes an **incoming HTTP server** that responds to requests with JSON data about your Hytale server.

## Configuration
Enable the incoming server in your `config.json`:

```json
{
  "enableIncomingServer": true,
  "incomingServerPort": 8090
}
```

## API Endpoints

### 1. Server Status (Full Data)
**Endpoint:** `GET http://localhost:8090/api/server-status`

**Response:**
```json
{
  "timestamp": 1738534800000,
  "server": {
    "name": "My Hytale Server",
    "address": "play.myserver.com:5520",
    "description": "A friendly Hytale server"
  },
  "players": {
    "online": 3,
    "max": 20,
    "list": [
      {
        "name": "Player1",
        "uuid": "550e8400-e29b-41d4-a716-446655440000",
        "displayName": "Player1"
      },
      {
        "name": "Player2",
        "uuid": "550e8400-e29b-41d4-a716-446655440001",
        "displayName": "Player2"
      }
    ]
  }
}
```

### 2. Player List (Lightweight)
**Endpoint:** `GET http://localhost:8090/api/players`

**Response:**
```json
{
  "count": 3,
  "players": ["Player1", "Player2", "Player3"]
}
```

## Usage Examples

### JavaScript (Fetch API)
```javascript
// Get full server status
fetch('http://localhost:8090/api/server-status')
  .then(response => response.json())
  .then(data => {
    console.log(`${data.players.online}/${data.players.max} players online`);
    console.log('Players:', data.players.list);
  });

// Get player names only
fetch('http://localhost:8090/api/players')
  .then(response => response.json())
  .then(data => {
    console.log(`${data.count} players:`, data.players);
  });
```

### cURL
```bash
# Full server status
curl http://localhost:8090/api/server-status

# Player list only
curl http://localhost:8090/api/players
```

### Python
```python
import requests

# Get server status
response = requests.get('http://localhost:8090/api/server-status')
data = response.json()

print(f"{data['players']['online']}/{data['players']['max']} players online")
for player in data['players']['list']:
    print(f"  - {player['displayName']} ({player['uuid']})")
```

## CORS Support
The server includes CORS headers, allowing web browsers to access the API from any domain:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type`

## Integration with Discord/Websites
You can now:
1. **Query server status** from external websites/dashboards
2. **Display live player counts** on your website
3. **Build player leaderboards** using player data
4. **Create Discord bots** that check server status

## Discord Webhook Integration
The mod still supports sending notifications to Discord webhooks (player join/leave events). This is separate from the incoming API server and can be enabled independently.

## Port Configuration
- Default port: `8090`
- Make sure this port is open in your firewall if accessing from other machines
- For external access, configure port forwarding on your router

## Error Responses
All error responses return JSON:
```json
{
  "error": "Error description"
}
```

**Status Codes:**
- `200` - Success
- `405` - Method Not Allowed (only GET is supported)
- `500` - Internal Server Error
