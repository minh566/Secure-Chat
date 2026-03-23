package com.securechat.domain.usecase.chat

import com.securechat.domain.model.*
import com.securechat.domain.repository.ChatRepository
import java.util.*
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        roomId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Resource<Unit> {
        if (content.isBlank()) return Resource.Error("Tin nhắn không được trống")
        val message = Message(
            id        = UUID.randomUUID().toString(),
            roomId    = roomId,
            senderId  = senderId,
            senderName= senderName,
            content   = content.trim(),
            type      = MessageType.TEXT,
            createdAt = Date()
        )
        return chatRepository.sendMessage(message)
    }
}
