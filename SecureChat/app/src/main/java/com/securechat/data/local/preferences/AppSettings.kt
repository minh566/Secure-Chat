package com.securechat.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorageValue(prefs.getString(KEY_THEME_MODE, null))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hideOnlineStatus = MutableStateFlow(
        prefs.getBoolean(KEY_HIDE_ONLINE_STATUS, false)
    )
    val hideOnlineStatus: StateFlow<Boolean> = _hideOnlineStatus.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    )
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        when (key) {
            KEY_NOTIFICATIONS_ENABLED -> {
                _notificationsEnabled.value = sharedPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            }

            KEY_THEME_MODE -> {
                _themeMode.value = ThemeMode.fromStorageValue(
                    sharedPrefs.getString(KEY_THEME_MODE, null)
                )
            }

            KEY_HIDE_ONLINE_STATUS -> {
                _hideOnlineStatus.value = sharedPrefs.getBoolean(KEY_HIDE_ONLINE_STATUS, false)
            }

            KEY_BIOMETRIC_LOCK -> {
                _biometricLockEnabled.value = sharedPrefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
    }

    fun setHideOnlineStatus(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_ONLINE_STATUS, hidden).apply()
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    companion object {
        private const val PREF_NAME = "securechat_settings"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HIDE_ONLINE_STATUS = "hide_online_status"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
    }
}

enum class ThemeMode(val storageValue: String, val label: String) {
    SYSTEM("system", "Theo hệ thống"),
    LIGHT("light", "Premium Light"),
    DARK("dark", "Premium Dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

