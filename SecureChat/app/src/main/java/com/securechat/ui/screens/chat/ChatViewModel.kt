package com.securechat.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Message
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
import com.securechat.domain.usecase.chat.AddMembersToRoomUseCase
import com.securechat.domain.usecase.chat.GetMessagesUseCase
import com.securechat.domain.usecase.chat.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val memberSearchQuery: String = "",
    val memberSearchResults: List<User> = emptyList(),
    val selectedMemberIds: Set<String> = emptySet(),
    val showAddMembersDialog: Boolean = false,
    val isAddingMembers: Boolean = false,
    val errorMessage: String? = null,
    val isSending: Boolean = false,
    val chatRoom: ChatRoom? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val addMembersToRoomUseCase: AddMembersToRoomUseCase,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val currentUserId: String get() = authRepository.currentUser?.uid.orEmpty()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        loadRoomDetails()
        setupMemberSearch()
    }

    private fun loadRoomDetails() {
        viewModelScope.launch {
            val result = chatRepository.getChatRoom(roomId)
            if (result is Resource.Success) {
                _uiState.update { it.copy(chatRoom = result.data) }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(roomId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, messages = result.data) }
                        if (currentUserId.isNotBlank()) {
                            acknowledgeIncomingMessages(currentUserId)
                        }
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun onInputChange(value: String) = _uiState.update { it.copy(inputText = value) }

    fun showAddMembersDialog() {
        _uiState.update {
            it.copy(
                showAddMembersDialog = true,
                memberSearchQuery = "",
                memberSearchResults = emptyList(),
                selectedMemberIds = emptySet(),
                errorMessage = null
            )
        }
    }

    fun dismissAddMembersDialog() {
        _uiState.update {
            it.copy(
                showAddMembersDialog = false,
                memberSearchQuery = "",
                memberSearchResults = emptyList(),
                selectedMemberIds = emptySet()
            )
        }
    }

    fun onMemberSearchQueryChange(query: String) {
        _uiState.update { it.copy(memberSearchQuery = query) }
    }

    fun toggleMemberSelection(userId: String) {
        _uiState.update { state ->
            val selected = state.selectedMemberIds.toMutableSet()
            if (!selected.add(userId)) selected.remove(userId)
            state.copy(selectedMemberIds = selected)
        }
    }

    fun addSelectedMembers() {
        val selected = _uiState.value.selectedMemberIds.toList()
        if (selected.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn thành viên") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingMembers = true, errorMessage = null) }
            when (val result = addMembersToRoomUseCase(roomId, selected)) {
                is Resource.Success -> {
                    loadRoomDetails()
                    dismissAddMembersDialog()
                    _uiState.update { it.copy(isAddingMembers = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isAddingMembers = false, errorMessage = result.message) }
                }
                Resource.Loading -> Unit
            }
        }
    }

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
    
    fun getCalleeId(): String? {
        val currentUserId = authRepository.currentUser?.uid ?: return null
        return uiState.value.chatRoom?.members?.find { it != currentUserId }
    }

    private fun acknowledgeIncomingMessages(userId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesDelivered(roomId, userId)
            chatRepository.markMessagesSeen(roomId, userId)
            chatRepository.markAsRead(roomId, userId)
        }
    }

    private fun setupMemberSearch() {
        _uiState
            .map { it.memberSearchQuery.trim() }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    _uiState.update { it.copy(memberSearchResults = emptyList()) }
                    return@onEach
                }

                userRepository.searchUsers(query).collectLatest { result ->
                    if (result is Resource.Success) {
                        val roomMembers = _uiState.value.chatRoom?.members?.toSet().orEmpty()
                        val filtered = result.data.filter { it.uid !in roomMembers }
                        _uiState.update { it.copy(memberSearchResults = filtered) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
