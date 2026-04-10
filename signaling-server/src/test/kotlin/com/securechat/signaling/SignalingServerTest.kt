package com.securechat.signaling

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
}

