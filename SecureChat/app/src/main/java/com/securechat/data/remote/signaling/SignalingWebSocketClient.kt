package com.securechat.data.remote.signaling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val wsBaseUrl: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val socketRef = AtomicReference<WebSocket?>(null)
    private val connectedUserRef = AtomicReference<String?>(null)

    private val _events = MutableSharedFlow<SignalingEnvelope>(extraBufferCapacity = 128)
    val events: SharedFlow<SignalingEnvelope> = _events.asSharedFlow()

    fun connect(userId: String) {
        if (userId.isBlank()) return
        if (connectedUserRef.get() == userId && socketRef.get() != null) return

        disconnect()
        connectedUserRef.set(userId)

        val request = Request.Builder()
            .url("$wsBaseUrl?userId=$userId")
            .build()

        val socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val envelope = SignalingEnvelope.fromJsonString(text) ?: return
                scope.launch { _events.emit(envelope) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socketRef.compareAndSet(webSocket, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socketRef.compareAndSet(webSocket, null)
            }
        })

        socketRef.set(socket)
    }

    fun send(envelope: SignalingEnvelope): Boolean {
        val socket = socketRef.get() ?: return false
        return socket.send(envelope.toJsonString())
    }

    fun disconnect() {
        socketRef.getAndSet(null)?.close(1000, "client-close")
        connectedUserRef.set(null)
    }
}

