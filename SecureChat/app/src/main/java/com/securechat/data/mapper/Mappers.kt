package com.securechat.data.mapper

import com.google.firebase.auth.FirebaseUser
import com.securechat.data.local.entity.MessageEntity
import com.securechat.domain.model.Message
import com.securechat.domain.model.MessageType
import com.securechat.domain.model.User
import java.util.Date

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
    isRead     = isRead,
    createdAt  = createdAt.time          // Date → Long
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
    isRead     = isRead,
    createdAt  = Date(createdAt)         // Long → Date
)
