package com.securechat.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Message
import com.securechat.domain.model.MessageType
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
import com.securechat.domain.usecase.chat.AddMembersToRoomUseCase
import com.securechat.domain.usecase.chat.GetMessagesUseCase
import com.securechat.domain.usecase.chat.SendMessageUseCase
import com.securechat.ui.common.toSearchResourceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    val isUploadingAttachment: Boolean = false,
    val uploadProgressPercent: Int = 0,
    val pendingOpenAttachment: Message? = null,
    val brokenAttachmentMessageIds: Set<String> = emptySet(),
    val resendTargetType: MessageType? = null,
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

    companion object {
        val SUPPORTED_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
    }

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val currentUserId: String get() = authRepository.currentUser?.uid.orEmpty()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val memberSearchResource = _uiState
        .map { it.memberSearchQuery }
        .toSearchResourceState(viewModelScope) { query ->
            userRepository.searchUsers(query)
        }

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
            _uiState.update { it.copy(isSending = true, inputText = "", errorMessage = null) }
            val result = sendMessageUseCase(
                roomId     = roomId,
                senderId   = user.uid,
                senderName = user.displayName,
                content    = content
            )
            _uiState.update {
                when (result) {
                    is Resource.Error -> it.copy(isSending = false, inputText = content, errorMessage = result.message)
                    else -> it.copy(isSending = false)
                }
            }
        }
    }

    fun sendLike() {
        if (_uiState.value.isSending || _uiState.value.isUploadingAttachment) return
        val user = authRepository.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            val result = sendMessageUseCase(
                roomId = roomId,
                senderId = user.uid,
                senderName = user.displayName,
                content = "👍"
            )
            _uiState.update {
                if (result is Resource.Error) it.copy(isSending = false, errorMessage = result.message)
                else it.copy(isSending = false)
            }
        }
    }

    fun sendAttachment(fileUri: Uri, type: MessageType) {
        if (_uiState.value.isSending || _uiState.value.isUploadingAttachment) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingAttachment = true,
                    uploadProgressPercent = 0,
                    errorMessage = null
                )
            }
            val result = chatRepository.sendFile(
                roomId = roomId,
                fileUri = fileUri.toString(),
                type = type,
                onProgress = { percent ->
                    _uiState.update { current ->
                        current.copy(uploadProgressPercent = percent.coerceIn(0, 100))
                    }
                }
            )
            _uiState.update {
                if (result is Resource.Error) {
                    it.copy(
                        isUploadingAttachment = false,
                        uploadProgressPercent = 0,
                        resendTargetType = null,
                        errorMessage = result.message
                    )
                } else {
                    it.copy(
                        isUploadingAttachment = false,
                        uploadProgressPercent = 0,
                        resendTargetType = null
                    )
                }
            }
        }
    }

    fun openAttachment(message: Message) {
        if (message.type != MessageType.IMAGE && message.type != MessageType.FILE) return

        viewModelScope.launch {
            if (message.id in _uiState.value.brokenAttachmentMessageIds) {
                if (message.senderId == currentUserId) {
                    requestResendAttachment(message.type)
                }
                return@launch
            }

            val cachedPath = message.localCachePath
            if (!cachedPath.isNullOrBlank() && File(cachedPath).exists()) {
                _uiState.update { it.copy(pendingOpenAttachment = message) }
                return@launch
            }

            when (val result = chatRepository.cacheAttachment(roomId, message)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            pendingOpenAttachment = result.data,
                            brokenAttachmentMessageIds = it.brokenAttachmentMessageIds - message.id
                        )
                    }
                }

                is Resource.Error -> {
                    val isMissingObject = isMissingObjectError(result.message)
                    _uiState.update {
                        it.copy(
                            errorMessage = if (isMissingObject) {
                                if (message.senderId == currentUserId) {
                                    "Tep da mat tren server. Vui long gui lai tep"
                                } else {
                                    "Tep nay da bi xoa tren server"
                                }
                            } else {
                                result.message
                            },
                            brokenAttachmentMessageIds = if (isMissingObject) {
                                it.brokenAttachmentMessageIds + message.id
                            } else {
                                it.brokenAttachmentMessageIds
                            }
                        )
                    }

                    if (isMissingObject && message.senderId == currentUserId) {
                        requestResendAttachment(message.type)
                    }
                }

                Resource.Loading -> Unit
            }
        }
    }

    private fun isMissingObjectError(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("khong ton tai") ||
            normalized.contains("object") ||
            normalized.contains("not exist")
    }

    fun consumePendingOpenAttachment() {
        _uiState.update { it.copy(pendingOpenAttachment = null) }
    }

    fun requestResendAttachment(type: MessageType) {
        _uiState.update { it.copy(resendTargetType = type) }
    }

    fun consumeResendRequest() {
        _uiState.update { it.copy(resendTargetType = null) }
    }

    fun toggleReaction(message: Message, emoji: String) {
        val user = authRepository.currentUser ?: return
        if (emoji !in SUPPORTED_REACTIONS) return

        viewModelScope.launch {
            val current = message.reactions[user.uid]
            val result = if (current == emoji) {
                chatRepository.removeMessageReaction(roomId, message.id, user.uid)
            } else {
                chatRepository.setMessageReaction(roomId, message.id, user.uid, emoji)
            }

            if (result is Resource.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
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
        viewModelScope.launch {
            memberSearchResource.collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        val roomMembers = _uiState.value.chatRoom?.members?.toSet().orEmpty()
                        val filtered = result.data.filter { it.uid !in roomMembers }
                        _uiState.update { it.copy(memberSearchResults = filtered) }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(memberSearchResults = emptyList(), errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            }
        }
    }
}
