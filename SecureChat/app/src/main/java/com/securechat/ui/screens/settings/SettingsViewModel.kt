package com.securechat.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.securechat.data.local.preferences.AppSettings
import com.securechat.data.local.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isFetchingFcmToken: Boolean = false,
    val fcmToken: String? = null,
    val fcmDebugError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings
) : ViewModel() {

    private val _fcmDebugState = MutableStateFlow(SettingsUiState())

    val uiState = combine(
        appSettings.notificationsEnabled,
        appSettings.themeMode,
        _fcmDebugState
    ) { notificationsEnabled, themeMode, debugState ->
        SettingsUiState(
            notificationsEnabled = notificationsEnabled,
            themeMode = themeMode,
            isFetchingFcmToken = debugState.isFetchingFcmToken,
            fcmToken = debugState.fcmToken,
            fcmDebugError = debugState.fcmDebugError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setNotificationsEnabled(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appSettings.setThemeMode(mode)
        }
    }

    fun loadFcmTokenForDebug() {
        viewModelScope.launch {
            _fcmDebugState.update {
                it.copy(
                    isFetchingFcmToken = true,
                    fcmDebugError = null
                )
            }

            val result = runCatching {
                FirebaseMessaging.getInstance().token.await()
            }

            _fcmDebugState.update {
                if (result.isSuccess) {
                    it.copy(
                        isFetchingFcmToken = false,
                        fcmToken = result.getOrNull().orEmpty().ifBlank { "<empty>" },
                        fcmDebugError = null
                    )
                } else {
                    it.copy(
                        isFetchingFcmToken = false,
                        fcmDebugError = result.exceptionOrNull()?.localizedMessage ?: "Khong lay duoc token",
                        fcmToken = null
                    )
                }
            }
        }
    }

    fun clearFcmDebugDialog() {
        _fcmDebugState.update { it.copy(fcmToken = null, fcmDebugError = null) }
    }
}

