package com.securechat

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.securechat.data.local.preferences.AppSettings
import com.securechat.data.local.preferences.ThemeMode
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import com.securechat.ui.navigation.Screen
import com.securechat.ui.navigation.SecureChatNavGraph
import com.securechat.ui.theme.SecureChatTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private data class IncomingCallNavigation(
        val sessionId: String,
        val callerName: String,
        val peerId: String
    )

    private data class OpenChatNavigation(
        val roomId: String,
        val roomName: String
    )

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var callRepository: CallRepository
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mainExecutor: Executor
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val authStateListener = FirebaseAuth.AuthStateListener {
        syncFcmTokenIfLoggedIn()
    }

    private val incomingCallEvents = MutableSharedFlow<IncomingCallNavigation>(extraBufferCapacity = 1)
    private val openChatEvents = MutableSharedFlow<OpenChatNavigation>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(this)
        requestNotificationPermissionIfNeeded()
        syncFcmTokenIfLoggedIn()

        setContentView(ComposeView(this).apply {
            setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SecureChatTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    val handledIncomingSessionId = remember {
                        mutableStateOf<String?>(null)
                    }

                    // Lắng nghe cuộc gọi đến Realtime
                    LaunchedEffect(authRepository.currentUser) {
                        authRepository.currentUser?.let { user ->
                            callRepository.observeIncomingCall(user.uid).collectLatest { session ->
                                session?.let {
                                    val shouldNavigate = handledIncomingSessionId.value != it.id
                                    if (!shouldNavigate) return@collectLatest

                                    handledIncomingSessionId.value = it.id
                                    navController.navigate(
                                        Screen.Call.go(it.id, "Cuộc gọi đến", false, it.callerId, false)
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }

                    // Điều hướng khi người dùng bấm notification ACCEPT_CALL.
                    LaunchedEffect(Unit) {
                        parseIncomingCallIntent(intent)?.let { incomingCallEvents.tryEmit(it) }
                        parseOpenChatIntent(intent)?.let { openChatEvents.tryEmit(it) }

                        incomingCallEvents.collectLatest { payload ->
                            val shouldNavigate = handledIncomingSessionId.value != payload.sessionId
                            if (!shouldNavigate) return@collectLatest

                            handledIncomingSessionId.value = payload.sessionId
                            navController.navigate(
                                Screen.Call.go(payload.sessionId, payload.callerName, false, payload.peerId, false)
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        openChatEvents.collectLatest { payload ->
                            navController.navigate(Screen.Chat.go(payload.roomId, payload.roomName)) {
                                launchSingleTop = true
                            }
                        }
                    }

                    val startDestination = if (authRepository.isLoggedIn())
                        Screen.Home.route
                    else
                        Screen.Login.route

                    val biometricLockEnabled by appSettings.biometricLockEnabled.collectAsStateWithLifecycle()
                    var appUnlocked by remember { mutableStateOf(!biometricLockEnabled) }
                    var authInProgress by remember { mutableStateOf(false) }
                    var authErrorMessage by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(biometricLockEnabled) {
                        appUnlocked = !biometricLockEnabled
                        authErrorMessage = null
                    }

                    fun requestAppUnlock() {
                        if (authInProgress) return
                        authInProgress = true
                        authenticateWithFaceOrBiometric(
                            onSuccess = {
                                authInProgress = false
                                authErrorMessage = null
                                appUnlocked = true
                            },
                            onFailure = { message, disableLock ->
                                authInProgress = false
                                authErrorMessage = message
                                if (disableLock) {
                                    appSettings.setBiometricLockEnabled(false)
                                    appUnlocked = true
                                }
                            }
                        )
                    }

                    LaunchedEffect(biometricLockEnabled, appUnlocked) {
                        if (biometricLockEnabled && !appUnlocked) {
                            requestAppUnlock()
                        }
                    }

                    if (biometricLockEnabled && !appUnlocked) {
                        BackHandler(enabled = true) { moveTaskToBack(true) }
                        LockScreen(
                            errorMessage = authErrorMessage,
                            onRetry = ::requestAppUnlock
                        )
                    } else {
                        SecureChatNavGraph(
                            navController    = navController,
                            startDestination = startDestination,
                            authRepository   = authRepository
                        )
                    }
                }
            }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIncomingCallIntent(intent)?.let { incomingCallEvents.tryEmit(it) }
        parseOpenChatIntent(intent)?.let { openChatEvents.tryEmit(it) }
    }

    private fun parseIncomingCallIntent(intent: Intent?): IncomingCallNavigation? {
        if (intent?.getStringExtra("action") != "ACCEPT_CALL") return null
        val sessionId = intent.getStringExtra("sessionId") ?: return null
        val callerName = intent.getStringExtra("callerName") ?: "Cuộc gọi đến"
        val peerId = intent.getStringExtra("peerId") ?: return null
        return IncomingCallNavigation(sessionId = sessionId, callerName = callerName, peerId = peerId)
    }

    private fun parseOpenChatIntent(intent: Intent?): OpenChatNavigation? {
        if (intent?.getStringExtra("action") != "OPEN_CHAT") return null
        val roomId = intent.getStringExtra("roomId") ?: return null
        val roomName = intent.getStringExtra("roomName") ?: "Chat"
        return OpenChatNavigation(roomId = roomId, roomName = roomName)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
    }

    private fun syncFcmTokenIfLoggedIn() {
        val currentUser = authRepository.currentUser ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) return@addOnSuccessListener
                ioScope.launch {
                    authRepository.updateFcmToken(token)
                }
            }
    }

    private fun authenticateWithFaceOrBiometric(
        onSuccess: () -> Unit,
        onFailure: (message: String, disableLock: Boolean) -> Unit
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailure("Thiết bị chưa hỗ trợ hoặc chưa cài khuôn mặt/vân tay", true)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mở khóa SecureChat")
            .setSubtitle("Xác thực khuôn mặt hoặc sinh trắc học để tiếp tục")
            .setAllowedAuthenticators(authenticators)
            .build()

        val prompt = BiometricPrompt(
            this,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(errString.toString(), false)
                }

                override fun onAuthenticationFailed() {
                    onFailure("Xác thực chưa đúng, vui lòng thử lại", false)
                }
            }
        )

        prompt.authenticate(promptInfo)
    }
}

@Composable
private fun LockScreen(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SecureChat đã khóa")
            Spacer(Modifier.height(8.dp))
            Text("Dùng khuôn mặt hoặc sinh trắc học để mở ứng dụng")
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage)
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry) {
                Text("Mở khóa")
            }
        }
    }
}
