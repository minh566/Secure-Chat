# Signaling Server (Ktor)

Server signaling cho SecureChat, hỗ trợ:
- REST tạo phiên call và cập nhật trạng thái
- WebSocket relay `incoming_call`, `offer`, `answer`, `ice_candidate`, `call_status`

## Endpoints

- `GET /health`
- `POST /calls`
- `POST /calls/{sessionId}/status`
- `WS /ws?userId={uid}`

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

