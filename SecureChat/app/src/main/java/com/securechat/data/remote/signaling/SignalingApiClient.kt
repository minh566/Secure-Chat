package com.securechat.data.remote.signaling

import com.securechat.domain.model.CallSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val baseHttpUrl: String
) {

    fun pushNewMessage(
        roomId: String,
        roomName: String,
        senderId: String,
        senderName: String,
        content: String,
        recipientIds: List<String>
    ): Boolean {
        if (recipientIds.isEmpty()) return true

        val body = JSONObject()
            .put("type", "NEW_MESSAGE")
            .put("roomId", roomId)
            .put("roomName", roomName)
            .put("senderId", senderId)
            .put("senderName", senderName)
            .put("content", content)
            .put("recipientIds", JSONArray(recipientIds))
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseHttpUrl/notifications/new-message")
            .post(body)
            .build()

        return executeSuccess(request)
    }

    fun fetchSfuAccessToken(
        roomName: String,
        identity: String,
        participantName: String?
    ): SfuAccessToken? {
        val body = JSONObject()
            .put("roomName", roomName)
            .put("identity", identity)
            .put("participantName", participantName)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseHttpUrl/sfu/token")
            .post(body)
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return null
                val json = JSONObject(raw)
                val wsUrl = json.optString("wsUrl").trim()
                val token = json.optString("token").trim()
                val responseRoomName = json.optString("roomName").trim()
                val responseIdentity = json.optString("identity").trim()
                val expiresInSeconds = json.optLong("expiresInSeconds", 0L)
                if (wsUrl.isBlank() || token.isBlank() || responseRoomName.isBlank() || responseIdentity.isBlank()) {
                    return null
                }
                SfuAccessToken(
                    wsUrl = wsUrl,
                    token = token,
                    roomName = responseRoomName,
                    identity = responseIdentity,
                    expiresInSeconds = expiresInSeconds
                )
            }
        }.getOrNull()
    }

    fun createCall(session: CallSession): Boolean {
        val body = JSONObject()
            .put("sessionId", session.id)
            .put("callerId", session.callerId)
            .put("calleeId", session.calleeId)
            .put("roomId", session.roomId)
            .put("type", session.type.name)
            .put("status", session.status.name)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseHttpUrl/calls")
            .post(body)
            .build()

        return executeSuccess(request)
    }

    fun updateCallStatus(sessionId: String, status: String): Boolean {
        val body = JSONObject()
            .put("status", status)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseHttpUrl/calls/$sessionId/status")
            .post(body)
            .build()

        return executeSuccess(request)
    }

    fun fetchTurnCredentials(): TurnCredentials? {
        val request = Request.Builder()
            .url("$baseHttpUrl/turn-credentials")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return null
                parseTurnCredentials(JSONObject(raw))
            }
        }.getOrNull()
    }

    private fun executeSuccess(request: Request): Boolean {
        return runCatching {
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun parseTurnCredentials(json: JSONObject): TurnCredentials? {
        val urls = json.optJSONArray("urls").toStringList().filter { it.isNotBlank() }
        val username = json.optString("username").trim()
        val credential = json.optString("credential").trim()
        val ttl = json.optLong("ttl", 3600L)

        if (urls.isEmpty() || username.isBlank() || credential.isBlank()) {
            return null
        }

        return TurnCredentials(
            urls = urls,
            username = username,
            credential = credential,
            ttl = ttl
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val value = optString(i)
                if (!value.isNullOrBlank()) add(value.trim())
            }
        }
    }
}

data class SfuAccessToken(
    val wsUrl: String,
    val token: String,
    val roomName: String,
    val identity: String,
    val expiresInSeconds: Long
)

