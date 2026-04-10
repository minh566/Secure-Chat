package com.securechat.domain.usecase.chat

import com.securechat.domain.model.Resource
import com.securechat.domain.repository.ChatRepository
import javax.inject.Inject

class AddMembersToRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(roomId: String, memberIds: List<String>): Resource<Unit> {
        val cleanedIds = memberIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanedIds.isEmpty()) return Resource.Error("Chưa chọn thành viên")
        return chatRepository.addMembersToRoom(roomId, cleanedIds)
    }
}

