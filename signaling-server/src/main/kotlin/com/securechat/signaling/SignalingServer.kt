package com.securechat.signaling

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
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

    val callInviteNotifier = FirebaseCallInviteNotifier(
        firebaseApp = FirebaseAdminProvider.initializeFromEnvironment()
    )
    val appLogger = this.log

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        get("/turn-credentials") {
            val credentials = TurnCredentialProvider.fromEnvironment()
            if (credentials == null) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "TURN credentials are not configured")
                )
            } else {
                call.respond(HttpStatusCode.OK, credentials)
            }
        }

        post("/calls") {
            val payload = call.receive<CallSessionDto>()
            sessionsById[payload.sessionId] = payload
            val delivered = relayToUser(
                payload.calleeId,
                SignalingEnvelopeDto(
                    type = "incoming_call",
                    sessionId = payload.sessionId,
                    fromUserId = payload.callerId,
                    toUserId = payload.calleeId,
                    status = payload.status
                )
            )
            maybeSendOfflineInvite(
                eventType = "incoming_call",
                delivered = delivered,
                calleeId = payload.calleeId,
                session = payload,
                notifier = callInviteNotifier,
                logger = { message -> appLogger.warn(message) }
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
                        val delivered = relayToUser(targetUserId, envelope)
                        if (envelope.type == "offer") {
                            val session = sessionsById[envelope.sessionId]
                            if (session != null) {
                                maybeSendOfflineInvite(
                                    eventType = "offer",
                                    delivered = delivered,
                                    calleeId = targetUserId,
                                    session = session,
                                    notifier = callInviteNotifier,
                                    logger = { message -> appLogger.warn(message) }
                                )
                            }
                        }
                    }
                }
            } finally {
                userSockets.remove(this)
                if (userSockets.isEmpty()) socketsByUser.remove(userId)
            }
        }
    }
}

internal object FirebaseAdminProvider {
    fun initializeFromEnvironment(env: Map<String, String> = System.getenv()): FirebaseApp? {
        FirebaseApp.getApps().firstOrNull()?.let { return it }

        val credentials = credentialsFromEnvironment(env) ?: return null
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return runCatching { FirebaseApp.initializeApp(options) }.getOrNull()
    }

    private fun credentialsFromEnvironment(env: Map<String, String>): GoogleCredentials? {
        val json = env["FIREBASE_SERVICE_ACCOUNT_JSON"]?.trim().orEmpty()
        if (json.isNotBlank()) {
            return runCatching {
                ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)).use { stream ->
                    GoogleCredentials.fromStream(stream)
                }
            }.getOrNull()
        }

        val path = env["FIREBASE_SERVICE_ACCOUNT_PATH"]
            ?: env["GOOGLE_APPLICATION_CREDENTIALS"]
            ?: return null
        val file = File(path)
        if (!file.exists()) return null

        return runCatching {
            file.inputStream().use { stream -> GoogleCredentials.fromStream(stream) }
        }.getOrNull()
    }
}

internal class FirebaseCallInviteNotifier(
    private val firebaseApp: FirebaseApp?
) {
    fun sendIncomingCallInvite(calleeId: String, session: CallSessionDto): Boolean {
        val app = firebaseApp ?: return false

        val token = runCatching {
            FirestoreClient.getFirestore(app)
                .collection("users")
                .document(calleeId)
                .get()
                .get()
                .getString("fcmToken")
                ?.trim()
                .orEmpty()
        }.getOrDefault("")

        if (token.isBlank()) return false

        val payload = CallInvitePayloadFactory.build(session)
        val message = com.google.firebase.messaging.Message.builder()
            .setToken(token)
            .putAllData(payload)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(30_000)
                    .build()
            )
            .build()

        return runCatching {
            FirebaseMessaging.getInstance(app).send(message)
            true
        }.getOrDefault(false)
    }
}

internal object CallInvitePayloadFactory {
    fun build(session: CallSessionDto): Map<String, String> {
        val callerName = session.callerName?.takeIf { it.isNotBlank() } ?: session.callerId
        return mapOf(
            "type" to "INCOMING_CALL",
            "callerId" to session.callerId,
            "callerName" to callerName,
            "callType" to session.type,
            "roomId" to session.roomId,
            "sessionId" to session.sessionId
        )
    }
}

internal object OfflineCallInvitePolicy {
    fun shouldSendFallback(eventType: String, delivered: Boolean): Boolean {
        return !delivered && (eventType == "incoming_call" || eventType == "offer")
    }
}

internal fun maybeSendOfflineInvite(
    eventType: String,
    delivered: Boolean,
    calleeId: String,
    session: CallSessionDto,
    notifier: FirebaseCallInviteNotifier,
    logger: (String) -> Unit
) {
    if (!OfflineCallInvitePolicy.shouldSendFallback(eventType, delivered)) return

    val sent = notifier.sendIncomingCallInvite(calleeId, session)
    if (!sent) {
        logger("FCM fallback skipped/failed for event=$eventType session=${session.sessionId} callee=$calleeId")
    }
}

internal object TurnCredentialProvider {
    fun fromEnvironment(env: Map<String, String> = System.getenv()): TurnCredentialsDto? {
        val urls = env["TURN_URLS"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val username = env["TURN_USERNAME"]?.trim().orEmpty()
        val credential = env["TURN_CREDENTIAL"]?.trim().orEmpty()
        val ttl = env["TURN_TTL_SECONDS"]?.toLongOrNull() ?: 3600L

        if (urls.isEmpty() || username.isBlank() || credential.isBlank()) {
            return null
        }

        return TurnCredentialsDto(
            urls = urls,
            username = username,
            credential = credential,
            ttl = ttl
        )
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

private suspend fun relayToUser(userId: String, envelope: SignalingEnvelopeDto): Boolean {
    val sockets = socketsByUser[userId] ?: return false
    val payload = Json.encodeToString(envelope)
    val invalidSockets = mutableListOf<DefaultWebSocketServerSession>()
    var deliveredCount = 0

    sockets.forEach { socket ->
        try {
            socket.send(Frame.Text(payload))
            deliveredCount++
        } catch (_: ClosedSendChannelException) {
            invalidSockets.add(socket)
        } catch (_: Throwable) {
            invalidSockets.add(socket)
        }
    }

    if (invalidSockets.isNotEmpty()) {
        sockets.removeAll(invalidSockets.toSet())
    }

    return deliveredCount > 0
}

