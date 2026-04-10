package com.securechat.di

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthTestingConfigTest {

    @Test
    fun shouldDisableAppVerification_debugBuild_returnsTrue() {
        assertTrue(AuthTestingConfig.shouldDisableAppVerification(isDebug = true))
    }

    @Test
    fun shouldDisableAppVerification_releaseBuild_returnsFalse() {
        assertFalse(AuthTestingConfig.shouldDisableAppVerification(isDebug = false))
    }
}

