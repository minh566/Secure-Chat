package com.securechat.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    // v1 -> v2: add attachment/read-receipt columns to existing messages rows.
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN fileUrl TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN fileName TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE messages ADD COLUMN deliveredToCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE messages ADD COLUMN seenByCsv TEXT NOT NULL DEFAULT ''")
        }
    }

    // v2 -> v3: add reactions metadata for message-level emoji reacts.
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN reactionsCsv TEXT NOT NULL DEFAULT ''")
        }
    }

    // v3 -> v4: persist local cached attachment path for offline/fast open.
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN localCachePath TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}

