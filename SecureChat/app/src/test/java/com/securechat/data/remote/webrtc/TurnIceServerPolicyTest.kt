package com.securechat.data.remote.webrtc

import com.securechat.data.remote.signaling.TurnCredentials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TurnIceServerPolicyTest {

    @Test
    fun resolve_happyPath_withTurnCredentials_includesTurnAndStun() {
        val creds = TurnCredentials(
            urls = listOf("turn:turn.example.com:3478?transport=udp"),
            username = "user",
            credential = "pass",
            ttl = 300L
        )

        val specs = TurnIceServerPolicy.resolve(creds)

        assertTrue(specs.any { it.url.startsWith("stun:") })
        assertTrue(specs.any { it.url.startsWith("turn:") && it.username == "user" })
    }

    @Test
    fun resolve_failure_nullCredentials_fallsBackToStunOnly() {
        val specs = TurnIceServerPolicy.resolve(null)

        assertEquals(3, specs.size)
        assertTrue(specs.all { it.url.startsWith("stun:") })
    }
}

