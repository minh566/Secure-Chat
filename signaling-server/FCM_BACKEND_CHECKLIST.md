# FCM Backend Checklist (Sleep/Doze Realtime)

Checklist nay dung cho backend `signaling-server` de dam bao Android nhan push gan realtime khi man hinh tat (Doze).

## 1) Payload strategy (bat buoc)

- [ ] Chi gui **data message** (khong dung notification-only payload)
- [ ] Luon co key `type` de app route dung logic
- [ ] Payload nho gon (< 4KB)
- [ ] Dung key on dinh, khong doi ten tuy y

### `INCOMING_CALL` payload toi thieu

```json
{
  "type": "INCOMING_CALL",
  "sessionId": "call_123",
  "callerId": "uid_a",
  "callerName": "Tan",
  "callType": "VIDEO",
  "roomId": "room_abc"
}
```

### `NEW_MESSAGE` payload toi thieu

```json
{
  "type": "NEW_MESSAGE",
  "roomId": "room_abc",
  "roomName": "Tan",
  "senderId": "uid_a",
  "senderName": "Tan",
  "content": "Xin chao"
}
```

## 2) AndroidConfig FCM (bat buoc)

- [ ] `priority = HIGH`
- [ ] `ttl` ngan, phu hop use-case realtime
- [ ] Co `collapseKey` cho `NEW_MESSAGE` de tranh spam khi app ngu
- [ ] Khong collapse call invite (tranh mat cuoc goi)

### Goi y setup

- `INCOMING_CALL`:
  - [ ] `priority = HIGH`
  - [ ] `ttl = 30000 ms` (30s)
  - [ ] khong set collapse key
- `NEW_MESSAGE`:
  - [ ] `priority = HIGH`
  - [ ] `ttl = 120000 ms` (120s)
  - [ ] `collapseKey = "chat_{roomId}"`

## 3) Token hygiene (bat buoc)

- [ ] Luu token o `users/{uid}.fcmToken`
- [ ] Cap nhat token qua `onNewToken`
- [ ] Khi send loi token invalid/unregistered -> xoa token khoi user doc
- [ ] Neu 1 user dang nhap nhieu may -> luu danh sach token (khuyen nghi)

## 4) Retry va error handling

- [ ] Retry cho loi tam thoi (`UNAVAILABLE`, timeout) theo exponential backoff + jitter
- [ ] Khong retry loi vinh vien (`UNREGISTERED`, invalid token)
- [ ] Ghi log day du:
  - [ ] `eventType`
  - [ ] `uid`
  - [ ] `roomId`/`sessionId`
  - [ ] FCM response id hoac error code

## 5) Mapping voi app Android

Trong `SecureChatFCMService`:

- [ ] `type=INCOMING_CALL` -> hien incoming-call UI
- [ ] `type=NEW_MESSAGE` -> hien status-bar notification + mo dung room khi tap
- [ ] Neu app dang foreground chat room do -> can nhac khong show notification (tranh trung)

## 6) Kiem tra thuc te (Doze)

### A. Kiem tra token ton tai

- [ ] Firestore user doc co `fcmToken` hop le

### B. Kiem tra gui push backend

- [ ] Backend log co `sent messageId`
- [ ] Khong co error `UNREGISTERED`/`INVALID_ARGUMENT`

### C. Kiem tra tren may Android

- [ ] Tat man hinh 1-2 phut
- [ ] Gui `NEW_MESSAGE` tu account khac
- [ ] Xac nhan thong bao len tren status bar/lock screen trong vai giay

## 7) Trang thai hien tai cua project (quick audit)

Da co trong `SignalingServer.kt`:

- [x] Firebase Admin init tu env
- [x] `INCOMING_CALL` fallback push khi callee offline
- [x] Android priority `HIGH`
- [x] `ttl=30s` cho call invite

Con thieu de full production:

- [ ] Sender backend cho `NEW_MESSAGE` FCM
- [ ] Xu ly loi token invalid -> cleanup Firestore
- [ ] Retry co backoff cho loi tam thoi
- [ ] Co the luu nhieu token/mot user

## 8) Env checklist (server)

- [ ] `FIREBASE_SERVICE_ACCOUNT_JSON` hoac `FIREBASE_SERVICE_ACCOUNT_PATH`
- [ ] Service account co quyen Firebase Messaging + Firestore read
- [ ] Gio he thong server chuan (NTP) de `sentTime` va timeout khop

---

Neu can, buoc tiep theo la implement luon sender `NEW_MESSAGE` trong backend de dong bo 100% voi `SecureChatFCMService.handleNewMessage(...)`.

