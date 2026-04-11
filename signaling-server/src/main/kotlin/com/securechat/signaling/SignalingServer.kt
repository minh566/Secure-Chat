package com.securechat.signaling

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
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
import java.util.Date
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

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

        post("/sfu/token") {
            val payload = call.receive<SfuTokenRequestDto>()
            val config = SfuConfigProvider.fromEnvironment()
            if (config == null) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("error" to "SFU config is not ready")
                )
                return@post
            }

            val roomName = payload.roomName.trim()
            val identity = payload.identity.trim()
            if (roomName.isBlank() || identity.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "roomName and identity are required")
                )
                return@post
            }

            val displayName = payload.participantName?.trim().orEmpty().ifBlank { identity }
            val token = LiveKitTokenFactory.createJoinToken(
                apiKey = config.apiKey,
                apiSecret = config.apiSecret,
                roomName = roomName,
                identity = identity,
                participantName = displayName,
                ttlSeconds = config.tokenTtlSeconds
            )

            call.respond(
                HttpStatusCode.OK,
                SfuTokenResponseDto(
                    wsUrl = config.wsUrl,
                    token = token,
                    roomName = roomName,
                    identity = identity,
                    expiresInSeconds = config.tokenTtlSeconds
                )
            )
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

        post("/notifications/new-message") {
            val payload = call.receive<NewMessagePushRequestDto>()

            if (payload.roomId.isBlank() || payload.senderId.isBlank() || payload.senderName.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "roomId, senderId, senderName are required")
                )
                return@post
            }

            val recipients = payload.recipientIds
                .map { it.trim() }
                .filter { it.isNotBlank() && it != payload.senderId }
                .distinct()

            if (recipients.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "recipientIds must contain at least one user different from senderId")
                )
                return@post
            }

            val pushResult = callInviteNotifier.sendNewMessagePush(
                payload = payload,
                recipientIds = recipients,
                logger = { message -> appLogger.warn(message) },
                infoLogger = { message -> appLogger.info(message) }
            )

            call.respond(
                HttpStatusCode.OK,
                NewMessagePushResponseDto(
                    requested = recipients.size,
                    sent = pushResult.sent,
                    failed = pushResult.failed,
                    cleanedInvalidTokens = pushResult.cleanedInvalidTokens
                )
            )
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

internal data class SfuConfig(
    val wsUrl: String,
    val apiKey: String,
    val apiSecret: String,
    val tokenTtlSeconds: Long
)

internal object SfuConfigProvider {
    fun fromEnvironment(env: Map<String, String> = System.getenv()): SfuConfig? {
        val wsUrl = env["LIVEKIT_WS_URL"]?.trim().orEmpty()
        val apiKey = env["LIVEKIT_API_KEY"]?.trim().orEmpty()
        val apiSecret = env["LIVEKIT_API_SECRET"]?.trim().orEmpty()
        val tokenTtlSeconds = env["LIVEKIT_TOKEN_TTL_SECONDS"]?.trim()?.toLongOrNull() ?: 3600L

        if (wsUrl.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) return null

        return SfuConfig(
            wsUrl = wsUrl,
            apiKey = apiKey,
            apiSecret = apiSecret,
            tokenTtlSeconds = tokenTtlSeconds.coerceAtLeast(60L)
        )
    }
}

