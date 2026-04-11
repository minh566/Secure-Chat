package com.example.securechat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securechat.presentation.chat.ChatUiState
import com.example.securechat.presentation.chat.UiMessage
import com.example.securechat.presentation.chat.MsgStatus
import com.example.securechat.presentation.video.VideoCallUiState
import com.example.securechat.presentation.video.VideoParticipant
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// ── Stub ChatViewModel ────────────────────────────────────────────────────────
class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                UiMessage(
                    id = "1",
                    senderId = "user2",
                    senderName = "Minh Anh",
                    senderInitial = "MA",
                    senderColor = Color(0xFFE91E8C),
                    content = "Chào mọi người! Họp lúc 3h chiều nha 👋",
                    timestamp = System.currentTimeMillis() - 300000,
                    isSentByMe = false
                ),
                UiMessage(
                    id = "2",
                    senderId = "me",
                    senderName = "Tôi",
                    senderInitial = "T",
                    senderColor = Color(0xFF0084FF),
                    content = "Ok mình tham gia được nha 👍",
                    timestamp = System.currentTimeMillis() - 240000,
                    isSentByMe = true,
                    status = MsgStatus.READ
                ),
                UiMessage(
                    id = "3",
                    senderId = "user3",
                    senderName = "Tuấn",
                    senderInitial = "Tu",
                    senderColor = Color(0xFF9C27B0),
                    content = "Mình cũng ok! Họp qua Google Meet hay Zoom?",
                    timestamp = System.currentTimeMillis() - 180000,
                    isSentByMe = false
                ),
                UiMessage(
                    id = "4",
                    senderId = "me",
                    senderName = "Tôi",
                    senderInitial = "T",
                    senderColor = Color(0xFF0084FF),
                    content = "Meet nha mọi người. Link mình gửi sau",
                    timestamp = System.currentTimeMillis() - 120000,
                    isSentByMe = true,
                    status = MsgStatus.DELIVERED
                ),
            ),
            typingUsers = emptySet()
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun initGroup(groupId: String) {
        // Stub: không làm gì, data đã có sẵn ở trên
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val newMsg = UiMessage(
            id = System.currentTimeMillis().toString(),
            senderId = "me",
            senderName = "Tôi",
            senderInitial = "T",
            senderColor = Color(0xFF0084FF),
            content = text,
            timestamp = System.currentTimeMillis(),
            isSentByMe = true,
            status = MsgStatus.SENDING
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + newMsg,
            inputText = ""
        )

        // Giả lập reply sau 1.5 giây
        viewModelScope.launch {
            delay(1500)
            val reply = UiMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                senderId = "user2",
                senderName = "Minh Anh",
                senderInitial = "MA",
                senderColor = Color(0xFFE91E8C),
                content = "Ok mình hiểu rồi! 👌",
                timestamp = System.currentTimeMillis(),
                isSentByMe = false
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + reply
            )
        }
    }
}

// ── Stub VideoCallViewModel ───────────────────────────────────────────────────
class VideoCallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        VideoCallUiState(
            remoteParticipants = listOf(
                VideoParticipant(
                    id = "p1", name = "Minh Anh",
                    initial = "MA", avatarColor = Color(0xFFE91E8C),
                    isSpeaking = true
                ),
                VideoParticipant(
                    id = "p2", name = "Tuấn",
                    initial = "Tu", avatarColor = Color(0xFF9C27B0),
                    isMuted = true
                ),
                VideoParticipant(
                    id = "p3", name = "Linh",
                    initial = "Li", avatarColor = Color(0xFFFF5722)
                ),
            ),
            isMuted = false,
            isCameraOff = false,
            isSpeakerOn = true
        )
    )
    val uiState: StateFlow<VideoCallUiState> = _uiState.asStateFlow()

    fun joinRoom(roomId: String) {
        // Stub: data đã có sẵn
    }

    fun toggleMicrophone() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(isCameraOff = !_uiState.value.isCameraOff)
    }

    fun switchCamera() {
        // Stub: không làm gì khi demo
    }

    fun toggleSpeaker() {
        _uiState.value = _uiState.value.copy(isSpeakerOn = !_uiState.value.isSpeakerOn)
    }

    fun endCall() {
        // Stub: chỉ navigate back
    }

    fun initLocalRenderer(renderer: Any) {
        // Stub: không cần WebRTC khi demo
    }

    fun initRemoteRenderer(peerId: String, renderer: Any) {
        // Stub: không cần WebRTC khi demo
    }
}

// ── Stub HomeViewModel ────────────────────────────────────────────────────────
class HomeViewModel : ViewModel() {
    // HomeScreen dùng data tĩnh sampleGroups nên không cần gì thêm
}

// ── Stub AuthViewModel ────────────────────────────────────────────────────────
class AuthViewModel : ViewModel() {
    // LoginScreen tự handle UI state, không cần ViewModel phức tạp
}