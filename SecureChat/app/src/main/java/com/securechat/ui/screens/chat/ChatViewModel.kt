package com.securechat.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.Message
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.usecase.chat.GetMessagesUseCase
import com.securechat.domain.usecase.chat.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val errorMessage: String? = null,
    val isSending: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { loadMessages() }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(roomId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, messages = result.data)
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(inputText = value) }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return
        val user = authRepository.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }
            sendMessageUseCase(
                roomId     = roomId,
                senderId   = user.uid,
                senderName = user.displayName,
                content    = content
            )
            _uiState.update { it.copy(isSending = false) }
        }
    }
}
