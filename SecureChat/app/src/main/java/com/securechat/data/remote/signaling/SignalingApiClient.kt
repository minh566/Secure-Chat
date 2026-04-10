package com.securechat.data.remote.signaling

import com.securechat.domain.model.CallSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private fun executeSuccess(request: Request): Boolean {
        return runCatching {
            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}

