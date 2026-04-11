package com.securechat.domain.usecase.chat

import com.securechat.domain.model.Resource
import com.securechat.domain.repository.ChatRepository
import javax.inject.Inject

class DeleteChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(roomId: String): Resource<Unit> {
        if (roomId.isBlank()) return Resource.Error("Thiếu roomId")
        return chatRepository.deleteChatRoom(roomId)
    }
}

