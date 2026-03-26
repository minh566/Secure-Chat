# Quick Reference: Key Code Changes

## 1. Firestore Rules - Role-Scoped ICE

**File:** `SecureChat/firestore.rules`

```javascript
// ── Calls collection ───────────────────────────────────────────────────
match /calls/{sessionId} {
  // ... existing rules ...
  
  // ── ICE candidates (legacy flat structure) ─────────────────────────
  match /iceCandidates/{candidateId} {
    allow read, write: if isAuthenticated() &&
                          (request.auth.uid == get(...).data.callerId ||
                           request.auth.uid == get(...).data.calleeId);
  }

  // ── ICE candidates (role-scoped structure) ─────────────────────────
  // Separate collections for caller and callee roles
  match /iceCandidates/{role}/items/{itemId} {
    allow read: if isAuthenticated() &&
                   (request.auth.uid == get(...).data.callerId ||
                    request.auth.uid == get(...).data.calleeId);
    allow write: if isAuthenticated() &&
                    (request.auth.uid == get(...).data.callerId ||
                     request.auth.uid == get(...).data.calleeId);
  }
}
```

---

## 2. WebRTC - Transceiver Direction (SEND_RECV)

**File:** `app/src/main/java/com/securechat/data/remote/webrtc/WebRTCManager.kt`  
**Lines:** 195-205

```kotlin
private fun createPeerConnection(sessionId: String): PeerConnection {
    // ... create pc ...
    
    // ═══ Cấu hình Transceiver: SEND_RECV (rõ ràng) ═══
    val sendRecvInit = RtpTransceiver.RtpTransceiverInit(
        RtpTransceiver.RtpTransceiverDirection.SEND_RECV
    )
    
    localVideoTrack?.let { 
        pc.addTransceiver(it, sendRecvInit)
        Log.i(TAG, "addTransceiver[video] SEND_RECV")
    }
    localAudioTrack?.let { 
        pc.addTransceiver(it, sendRecvInit)
        Log.i(TAG, "addTransceiver[audio] SEND_RECV")
    }
    return pc
}
```

---

## 3. WebRTC - Camera Fallback (Camera2 → Camera1)

**File:** `app/src/main/java/com/securechat/data/remote/webrtc/WebRTCManager.kt`  
**Lines:** 115-143

```kotlin
private fun createBestCameraCapturer(): VideoCapturer? {
    val camera2Enumerator = Camera2Enumerator(context)
    val camera2Front = camera2Enumerator.deviceNames
        .firstOrNull { camera2Enumerator.isFrontFacing(it) }
    val camera2Name = camera2Front ?: camera2Enumerator.deviceNames.firstOrNull()
    
    if (camera2Name != null) {
        val camera2Capturer = camera2Enumerator.createCapturer(camera2Name, null)
        if (camera2Capturer != null) {
            Log.i(TAG, "Using Camera2 capturer: $camera2Name")
            return camera2Capturer
        }
    }

    // Fallback to Camera1 if Camera2 fails
    val camera1Enumerator = Camera1Enumerator(false)
    val camera1Front = camera1Enumerator.deviceNames
        .firstOrNull { camera1Enumerator.isFrontFacing(it) }
    val camera1Name = camera1Front ?: camera1Enumerator.deviceNames.firstOrNull()
    
    if (camera1Name != null) {
        val camera1Capturer = camera1Enumerator.createCapturer(camera1Name, null)
        if (camera1Capturer != null) {
            Log.i(TAG, "Using Camera1 capturer fallback: $camera1Name")
            return camera1Capturer
        }
    }

    Log.e(TAG, "Failed to create any camera capturer")
    return null
}
```

---

## 4. WebRTC - SDP Media Section Logging

**File:** `app/src/main/java/com/securechat/data/remote/webrtc/WebRTCManager.kt`  
**Lines:** 351-390

