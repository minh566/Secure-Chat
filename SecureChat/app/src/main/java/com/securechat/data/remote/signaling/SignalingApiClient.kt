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
