package com.securechat.signaling

import kotlinx.serialization.Serializable

@Serializable
data class CallSessionDto(
    val sessionId: String,
    val callerId: String,
    val calleeId: String,
    val roomId: String = "",
    val callerName: String? = null,
    val type: String = "VIDEO",
    val status: String = "RINGING",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class UpdateStatusDto(
    val status: String
)

@Serializable
data class SignalingEnvelopeDto(
    val type: String,
    val sessionId: String,
    val fromUserId: String,
    val toUserId: String? = null,
    val sdp: String? = null,
    val candidate: String? = null,
    val role: String? = null,
    val status: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TurnCredentialsDto(
    val urls: List<String>,
    val username: String,
    val credential: String,
    val ttl: Long
)

@Serializable
data class SfuTokenRequestDto(
    val roomName: String,
    val identity: String,
    val participantName: String? = null
)

@Serializable
data class SfuTokenResponseDto(
    val wsUrl: String,
    val token: String,
    val roomName: String,
    val identity: String,
    val expiresInSeconds: Long
)

@Serializable
data class NewMessagePushRequestDto(
    val roomId: String,
    val roomName: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val recipientIds: List<String>,
    val type: String = "NEW_MESSAGE"
)

@Serializable
data class NewMessagePushResponseDto(
    val requested: Int,
    val sent: Int,
    val failed: Int,
    val cleanedInvalidTokens: Int
)