internal object LiveKitTokenFactory {
    fun createJoinToken(
        apiKey: String,
        apiSecret: String,
        roomName: String,
        identity: String,
        participantName: String,
        ttlSeconds: Long
    ): String {
        val now = System.currentTimeMillis()
        val expiresAt = Date(now + ttlSeconds * 1000)
        val videoGrant = mapOf(
            "roomJoin" to true,
            "room" to roomName,
            "canPublish" to true,
            "canSubscribe" to true
        )

        return JWT.create()
            .withIssuer(apiKey)
            .withSubject(identity)
            .withAudience("livekit")
            .withExpiresAt(expiresAt)
            .withClaim("name", participantName)
            .withClaim("video", videoGrant)
            .sign(Algorithm.HMAC256(apiSecret))
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

    fun sendNewMessagePush(
        payload: NewMessagePushRequestDto,
        recipientIds: List<String>,
        logger: (String) -> Unit,
        infoLogger: (String) -> Unit
    ): NewMessagePushResult {
        val app = firebaseApp ?: return NewMessagePushResult(
            sent = 0,
            failed = recipientIds.size,
            cleanedInvalidTokens = 0
        )

        var sentCount = 0
        var failedCount = 0
        var cleanedCount = 0
        val firestore = FirestoreClient.getFirestore(app)

        recipientIds.forEach { uid ->
            val token = runCatching {
                firestore.collection("users")
                    .document(uid)
                    .get()
                    .get()
                    .getString("fcmToken")
                    ?.trim()
                    .orEmpty()
            }.getOrDefault("")

            infoLogger(
                "NEW_MESSAGE token-check room=${payload.roomId} to=$uid hasToken=${token.isNotBlank()} token=${token.maskForLogs()}"
            )

            if (token.isBlank()) {
                failedCount++
                logger("NEW_MESSAGE skipped room=${payload.roomId} to=$uid reason=missing_fcm_token")
                return@forEach
            }

            val message = com.google.firebase.messaging.Message.builder()
                .setToken(token)
                .putAllData(NewMessagePayloadFactory.build(payload))
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setCollapseKey("chat_${payload.roomId}")
                        .setTtl(120_000)
                        .build()
                )
                .build()

            val sendResult = sendWithRetry(app, message, maxAttempts = 3)
            when {
                sendResult.success -> {
                    sentCount++
                    infoLogger("NEW_MESSAGE sent room=${payload.roomId} to=$uid")
                }
                sendResult.shouldCleanupToken -> {
                    failedCount++
                    logger(
                        "NEW_MESSAGE invalid-token room=${payload.roomId} to=$uid reason=${sendResult.reason ?: "unknown"}"
                    )
                    val cleaned = runCatching {
                        firestore.collection("users")
                            .document(uid)
                            .update("fcmToken", "")
                            .get()
                        true
                    }.getOrDefault(false)
                    if (cleaned) {
                        cleanedCount++
                        infoLogger("NEW_MESSAGE token-cleanup ok room=${payload.roomId} to=$uid")
                    } else {
                        logger("NEW_MESSAGE token-cleanup failed room=${payload.roomId} to=$uid")
                    }
                }
                else -> {
                    failedCount++
                    logger(
                        "NEW_MESSAGE push failed room=${payload.roomId} to=$uid reason=${sendResult.reason ?: "unknown"}"
                    )
                }
            }
        }

        return NewMessagePushResult(
            sent = sentCount,
            failed = failedCount,
            cleanedInvalidTokens = cleanedCount
        )
    }

    private fun sendWithRetry(
        app: FirebaseApp,
        message: com.google.firebase.messaging.Message,
        maxAttempts: Int
    ): SendAttemptResult {
        repeat(maxAttempts) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                FirebaseMessaging.getInstance(app).send(message)
                return SendAttemptResult(success = true)
            } catch (e: FirebaseMessagingException) {
                val code = e.messagingErrorCode
                if (code in INVALID_TOKEN_CODES) {
                    return SendAttemptResult(success = false, shouldCleanupToken = true, reason = code?.name)
                }

                val isTransient = code in TRANSIENT_CODES
                if (!isTransient || attempt == maxAttempts) {
                    return SendAttemptResult(success = false, reason = code?.name ?: e.errorCode.toString())
                }
                Thread.sleep(backoffWithJitter(attempt))
            } catch (e: Exception) {
                if (attempt == maxAttempts) {
                    return SendAttemptResult(success = false, reason = e.message)
                }
                Thread.sleep(backoffWithJitter(attempt))
            }
        }

        return SendAttemptResult(success = false, reason = "exhausted")
    }

    private fun backoffWithJitter(attempt: Int): Long {
        val base = 250L * (1L shl (attempt - 1).coerceAtMost(4))
        val jitter = Random.nextLong(50L, 180L)
        return (base + jitter).coerceAtMost(2000L)
    }

    private companion object {
        val INVALID_TOKEN_CODES = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT
        )

        val TRANSIENT_CODES = setOf(
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.QUOTA_EXCEEDED
        )
    }
}

private fun String.maskForLogs(): String {
    if (isBlank()) return "<empty>"
    if (length <= 12) return "***"
    return take(6) + "..." + takeLast(4)
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

internal object NewMessagePayloadFactory {
    fun build(payload: NewMessagePushRequestDto): Map<String, String> {
        return mapOf(
            "type" to "NEW_MESSAGE",
            "roomId" to payload.roomId,
            "roomName" to payload.roomName,
            "senderId" to payload.senderId,
            "senderName" to payload.senderName,
            "content" to payload.content
        )
    }
}

internal data class NewMessagePushResult(
    val sent: Int,
    val failed: Int,
    val cleanedInvalidTokens: Int
)

private data class SendAttemptResult(
    val success: Boolean,
    val shouldCleanupToken: Boolean = false,
    val reason: String? = null
)

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

