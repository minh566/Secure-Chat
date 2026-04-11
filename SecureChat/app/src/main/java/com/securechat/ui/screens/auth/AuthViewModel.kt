package com.securechat.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securechat.domain.model.AuthResult
import com.securechat.domain.usecase.auth.SignInUseCase
import com.securechat.domain.usecase.auth.SignUpUseCase
import com.securechat.domain.usecase.auth.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UiState — sealed class mô tả trạng thái màn hình
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val email: String = "",
    val password: String = "",
    val displayName: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    // StateFlow: Composable observe để re-compose khi thay đổi
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String)       = _uiState.update { it.copy(email = value) }
    fun onPasswordChange(value: String)    = _uiState.update { it.copy(password = value) }
    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value) }
    fun clearError()                       = _uiState.update { it.copy(errorMessage = null) }

    fun signIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = signInUseCase(_uiState.value.email, _uiState.value.password)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                AuthResult.Loading    -> Unit
            }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signUpUseCase(
                _uiState.value.email,
                _uiState.value.password,
                _uiState.value.displayName
            )
            when (result) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                AuthResult.Loading    -> Unit
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _uiState.update { it.copy(isLoggedIn = false) }
        }
    }
}