```kotlin
private fun logSdpMediaSections(label: String, sdp: String) {
    val sections = mutableListOf<Pair<String, String>>()
    var currentType: String? = null
    var currentDir = "unspecified"

    sdp.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.startsWith("m=")) {
            currentType?.let { sections.add(it to currentDir) }
            currentType = line.removePrefix("m=").substringBefore(' ')
            currentDir = "unspecified"
        } else if (line == "a=sendrecv") {
            currentDir = "sendrecv"
        } else if (line == "a=sendonly") {
            currentDir = "sendonly"
        } else if (line == "a=recvonly") {
            currentDir = "recvonly"
        } else if (line == "a=inactive") {
            currentDir = "inactive"
        }
    }
    currentType?.let { sections.add(it to currentDir) }

    if (sections.isEmpty()) {
        Log.w(TAG, "SDP[$label] has no media sections")
        return
    }

    val summary = sections.joinToString(" | ") { "${it.first}:${it.second}" }
    Log.i(TAG, "╔═══ SDP[$label] Media Sections ═══")
    Log.i(TAG, "║ $summary")
    
    sections.forEachIndexed { index, (type, direction) ->
        Log.i(TAG, "║ [$index] m=$type → a=$direction")
    }

    val videoDirection = sections.firstOrNull { it.first == "video" }?.second
    when (videoDirection) {
        null -> Log.w(TAG, "║ ⚠️  missing m=video section")
        "sendrecv" -> Log.i(TAG, "║ ✓ m=video is BIDIRECTIONAL (sendrecv)")
        else -> Log.w(TAG, "║ ⚠️  m=video direction is '$videoDirection' (not bidirectional)")
    }
    
    Log.i(TAG, "╚═══════════════════════════════════")
}
```

---

## 5. WebRTC - Role-Based ICE Initialization

**File:** `app/src/main/java/com/securechat/data/remote/webrtc/WebRTCManager.kt`

### Caller Side:
```kotlin
fun call(sessionId: String) {
    localIceRole = ROLE_CALLER      // "caller"
    remoteIceRole = ROLE_CALLEE     // "callee"
    pendingRemoteIceCandidates.clear()
    processedRemoteCandidateKeys.clear()
    
    Log.i(TAG, "╔═══ CALL INITIATED ═══")
    Log.i(TAG, "║ localRole: $localIceRole")
    Log.i(TAG, "║ remoteRole: $remoteIceRole")
    Log.i(TAG, "╚════════════════════════")
    
    // ... create PeerConnection, send offer ...
}
```

### Callee Side:
```kotlin
fun answer(sessionId: String, offerSdp: String) {
    localIceRole = ROLE_CALLEE      // "callee"
    remoteIceRole = ROLE_CALLER     // "caller"
    pendingRemoteIceCandidates.clear()
    processedRemoteCandidateKeys.clear()

    Log.i(TAG, "╔═══ CALL ANSWERED ═══")
    Log.i(TAG, "║ localRole: $localIceRole")
    Log.i(TAG, "║ remoteRole: $remoteIceRole")
    Log.i(TAG, "╚════════════════════════")
    
    // ... create PeerConnection, set remote offer, send answer ...
}
```

---

## 6. WebRTC - Async SDP Handling

**File:** `app/src/main/java/com/securechat/data/remote/webrtc/WebRTCManager.kt`  
**Lines:** 245-252

```kotlin
callRepository.observeAnswer(sessionId).collectLatest { answerSdp ->
    answerSdp?.let {
        logSdpMediaSections("remote-answer", it)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description (ANSWER) set, flushing ICE candidates...")
                flushPendingIceCandidates()  // ← MOVED HERE (inside callback)
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "setRemoteDescription(answer) failed: $error")
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, it))
    }
}
```

**Key Point:** `flushPendingIceCandidates()` is now called **inside** the `onSetSuccess()` callback, ensuring remote description is fully set before processing ICE candidates.

---

## 7. Repository - Role-Scoped ICE Collections

**File:** `app/src/main/java/com/securechat/data/repository/CallRepositoryImpl.kt`  
**Lines:** 39-42

```kotlin
private fun roleIceRef(sessionId: String, role: String) = 
    callsRef.document(sessionId)
        .collection("iceCandidates")
        .document(role)  // ← "caller" or "callee"
        .collection("items")
```

### Sending ICE Candidate:
```kotlin
override suspend fun sendIceCandidate(
    sessionId: String, 
    role: String, 
    candidate: String
): Resource<Unit> {
    return try {
        roleIceRef(sessionId, role)
            .add(mapOf("candidate" to candidate)).await()
        Resource.Success(Unit)
    } catch (e: Exception) { Resource.Error(...) }
}
```

