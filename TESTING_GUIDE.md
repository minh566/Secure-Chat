# SecureChat - Testing & Verification Guide

## Pre-Deployment Checklist

### 1. Firebase Project Setup
- [ ] Firebase project created
- [ ] Firestore database initialized
- [ ] Authentication enabled (Email/Password)
- [ ] Google Services JSON downloaded and placed in `app/`

### 2. Firestore Rules Deployment
```bash
cd D:\SecureChat\SecureChat
firebase deploy --only firestore:rules
```
Verify in Firebase Console:
- [ ] No errors in deployment
- [ ] Rules show new `iceCandidates/{role}/items` structure
- [ ] Legacy `iceCandidates/{candidateId}` still present for compatibility

### 3. Android Build
```bash
cd D:\SecureChat
./gradlew clean build
```
Expected result:
- [ ] Build succeeds
- [ ] No critical errors (warnings for deprecated API are acceptable)
- [ ] APK generated in `app/build/outputs/apk/debug/`

---

## Testing Scenarios

### Scenario 1: WebRTC Video Call - Bidirectional Video

**Prerequisites:**
- Two Android devices (or emulators) with cameras
- Both signed into the app
- Network connectivity between devices

**Test Steps:**

1. **Device A (Caller):**
   - Open app and navigate to contacts
   - Initiate video call to Device B
   - Observe logs: Should see `CALL INITIATED` with `localRole: caller`
   - Verify transceiver logs: `addTransceiver[video] SEND_RECV`

2. **Device B (Callee):**
   - See incoming call notification
   - Accept the call
   - Observe logs: Should see `CALL ANSWERED` with `localRole: callee`
   - Verify SDP logs: Both sides should show `✓ m=video is BIDIRECTIONAL`

