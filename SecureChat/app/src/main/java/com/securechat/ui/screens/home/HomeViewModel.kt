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
    val newRoomName: String = ""
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

    init { loadRooms() }

    private fun loadRooms() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            getChatRoomsUseCase(uid).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, rooms = result.data)
                    }
                    is Resource.Error   -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun onNewRoomNameChange(name: String) = _uiState.update { it.copy(newRoomName = name) }
    fun showCreateDialog()  = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false, newRoomName = "") }

    fun createRoom() {
        val name = _uiState.value.newRoomName.trim()
        val uid  = authRepository.currentUser?.uid ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            dismissCreateDialog()
            createRoomUseCase(name, listOf(uid), false)
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase()
            onDone()
        }
    }
}
