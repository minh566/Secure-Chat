package com.securechat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.securechat.data.local.dao.MessageDao
import com.securechat.data.local.entity.MessageEntity

@Database(
    entities  = [MessageEntity::class],
    version   = 4,
    exportSchema = false
)
abstract class SecureChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