3. **Both Devices:**
   - [ ] Local video visible (your own camera)
   - [ ] Remote video visible (other person's camera) - NO BLACK SCREEN
   - [ ] Audio works bidirectionally
   - [ ] Can end call on either device
   - [ ] Other device auto-ends (no manual action needed)

**Expected Logs:**

Device A (Caller) Console:
```
I/WebRTCManager: ╔═══ CALL INITIATED ═══
I/WebRTCManager: ║ localRole: caller
I/WebRTCManager: ║ remoteRole: callee
I/WebRTCManager: ╚════════════════════════

I/WebRTCManager: addTransceiver[video] SEND_RECV
I/WebRTCManager: addTransceiver[audio] SEND_RECV

I/WebRTCManager: ╔═══ SDP[local-offer] Media Sections ═══
I/WebRTCManager: ║ video:sendrecv | audio:sendrecv
I/WebRTCManager: ║ [0] m=video → a=sendrecv
I/WebRTCManager: ║ [1] m=audio → a=sendrecv
I/WebRTCManager: ║ ✓ m=video is BIDIRECTIONAL (sendrecv)
I/WebRTCManager: ╚═══════════════════════════════════

D/WebRTCManager: Sending OFFER from caller

I/WebRTCManager: ╔═══ SDP[remote-answer] Media Sections ═══
I/WebRTCManager: ║ video:sendrecv | audio:sendrecv
I/WebRTCManager: ║ ✓ m=video is BIDIRECTIONAL (sendrecv)
I/WebRTCManager: ╚═══════════════════════════════════

D/WebRTCManager: Remote description (ANSWER) set, flushing ICE candidates...

I/WebRTCManager: ╔═══ Flushing 5 ICE Candidates ═══
I/WebRTCManager: ║ Result: 5 succeeded, 0 failed
I/WebRTCManager: ╚═══════════════════════════════════════════════════════════

I/WebRTCManager: onIceConnectionChange: CONNECTED (role=caller)
```

Device B (Callee) Console:
```
I/WebRTCManager: ╔═══ CALL ANSWERED ═══
I/WebRTCManager: ║ localRole: callee
I/WebRTCManager: ║ remoteRole: caller
I/WebRTCManager: ╚════════════════════════

I/WebRTCManager: addTransceiver[video] SEND_RECV
I/WebRTCManager: addTransceiver[audio] SEND_RECV

I/WebRTCManager: ╔═══ SDP[remote-offer] Media Sections ═══
I/WebRTCManager: ║ video:sendrecv | audio:sendrecv
I/WebRTCManager: ║ ✓ m=video is BIDIRECTIONAL (sendrecv)
I/WebRTCManager: ╚═══════════════════════════════════

D/WebRTCManager: Remote description (OFFER) set, flushing ICE candidates...

I/WebRTCManager: ╔═══ Flushing 5 ICE Candidates ═══
I/WebRTCManager: ║ Result: 5 succeeded, 0 failed
I/WebRTCManager: ╚═══════════════════════════════════════════════════════════

I/WebRTCManager: ╔═══ SDP[local-answer] Media Sections ═══
I/WebRTCManager: ║ video:sendrecv | audio:sendrecv
I/WebRTCManager: ║ ✓ m=video is BIDIRECTIONAL (sendrecv)
I/WebRTCManager: ╚═══════════════════════════════════

D/WebRTCManager: Sending ANSWER from callee

I/WebRTCManager: onIceConnectionChange: CONNECTED (role=callee)
```

**Verification:**
- [ ] Both logs show `m=video is BIDIRECTIONAL`
- [ ] Both see video track attached: `Remote VideoTrack attached`
- [ ] Connection state reaches CONNECTED
- [ ] No messages like "missing m=video" or "not bidirectional"

---

### Scenario 2: Message Delivery & Read Status

**Prerequisites:**
- Two devices with app installed
- Both users in same chat room

**Test Steps:**

1. **Device A sends message:**
   - Type message "Hello"
   - Send
   - [ ] Message appears with status "Đã gửi" (gray)
   - [ ] In chat list, see "Bạn: Hello" (dimmed gray)

2. **Device B receives message:**
   - [ ] Notification received
   - [ ] Message appears in chat list in **bold** (unread)
   - [ ] Open chat room
   - [ ] Message appears in chat detail

3. **Device A (after Device B opens chat):**
   - [ ] Message status changes to "Đã nhận" (Received)
   - [ ] After Device B views message, status changes to "Đã xem" (Seen)

4. **Device B (after opening chat):**
   - [ ] Message in list becomes normal weight (no longer bold)
   - [ ] Chat list preview shows normal gray (no longer bold)

**Firestore Verification:**
```
rooms/{roomId}/messages/{messageId}
├── senderId: "userA_id"
├── deliveredTo: ["userA_id", "userB_id"]  ← Both included
├── seenBy: ["userA_id", "userB_id"]       ← Both included when read
└── ...
```

---

### Scenario 3: Long-Press Delete Conversation

**Prerequisites:**
- Chat room with multiple messages

**Test Steps:**

1. **Long-press on chat room in list:**
   - [ ] Room highlights with red background tint
   - [ ] Confirmation dialog appears

2. **Dialog content:**
   - Title: "Xóa cuộc trò chuyện"
   - Message: "Xóa cuộc trò chuyện này và tất cả tin nhắn?"
   - Buttons: "Xóa" (red/danger) and "Hủy"

3. **Tap "Xóa":**
   - [ ] Dialog closes
   - [ ] Room immediately removed from list
   - [ ] Room no longer visible in list

4. **Firestore Verification:**
   - [ ] Document `rooms/{roomId}` deleted
   - [ ] All messages in `rooms/{roomId}/messages/*` deleted
   - [ ] Room no longer in chat list next app restart

5. **Tap "Hủy":**
   - [ ] Dialog closes
   - [ ] Room remains in list
   - [ ] Red tint removed
   - [ ] No changes to Firestore

---

### Scenario 4: Auto-End Call on Remote Disconnect

**Prerequisites:**
- Two devices in active call
- Video and audio working

**Test Steps:**

1. **Device A ends call:**
   - Tap "End Call" button
   - [ ] Call screen closes on Device A
   - [ ] Device A updates Firestore: `calls/{sessionId}/status = ENDED`

2. **Device B observes status change:**
   - [ ] Device B **automatically** receives status update (no manual action needed)
   - [ ] Device B call screen **automatically** closes
   - [ ] Device B returns to home screen

3. **Reverse test - Device B ends first:**
   - [ ] Initiate new call
   - [ ] Device B ends call
   - [ ] Device A call screen closes automatically
   - [ ] Device A returns to home screen

**Expected Behavior:**
- No need to manually end call on both devices
- UI responds immediately to remote status change
- No "call already ended" errors

**Firestore Verification:**
```
calls/{sessionId}
├── status: "ENDED"  ← Updated by first device
└── ...
```

---

### Scenario 5: Camera Fallback (Older Devices)

**Prerequisites:**
- Android device with older or limited camera support

**Test Steps:**

1. **Initiate call on device:**
   - Observe logs during local stream setup

2. **Check camera selection:**
   - Look for logs indicating which camera API is used

**Expected Logs - Camera2 Success:**
```
I/WebRTCManager: Using Camera2 capturer: front
I/WebRTCManager: startCapture success: 1280x720@30
```

**Expected Logs - Camera2 Fails, Camera1 Used:**
```
I/WebRTCManager: Using Camera1 capturer fallback: front
I/WebRTCManager: startCapture success: 1280x720@30
```

**Expected Logs - Both Fail:**
```
E/WebRTCManager: Failed to create any camera capturer
```

**Verification:**
- [ ] Video still works (no black screen from camera init failure)
- [ ] Logs show appropriate fallback behavior
- [ ] Camera1 fallback works on older devices

---

### Scenario 6: Firestore Security Rules

**Prerequisites:**
- Firebase project with updated rules
- Two users: Alice (logged in) and Bob (not in call)

**Test Steps:**

1. **Alice initiates call to Bob:**
   - ICE candidates should write to `calls/{sessionId}/iceCandidates/caller/items`
   - [ ] Write succeeds (Alice is caller)

2. **Bob accepts call:**
   - Bob's ICE candidates should write to `calls/{sessionId}/iceCandidates/callee/items`
   - [ ] Write succeeds (Bob is callee)

3. **Alice reads Bob's ICE:**
   - Should read from `calls/{sessionId}/iceCandidates/callee/items`
   - [ ] Read succeeds (Alice is in call)

4. **Charlie (not in call) tries to access:**
   - Attempt to read/write to `calls/{sessionId}/iceCandidates/*`
   - [ ] Firestore returns permission denied

**Firestore Console Verification:**
```
calls/
└── {sessionId}/
    ├── offer: {...}
    ├── answer: {...}
    ├── status: "CONNECTED"
    └── iceCandidates/
        ├── caller/
        │   └── items/
        │       ├── {docId1}: {candidate: "..."}
        │       ├── {docId2}: {candidate: "..."}
        │       └── ...
        └── callee/
            └── items/
                ├── {docId1}: {candidate: "..."}
                ├── {docId2}: {candidate: "..."}
                └── ...
```

---

## Error Handling Tests

### Test 1: Network Disconnection During Call

**Steps:**
1. Start video call between two devices
2. Disconnect network on Device A (turn off WiFi/mobile data)
3. Observe behavior

**Expected:**
- [ ] Call automatically terminates
- [ ] Error message shown to user
- [ ] Can initiate new call after reconnection

### Test 2: Firestore Schema Mismatch

**Steps:**
1. Manually create a ChatRoom with malformed unreadCount (e.g., string instead of map)
2. Open app and navigate to chat list

**Expected:**
- [ ] No crash
- [ ] Malformed document is skipped
- [ ] Other valid rooms still load
- [ ] Logs show error for malformed doc

### Test 3: Concurrent Call Attempts

**Steps:**
1. Device A initiates call to Device B
2. Device B simultaneously initiates call to Device A
3. Observe which call is established

**Expected:**
- [ ] One call succeeds
- [ ] Other call is declined/missed
- [ ] No crashes
- [ ] Users can retry

### Test 4: Back Button During Call

**Steps:**
1. During active call, press back button
2. Confirm end call dialog appears

**Expected:**
- [ ] Dialog prevents accidental exit
- [ ] Can confirm or cancel
- [ ] Both devices properly handle end

---

## Performance Tests

### Test 1: Message List Performance

**Setup:**
1. Create chat with 1000+ messages
2. Open chat room

**Expected:**
- [ ] Chat list scrolls smoothly
- [ ] No lag or jank
- [ ] Messages load progressively

### Test 2: Call Connection Speed

**Setup:**
1. Measure time from call initiation to video appearing

**Expected:**
- [ ] Connection established within 3-5 seconds
- [ ] Video appears within 2-3 seconds of connection
- [ ] Audio synced with video

### Test 3: Memory Usage

**Setup:**
1. Monitor memory during 5-minute video call
2. End call and observe memory cleanup

**Expected:**
- [ ] Memory stays under 300MB during call
- [ ] Memory released after call ends (no leaks)
- [ ] Repeated calls don't accumulate memory

---

## Regression Tests

### Test 1: Existing Features Still Work

- [ ] User registration and login
- [ ] User profile update
- [ ] Contact list viewing
- [ ] Chat room creation
- [ ] Message sending (non-status)
- [ ] Chat history viewing
- [ ] Call history viewing
- [ ] User logout

### Test 2: UI Components

- [ ] Navigation works between all screens
- [ ] Buttons are responsive
- [ ] Text input works
- [ ] Dialogs appear/dismiss correctly
- [ ] Lists scroll smoothly
- [ ] Permissions are requested correctly

---

## Debugging Tips

### Enable WebRTC Logs

In Android Studio:
```
Logcat → Filter by "WebRTCManager"
```

### Monitor Firestore in Real-Time

Firebase Console:
1. Go to Firestore Database
2. Open collection `calls`
3. Click on session to see offer/answer/ICE candidates
4. Watch structure as call progresses

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Black screen on remote video | ICE candidates not processed | Check logs for "Flushing ICE" and success count |
| One-way video (only audio) | Transceiver not SEND_RECV | Verify logs show `addTransceiver[video] SEND_RECV` |
| No video on older device | Camera2 not supported | Check logs for "Camera1 fallback" |
| Crash on chat list open | Firestore schema mismatch | Check logs for "runCatching" with malformed docs |
| Call doesn't auto-end | Status observer not active | Verify `observeCallStatus` is in ViewModel init |
| Message status always "Đã gửi" | Delivery ack not called | Check if `markMessagesDelivered` called in ChatViewModel |

---

## Post-Deployment Checklist

After deploying to production:

- [ ] Monitor Firebase Crashlytics for any new crashes
- [ ] Check WebRTC metrics in Firebase (if configured)
- [ ] Monitor Firestore read/write quotas
- [ ] Verify no unauthorized access attempts
- [ ] Test with various Android versions (API 21+)
- [ ] Test with various network conditions (4G, 5G, WiFi)
- [ ] Collect user feedback on video quality
- [ ] Monitor battery usage during calls

---

## Support Contacts

If issues occur:

1. **WebRTC Issues:** Check browser/device logs for detailed error messages
2. **Firestore Issues:** Check Firebase Console for quota/billing alerts
3. **App Crashes:** Check Crashlytics in Firebase Console
4. **Network Issues:** Test with network traffic analysis tools

---

**Testing Guide Complete**

