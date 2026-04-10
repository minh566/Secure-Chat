package com.securechat.di.call

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignalingUrlConfigValidatorTest {

    @Test
    fun validate_happyPath_prodWithHttpsWss_returnsNormalizedUrls() {
        val (http, ws) = SignalingUrlConfigValidator.validate(
            httpUrl = " https://signal.example.com ",
            wsUrl = " wss://signal.example.com/ws ",
            flavor = "prod"
        )

        assertEquals("https://signal.example.com", http)
        assertEquals("wss://signal.example.com/ws", ws)
    }

    @Test
    fun validate_failure_prodWithCleartext_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            SignalingUrlConfigValidator.validate(
                httpUrl = "http://10.0.2.2:8081",
                wsUrl = "ws://10.0.2.2:8081/ws",
                flavor = "prod"
            )
        }
    }
}

