package com.securechat.domain.usecase.chat

import com.securechat.domain.model.Message
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(roomId: String): Flow<Resource<List<Message>>> =
        chatRepository.getMessages(roomId)
}
