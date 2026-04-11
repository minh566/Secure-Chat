package com.securechat.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.securechat.data.local.entity.MessageEntity
import com.securechat.domain.model.Message
import com.securechat.domain.model.MessageType
import com.securechat.domain.model.User
import java.util.Date

private fun Map<String, String>.toReactionsCsv(): String =
    entries.joinToString(";") { (userId, emoji) -> "${userId.replace(";", "").replace(",", "")},$emoji" }

private fun String.toReactionsMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return split(';')
        .mapNotNull { pair ->
            val parts = pair.split(',', limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
        }
        .toMap()
}

// FirebaseUser → Domain User
fun FirebaseUser.toUser() = User(
    uid         = uid,
    displayName = displayName ?: "Người dùng",
    email       = email ?: "",
    photoUrl    = photoUrl?.toString()
)

// Domain Message → Room Entity (để lưu offline)
fun Message.toMessageEntity() = MessageEntity(
    id         = id,
    roomId     = roomId,
    senderId   = senderId,
    senderName = senderName,
    content    = content,
    type       = type.name,
    fileUrl    = fileUrl,
    fileName   = fileName,
    localCachePath = localCachePath,
    isRead     = isRead,
    deliveredToCsv = deliveredTo.joinToString(","),
    seenByCsv  = seenBy.joinToString(","),
    reactionsCsv = reactions.toReactionsCsv(),
    createdAt  = createdAt.time          // Date -> Long
)

// Room Entity → Domain Message (để hiển thị)
fun MessageEntity.toMessage() = Message(
    id         = id,
    roomId     = roomId,
    senderId   = senderId,
    senderName = senderName,
    content    = content,
    type       = MessageType.valueOf(type),
    fileUrl    = fileUrl,
    fileName   = fileName,
    localCachePath = localCachePath,
    isRead     = isRead,
    deliveredTo = deliveredToCsv.split(',').filter { it.isNotBlank() },
    seenBy     = seenByCsv.split(',').filter { it.isNotBlank() },
    reactions  = reactionsCsv.toReactionsMap(),
    createdAt  = Date(createdAt)         // Long -> Date
)
