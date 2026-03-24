package com.securechat.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.ChatRoom
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.usecase.auth.SignOutUseCase
import com.securechat.domain.usecase.chat.CreateRoomUseCase
import com.securechat.domain.usecase.chat.GetChatRoomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val rooms: List<ChatRoom> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newRoomName: String = "",
    val isCreating: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentUser get() = authRepository.currentUser

    init {
        loadRooms()
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

    fun onNewRoomNameChange(name: String) = _uiState.update { it.copy(newRoomName = name) }
    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false, newRoomName = "") }

    fun createRoom() {
        val name = _uiState.value.newRoomName.trim()
        val user = authRepository.currentUser ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            val result = createRoomUseCase(name, listOf(user.uid), false)
            _uiState.update { it.copy(isCreating = false) }
            
            if (result is Resource.Success) {
                dismissCreateDialog()
                // Danh sách sẽ tự cập nhật nhờ collect GetChatRoomsUseCase
            } else if (result is Resource.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                signOutUseCase()
                onDone()
            } catch (e: Exception) {
                // Nếu lỗi Firestore offline vẫn ép đăng xuất ở local
                onDone()
            }
        }
    }
}
