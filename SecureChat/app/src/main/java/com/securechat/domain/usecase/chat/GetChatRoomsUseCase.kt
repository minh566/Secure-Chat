package com.securechat.domain.usecase.chat

import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatRoomsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<ChatRoom>>> =
        chatRepository.getChatRooms(userId)
}
