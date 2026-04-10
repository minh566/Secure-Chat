package com.securechat.di

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseConfigPolicyTest {

    @Test
    fun shouldUseDestructiveMigration_debugBuild_returnsTrue() {
        assertTrue(DatabaseConfigPolicy.shouldUseDestructiveMigration(isDebug = true))
    }

    @Test
    fun shouldUseDestructiveMigration_releaseBuild_returnsFalse() {
        assertFalse(DatabaseConfigPolicy.shouldUseDestructiveMigration(isDebug = false))
    }
}

