package com.securechat.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.Resource
import com.securechat.domain.repository.AuthRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUser get() = authRepository.currentUser

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.updateProfile(displayName, currentUser?.photoUrl)
            _uiState.update {
                when (result) {
                    is Resource.Success -> it.copy(isLoading = false, isSuccess = true)
                    is Resource.Error -> it.copy(isLoading = false, error = result.message)
                    else -> it
                }
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = currentUser?.uid ?: return@launch
                val ref = storage.reference.child("avatars/$userId.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                
                val result = authRepository.updateProfile(currentUser?.displayName ?: "", downloadUrl)
                _uiState.update {
                    when (result) {
                        is Resource.Success -> it.copy(isLoading = false, isSuccess = true)
                        is Resource.Error -> it.copy(isLoading = false, error = result.message)
                        else -> it
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
