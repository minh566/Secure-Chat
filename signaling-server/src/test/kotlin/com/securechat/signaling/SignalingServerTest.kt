package com.securechat.signaling

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SignalingServerTest {

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun turnCredentialProvider_happyPath_returnsCredentials() {
        val env = mapOf(
            "TURN_URLS" to "turn:turn.example.com:3478?transport=udp,turns:turn.example.com:5349",
            "TURN_USERNAME" to "demoUser",
            "TURN_CREDENTIAL" to "demoPass",
            "TURN_TTL_SECONDS" to "600"
        )

        val credentials = TurnCredentialProvider.fromEnvironment(env)

        assertNotNull(credentials)
        assertEquals(2, credentials.urls.size)
        assertEquals("demoUser", credentials.username)
        assertEquals(600L, credentials.ttl)
    }

    @Test
    fun turnCredentialProvider_failure_missingEnv_returnsNull() {
        val env = mapOf(
            "TURN_URLS" to "",
            "TURN_USERNAME" to "",
            "TURN_CREDENTIAL" to ""
        )

        val credentials = TurnCredentialProvider.fromEnvironment(env)

        assertNull(credentials)
    }

    @Test
    fun callInvitePayloadFactory_happyPath_containsRequiredKeys() {
        val payload = CallInvitePayloadFactory.build(
            CallSessionDto(
                sessionId = "session-1",
                callerId = "userA",
                calleeId = "userB",
                roomId = "room-1",
                callerName = "Tan",
                type = "VIDEO"
            )
        )

        assertEquals("INCOMING_CALL", payload["type"])
        assertEquals("userA", payload["callerId"])
        assertEquals("Tan", payload["callerName"])
        assertEquals("VIDEO", payload["callType"])
        assertEquals("room-1", payload["roomId"])
        assertEquals("session-1", payload["sessionId"])
    }

    @Test
    fun newMessagePayloadFactory_happyPath_containsRequiredKeys() {
        val payload = NewMessagePayloadFactory.build(
            NewMessagePushRequestDto(
                roomId = "room-1",
                roomName = "Tan",
                senderId = "userA",
                senderName = "Tan",
                content = "hello",
                recipientIds = listOf("userB")
            )
        )

        assertEquals("NEW_MESSAGE", payload["type"])
        assertEquals("room-1", payload["roomId"])
        assertEquals("Tan", payload["roomName"])
        assertEquals("userA", payload["senderId"])
        assertEquals("Tan", payload["senderName"])
        assertEquals("hello", payload["content"])
    }

    @Test
    fun offlineCallInvitePolicy_failure_whenAlreadyDelivered_returnsFalse() {
        val shouldSend = OfflineCallInvitePolicy.shouldSendFallback(
            eventType = "offer",
            delivered = true
        )

        assertFalse(shouldSend)
    }

    @Test
    fun offlineCallInvitePolicy_happyPath_offerUndelivered_returnsTrue() {
        val shouldSend = OfflineCallInvitePolicy.shouldSendFallback(
            eventType = "offer",
            delivered = false
        )

        assertTrue(shouldSend)
    }

    @Test
    fun sfuConfigProvider_happyPath_returnsConfig() {
        val env = mapOf(
            "LIVEKIT_WS_URL" to "wss://livekit.example.com",
            "LIVEKIT_API_KEY" to "lk_key",
            "LIVEKIT_API_SECRET" to "lk_secret",
            "LIVEKIT_TOKEN_TTL_SECONDS" to "120"
        )

        val config = SfuConfigProvider.fromEnvironment(env)

        assertNotNull(config)
        assertEquals("wss://livekit.example.com", config.wsUrl)
        assertEquals(120L, config.tokenTtlSeconds)
    }

    @Test
    fun liveKitTokenFactory_happyPath_containsJoinGrant() {
        val token = LiveKitTokenFactory.createJoinToken(
            apiKey = "lk_key",
            apiSecret = "lk_secret",
            roomName = "room-alpha",
            identity = "user-1",
            participantName = "Tan",
            ttlSeconds = 300
        )

        val verifier = JWT.require(Algorithm.HMAC256("lk_secret"))
            .withIssuer("lk_key")
            .withAudience("livekit")
            .build()
        val decoded = verifier.verify(token)

        assertEquals("user-1", decoded.subject)
        assertEquals("Tan", decoded.getClaim("name").asString())
        val video = decoded.getClaim("video").asMap() as Map<String, Any?>
        assertEquals("room-alpha", video["room"])
        assertEquals(true, video["roomJoin"])
    }
}

