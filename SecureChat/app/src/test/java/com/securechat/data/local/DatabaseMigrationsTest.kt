package com.securechat.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseMigrationsTest {

    @Test
    fun migration_happyPath_hasExpectedVersionRange() {
        assertEquals(1, DatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, DatabaseMigrations.MIGRATION_1_2.endVersion)
    }

    @Test
    fun migration_failurePath_noUnexpectedMigrationsRegistered() {
        assertEquals(1, DatabaseMigrations.ALL.size)
        assertTrue(DatabaseMigrations.ALL.first() === DatabaseMigrations.MIGRATION_1_2)
    }
}

