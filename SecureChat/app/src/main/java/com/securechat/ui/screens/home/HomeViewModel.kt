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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newRoomName: String = "",
    val isCreating: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val selectedUser: User? = null,
    val deletingRoomId: String? = null
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

    val currentUser get() = authRepository.currentUser

    init {
        loadRooms()
        setupSearch()
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

    private fun setupSearch() {
        _uiState.map { it.searchQuery }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { query ->
                if (query.isBlank()) {
                    _uiState.update { it.copy(searchResults = emptyList()) }
                    return@onEach
                }
                userRepository.searchUsers(query).collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(searchResults = result.data) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onNewRoomNameChange(name: String) = _uiState.update { it.copy(newRoomName = name) }
    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query) }
    
    fun onUserSelected(user: User) {
        _uiState.update { it.copy(selectedUser = user, newRoomName = user.displayName) }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    
    fun dismissCreateDialog() = _uiState.update { 
        it.copy(
            showCreateDialog = false, 
            newRoomName = "", 
            searchQuery = "", 
            searchResults = emptyList(), 
            selectedUser = null,
            errorMessage = null
        ) 
    }

    fun createRoom() {
        val currentUser = authRepository.currentUser ?: return
        val selectedUser = _uiState.value.selectedUser
        val name = _uiState.value.newRoomName.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập tên phòng") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            
            // QUAN TRỌNG: Thêm cả 2 người vào danh sách members
            val memberIds = if (selectedUser != null) {
                listOf(currentUser.uid, selectedUser.uid)
            } else {
                listOf(currentUser.uid)
            }
            
            val result = createRoomUseCase(name, memberIds, selectedUser == null)
            _uiState.update { it.copy(isCreating = false) }
            
            if (result is Resource.Success) {
                dismissCreateDialog()
            } else if (result is Resource.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun deleteRoom(roomId: String) {
        if (_uiState.value.deletingRoomId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(deletingRoomId = roomId, errorMessage = null) }
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
            try {
                signOutUseCase()
                onDone()
            } catch (e: Exception) {
                onDone()
            }
        }
    }
}
