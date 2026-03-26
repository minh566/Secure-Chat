package com.securechat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: String,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val isRead: Boolean = false,
    val deliveredToCsv: String = "",
    val seenByCsv: String = "",
    val createdAt: Long
)
