package com.securechat.data.remote.signaling

import org.json.JSONObject

data class SignalingEnvelope(
    val type: String,
    val sessionId: String,
    val fromUserId: String,
    val toUserId: String? = null,
    val sdp: String? = null,
    val candidate: String? = null,
    val role: String? = null,
    val status: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonString(): String {
        val json = JSONObject()
            .put("type", type)
            .put("sessionId", sessionId)
            .put("fromUserId", fromUserId)
            .put("timestamp", timestamp)

        toUserId?.let { json.put("toUserId", it) }
        sdp?.let { json.put("sdp", it) }
        candidate?.let { json.put("candidate", it) }
        role?.let { json.put("role", it) }
        status?.let { json.put("status", it) }
        return json.toString()
    }

    companion object {
        fun fromJsonString(raw: String): SignalingEnvelope? {
            return runCatching {
                val json = JSONObject(raw)
                SignalingEnvelope(
                    type = json.getString("type"),
                    sessionId = json.getString("sessionId"),
                    fromUserId = json.getString("fromUserId"),
                    toUserId = json.optString("toUserId").takeIf { it.isNotBlank() },
                    sdp = json.optString("sdp").takeIf { it.isNotBlank() },
                    candidate = json.optString("candidate").takeIf { it.isNotBlank() },
                    role = json.optString("role").takeIf { it.isNotBlank() },
                    status = json.optString("status").takeIf { it.isNotBlank() },
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            }.getOrNull()
        }
    }
}

