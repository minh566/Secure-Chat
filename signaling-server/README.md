# Signaling Server (Ktor)

Server signaling cho SecureChat, hỗ trợ:
- REST tạo phiên call và cập nhật trạng thái
- WebSocket relay `incoming_call`, `offer`, `answer`, `ice_candidate`, `call_status`
- SFU token endpoint cho group-call production (LiveKit-compatible)

## Production notes

- FCM backend checklist cho sleep/doze realtime: `signaling-server/FCM_BACKEND_CHECKLIST.md`

## Endpoints

- `GET /health`
- `GET /turn-credentials`
- `POST /sfu/token`
- `POST /calls`
- `POST /calls/{sessionId}/status`
- `POST /notifications/new-message`
- `WS /ws?userId={uid}`

### SFU token request

```json
{
  "roomName": "room-group-123",
  "identity": "userA",
  "participantName": "Tan"
}
```

### SFU token response

```json
{
  "wsUrl": "wss://livekit.example.com",
  "token": "<jwt>",
  "roomName": "room-group-123",
  "identity": "userA",
  "expiresInSeconds": 3600
}
```

## Environment

### Firebase Admin (required for offline call push)

- `FIREBASE_SERVICE_ACCOUNT_JSON` (recommended in CI)
- or `FIREBASE_SERVICE_ACCOUNT_PATH` / `GOOGLE_APPLICATION_CREDENTIALS`

### TURN (optional)

- `TURN_URLS`
- `TURN_USERNAME`
- `TURN_CREDENTIAL`
- `TURN_TTL_SECONDS` (optional, default `3600`)

### SFU token minting (required for `/sfu/token`)

- `LIVEKIT_WS_URL`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `LIVEKIT_TOKEN_TTL_SECONDS` (optional, default `3600`)

## Payload mẫu

### Create call

```json
{
  "sessionId": "session-123",
  "callerId": "userA",
  "calleeId": "userB",
  "type": "VIDEO",
  "status": "RINGING"
}
```

### WebSocket envelope

```json
{
  "type": "offer",
  "sessionId": "session-123",
  "fromUserId": "userA",
  "toUserId": "userB",
  "sdp": "v=0...",
  "timestamp": 1710000000000
}
```

### Push NEW_MESSAGE

```json
{
  "roomId": "room-123",
  "roomName": "Tan",
  "senderId": "userA",
  "senderName": "Tan",
  "content": "xin chao",
  "recipientIds": ["userB", "userC"]
}
```

Response:

```json
{
  "requested": 2,
  "sent": 2,
  "failed": 0,
  "cleanedInvalidTokens": 0
}
```

Server sends Android data message with:

- `priority=HIGH`
- `ttl=120s`
- `collapseKey=chat_{roomId}`

## Run

```bash
./gradlew :signaling-server:run
```

Windows PowerShell:

```powershell
.\gradlew.bat :signaling-server:run
```

## Test

```bash
./gradlew :signaling-server:test
```

