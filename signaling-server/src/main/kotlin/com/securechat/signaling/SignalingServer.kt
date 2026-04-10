package com.securechat.signaling

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private val sessionsById = ConcurrentHashMap<String, CallSessionDto>()
private val socketsByUser = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets)

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        post("/calls") {
            val payload = call.receive<CallSessionDto>()
            sessionsById[payload.sessionId] = payload
            relayToUser(
                payload.calleeId,
                SignalingEnvelopeDto(
                    type = "incoming_call",
                    sessionId = payload.sessionId,
                    fromUserId = payload.callerId,
                    toUserId = payload.calleeId,
                    status = payload.status
                )
            )
            call.respond(HttpStatusCode.Created, mapOf("ok" to true, "sessionId" to payload.sessionId))
        }

        post("/calls/{sessionId}/status") {
            val sessionId = call.parameters["sessionId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val update = call.receive<UpdateStatusDto>()
            val current = sessionsById[sessionId]
            if (current != null) {
                val updated = current.copy(status = update.status)
                sessionsById[sessionId] = updated
                relayToUser(
                    updated.calleeId,
                    SignalingEnvelopeDto(
                        type = "call_status",
                        sessionId = sessionId,
                        fromUserId = updated.callerId,
                        toUserId = updated.calleeId,
                        status = update.status
                    )
                )
                relayToUser(
                    updated.callerId,
                    SignalingEnvelopeDto(
                        type = "call_status",
                        sessionId = sessionId,
                        fromUserId = updated.calleeId,
                        toUserId = updated.callerId,
                        status = update.status
                    )
                )
            }
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }

        webSocket("/ws") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing userId"))
                return@webSocket
            }

            val userSockets = socketsByUser.computeIfAbsent(userId) {
                Collections.newSetFromMap(ConcurrentHashMap())
            }
            userSockets.add(this)

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val envelope = runCatching {
                        Json.decodeFromString<SignalingEnvelopeDto>(frame.readText())
                    }.getOrNull() ?: continue

                    val targetUserId = envelope.toUserId ?: inferTargetUserId(envelope)
                    if (targetUserId != null) {
                        relayToUser(targetUserId, envelope)
                    }
                }
            } finally {
                userSockets.remove(this)
                if (userSockets.isEmpty()) socketsByUser.remove(userId)
            }
        }
    }
}

private fun inferTargetUserId(envelope: SignalingEnvelopeDto): String? {
    val session = sessionsById[envelope.sessionId] ?: return null
    return when (envelope.fromUserId) {
        session.callerId -> session.calleeId
        session.calleeId -> session.callerId
        else -> null
    }
}

private suspend fun relayToUser(userId: String, envelope: SignalingEnvelopeDto) {
    val sockets = socketsByUser[userId] ?: return
    val payload = Json.encodeToString(SignalingEnvelopeDto.serializer(), envelope)
    val invalidSockets = mutableListOf<DefaultWebSocketServerSession>()

    sockets.forEach { socket ->
        try {
            socket.send(Frame.Text(payload))
        } catch (_: ClosedSendChannelException) {
            invalidSockets.add(socket)
        } catch (_: Throwable) {
            invalidSockets.add(socket)
        }
    }

    if (invalidSockets.isNotEmpty()) {
        sockets.removeAll(invalidSockets.toSet())
    }
}