### Observing ICE Candidates:
```kotlin
override fun observeIceCandidates(
    sessionId: String, 
    role: String
): Flow<List<String>> {
    val roleFlow = callbackFlow {
        val listener = roleIceRef(sessionId, role)
            .addSnapshotListener { snap, _ ->
                val candidates = snap?.documents
                    ?.mapNotNull { it.getString("candidate") } ?: emptyList()
                trySend(candidates)
            }
        awaitClose { listener.remove() }
    }

    val legacyFlow = callbackFlow {
        // Legacy fallback for backward compatibility
        val listener = callsRef.document(sessionId)
            .collection("iceCandidates")
            .addSnapshotListener { snap, _ ->
                val candidates = snap?.documents
                    ?.mapNotNull { it.getString("candidate") } ?: emptyList()
                trySend(candidates)
            }
        awaitClose { listener.remove() }
    }

    return combine(roleFlow, legacyFlow) { 
        (roleCandidates + legacyCandidates).distinct() 
    }.distinctUntilChanged()
}
```

---

## 8. Repository - Call Status Real-Time Sync

**File:** `app/src/main/java/com/securechat/data/repository/CallRepositoryImpl.kt`  
**Lines:** 84-95

```kotlin
override fun observeCallStatus(sessionId: String): Flow<CallStatus?> = callbackFlow {
    val listener = callsRef.document(sessionId)
        .addSnapshotListener { snap, _ ->
            val raw = snap?.getString("status")
            val status = raw?.let { value ->
                runCatching { CallStatus.valueOf(value) }.getOrNull()
            }
            trySend(status)
        }
    awaitClose { listener.remove() }
}.distinctUntilChanged()
```

---

## 9. ViewModel - Call Status Observer

**File:** `app/src/main/java/com/securechat/ui/screens/call/CallViewModel.kt`

```kotlin
private fun observeCallStatus() {
    viewModelScope.launch {
        callRepository.observeCallStatus(sessionId)
            .filterNotNull()
            .collect { status ->
                when (status) {
                    CallStatus.ENDED, CallStatus.DECLINED, CallStatus.MISSED ->
                        handleRemoteEnded(status)
                    CallStatus.ACCEPTED -> _uiState.update { 
                        it.copy(status = CallStatus.ACCEPTED) 
                    }
                    CallStatus.RINGING -> {} // Already in RINGING
                }
            }
    }
}

private fun handleRemoteEnded(status: CallStatus) {
    if (!endHandled.compareAndSet(false, true)) return
    webRTCManager.endCall()
    _uiState.update { it.copy(status = status) }
}
```

---

## 10. Message Status - Delivery & Read Tracking

**File:** `app/src/main/java/com/securechat/domain/model/Models.kt`

```kotlin
data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val content: String = "",
    val deliveredTo: List<String> = emptyList(),  // ← NEW
    val seenBy: List<String> = emptyList(),       // ← NEW
    val createdAt: Date = Date()
)
```

---

## 11. Message Status - Auto-Acknowledgement

**File:** `app/src/main/java/com/securechat/ui/screens/chat/ChatViewModel.kt`

```kotlin
private fun acknowledgeIncomingMessages(userId: String) {
    viewModelScope.launch {
        chatRepository.markMessagesDelivered(roomId, userId)
        chatRepository.markMessagesSeen(roomId, userId)
        chatRepository.markAsRead(roomId, userId)
    }
}

private fun loadMessages() {
    viewModelScope.launch {
        getMessagesUseCase(roomId).collect { result ->
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, messages = result.data) }
                    if (currentUserId.isNotBlank()) {
                        acknowledgeIncomingMessages(currentUserId)
                    }
                }
                // ...
            }
        }
    }
}
```

---

## 12. Message Status - Display in Chat Detail

**File:** `app/src/main/java/com/securechat/ui/screens/chat/ChatScreen.kt`

