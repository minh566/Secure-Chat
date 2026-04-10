
package com.securechat.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.ChatRepository
import com.securechat.domain.repository.UserRepository
import com.securechat.domain.usecase.auth.SignOutUseCase
import com.securechat.domain.usecase.chat.CreateRoomUseCase
import com.securechat.domain.usecase.chat.GetChatRoomsUseCase
import com.securechat.ui.common.toSearchResourceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val selectedUser: User? = null,
    val incomingRequests: List<User> = emptyList(),
    val deletingRoomId: String? = null,
    val roomToDeleteId: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val chatRepository: ChatRepository,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val searchResultsResource = _uiState
        .map { it.searchQuery }
        .toSearchResourceState(viewModelScope) { query ->
            userRepository.searchUsers(query)
        }

    val currentUser get() = authRepository.currentUser

    init {
        loadRooms()
        setupSearch()
        observeIncomingFriendRequests()
    }

    private fun loadRooms() {
        val user = authRepository.currentUser ?: return
        viewModelScope.launch {
            getChatRoomsUseCase(user.uid).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, rooms = result.data, errorMessage = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun observeIncomingFriendRequests() {
        viewModelScope.launch {
            userRepository.observeIncomingFriendRequests().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(incomingRequests = result.data) }
                    is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                    else -> Unit
                }
            }
        }
    }

    private fun setupSearch() {
        viewModelScope.launch {
            searchResultsResource.collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.update {
                        it.copy(searchResults = result.data)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(searchResults = emptyList(), errorMessage = result.message)
                    }
                    Resource.Loading -> Unit
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) = _uiState.update {
        it.copy(searchQuery = query, selectedUser = null, infoMessage = null)
    }

    fun onUserSelected(user: User) {
        _uiState.update {
            it.copy(
                selectedUser = user,
                searchQuery = user.displayName,
                searchResults = emptyList(),
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }

    fun dismissCreateDialog() = _uiState.update {
        it.copy(
            showCreateDialog = false,
            searchQuery = "",
            searchResults = emptyList(),
            selectedUser = null,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun sendFriendRequest() {
        val selectedUser = _uiState.value.selectedUser
        if (selectedUser == null) {
            _uiState.update { it.copy(errorMessage = "Vui long chon nguoi ban muon ket ban") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null, infoMessage = null) }
            val result = userRepository.sendFriendRequest(selectedUser.uid)
            _uiState.update { it.copy(isCreating = false) }

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            infoMessage = "Da gui loi moi ket ban, cho doi phuong xac nhan",
                            searchQuery = "",
                            searchResults = emptyList(),
                            selectedUser = null,
                            showCreateDialog = false
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun acceptFriendRequest(user: User) {
        val current = authRepository.currentUser ?: return
        viewModelScope.launch {
            when (val result = userRepository.acceptFriendRequest(user.uid)) {
                is Resource.Success -> {
                    createRoomUseCase(user.displayName, listOf(current.uid, user.uid), isGroup = false)
                    _uiState.update { it.copy(infoMessage = "Da chap nhan loi moi ket ban") }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun rejectFriendRequest(user: User) {
        viewModelScope.launch {
            when (val result = userRepository.rejectFriendRequest(user.uid)) {
                is Resource.Success -> _uiState.update { it.copy(infoMessage = "Da tu choi loi moi ket ban") }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun clearInfoMessage() = _uiState.update { it.copy(infoMessage = null) }

    fun showDeleteRoomDialog(roomId: String) {
        _uiState.update { it.copy(roomToDeleteId = roomId) }
    }

    fun dismissDeleteRoomDialog() {
        _uiState.update { it.copy(roomToDeleteId = null) }
    }

    fun deleteRoom(roomId: String) {
        if (_uiState.value.deletingRoomId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(deletingRoomId = roomId, errorMessage = null, roomToDeleteId = null) }
            when (val result = chatRepository.deleteChatRoom(roomId)) {
                is Resource.Error -> _uiState.update {
                    it.copy(deletingRoomId = null, errorMessage = result.message)
                }
                else -> _uiState.update { it.copy(deletingRoomId = null) }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
            onDone()
        }
    }
}

