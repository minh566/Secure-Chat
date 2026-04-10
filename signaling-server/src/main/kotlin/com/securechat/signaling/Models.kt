package com.securechat.signaling

import kotlinx.serialization.Serializable

@Serializable
data class CallSessionDto(
    val sessionId: String,
    val callerId: String,
    val calleeId: String,
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

