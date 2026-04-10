package com.securechat.data.remote.webrtc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RendererReleasePolicyTest {

    @Test
    fun shouldReleaseEgl_happyPath_bothRenderersReleased_returnsTrue() {
        val shouldRelease = RendererReleasePolicy.shouldReleaseEgl(
            localReleased = true,
            remoteReleased = true
        )

        assertTrue(shouldRelease)
    }

    @Test
    fun shouldReleaseEgl_failurePath_oneRendererStillAlive_returnsFalse() {
        val shouldRelease = RendererReleasePolicy.shouldReleaseEgl(
            localReleased = true,
            remoteReleased = false
        )

        assertFalse(shouldRelease)
    }
}