```kotlin
@Composable
fun ChatScreen(/* ... */) {
    val recipientId = uiState.chatRoom?.members
        ?.firstOrNull { it != currentUserId }
    
    // ... LazyColumn with messages ...
    items(uiState.messages) { message ->
        MessageBubble(
            message = message,
            isMine = message.senderId == currentUserId,
            statusText = if (message.senderId == currentUserId) {
                getOutgoingStatusText(message, recipientId)
            } else null
        )
    }
}

private fun getOutgoingStatusText(message: Message, recipientId: String?): String {
    if (recipientId.isNullOrBlank()) return "Đã gửi"
    return when {
        message.seenBy.contains(recipientId) -> "Đã xem"
        message.deliveredTo.contains(recipientId) -> "Đã nhận"
        else -> "Đã gửi"
    }
}
```

---

## 13. Message Preview - List Display with Status

**File:** `app/src/main/java/com/securechat/ui/screens/home/HomeScreen.kt`

```kotlin
private fun RoomItem(
    room: ChatRoom,
    displayName: String,
    displayPhoto: String?,
    currentUserId: String,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val lastMessage = room.lastMessage
    val isMine = lastMessage?.senderId == currentUserId
    val isIncomingUnread = lastMessage != null && 
                           !isMine && 
                           !lastMessage.seenBy.contains(currentUserId)
    
    val previewText = when {
        lastMessage == null -> "Chưa có tin nhắn"
        isMine -> "Bạn: ${lastMessage.content}"  // ← Prefix for own messages
        else -> lastMessage.content
    }
    
    ListItem(
        supportingContent = {
            Text(
                text = previewText,
                color = if (isMine) Color.Gray.copy(alpha = 0.75f) else Color.Gray,
                fontWeight = if (isIncomingUnread) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        // ...
    )
}
```

---

## 14. Long-Press Delete Conversation

**File:** `app/src/main/java/com/securechat/ui/screens/home/HomeScreen.kt`

```kotlin
ListItem(
    // ...
    modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onLongClick() }
            )
        }
        .background(
            if (isDeleting) Color.Red.copy(alpha = 0.2f) 
            else Color.Transparent
        )
    // ...
)

// Confirmation dialog
if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Xóa cuộc trò chuyện") },
        text = { Text("Xóa cuộc trò chuyện này và tất cả tin nhắn?") },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteConversation(room.id)
                    showDeleteDialog = false
                }
            ) { Text("Xóa") }
        },
        dismissButton = {
            Button(onClick = { showDeleteDialog = false }) { Text("Hủy") }
        }
    )
}
```

---

## Summary Table

| Feature | File | Key Lines | Status |
|---------|------|-----------|--------|
| Firestore Rules (role-scoped ICE) | `firestore.rules` | 61-77 | ✅ |
| Transceiver SEND_RECV | `WebRTCManager.kt` | 195-205 | ✅ |
| Camera Fallback (Camera2→Camera1) | `WebRTCManager.kt` | 115-143 | ✅ |
| SDP Media Section Logging | `WebRTCManager.kt` | 351-390 | ✅ |
| Role-Based ICE Initialization | `WebRTCManager.kt` | 208-260 | ✅ |
| Async SDP Handling | `WebRTCManager.kt` | 245-252 | ✅ |
| Batch ICE Flushing | `WebRTCManager.kt` | 408-427 | ✅ |
| Remote Track Attachment | `WebRTCManager.kt` | 392-402 | ✅ |
| Role-Scoped ICE Repository | `CallRepositoryImpl.kt` | 39-42 | ✅ |
| Send ICE Candidate (role-based) | `CallRepositoryImpl.kt` | 130-138 | ✅ |
| Observe ICE Candidates (role-based) | `CallRepositoryImpl.kt` | 140-169 | ✅ |
| Call Status Real-Time Sync | `CallRepositoryImpl.kt` | 84-95 | ✅ |
| Message Delivery/Read Status | `Models.kt` | N/A | ✅ |
| Auto-Acknowledge Messages | `ChatViewModel.kt` | N/A | ✅ |
| Display Message Status | `ChatScreen.kt` | N/A | ✅ |
| List Preview with Status | `HomeScreen.kt` | N/A | ✅ |
| Long-Press Delete | `HomeScreen.kt` | N/A | ✅ |
| Auto-End on Remote Disconnect | `CallViewModel.kt` | N/A | ✅ |

---

**All implementations complete and ready for deployment.**

