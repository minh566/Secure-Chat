package com.securechat.domain.model

import java.util.Date

// ── User ────────────────────────────────────────────────────────────────────
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val fcmToken: String? = null,
    val createdAt: Date = Date()
)

// ── Chat Room ────────────────────────────────────────────────────────────────
data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val members: List<String> = emptyList(),     // list of UIDs
    val isGroup: Boolean = false,
    val photoUrl: String? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: Date = Date()
)

// ── Message ──────────────────────────────────────────────────────────────────
data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val isRead: Boolean = false,
    val createdAt: Date = Date()
)

enum class MessageType { TEXT, IMAGE, FILE, CALL_LOG }

// ── Call ─────────────────────────────────────────────────────────────────────
data class CallSession(
    val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val roomId: String = "",
    val type: CallType = CallType.VIDEO,
    val status: CallStatus = CallStatus.RINGING,
    val startedAt: Date? = null,
    val endedAt: Date? = null
)

enum class CallType   { VIDEO, AUDIO }
enum class CallStatus { RINGING, ACCEPTED, DECLINED, ENDED, MISSED }

// ── Auth result wrapper ───────────────────────────────────────────────────────
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

// ── Generic Resource wrapper (cho Coroutines Flow) ────────────────────────────
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
