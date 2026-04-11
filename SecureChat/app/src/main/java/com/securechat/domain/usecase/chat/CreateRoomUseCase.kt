package com.securechat.domain.usecase.chat

import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.ChatRepository
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        name: String,
        memberIds: List<String>,
        isGroup: Boolean,
        roomImageUri: String? = null
    ): Resource<ChatRoom> {
        if (memberIds.isEmpty()) return Resource.Error("Cần ít nhất 1 thành viên")
        return chatRepository.createRoom(name, memberIds, isGroup, roomImageUri)
    }
}
