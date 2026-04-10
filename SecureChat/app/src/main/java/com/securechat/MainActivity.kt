package com.securechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.repository.CallRepository
import com.securechat.ui.navigation.Screen
import com.securechat.ui.navigation.SecureChatNavGraph
import com.securechat.ui.theme.SecureChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private data class IncomingCallNavigation(
        val sessionId: String,
        val callerName: String,
        val peerId: String
    )

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var callRepository: CallRepository

    private val incomingCallEvents = MutableSharedFlow<IncomingCallNavigation>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecureChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    val handledIncomingSessionId = androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf<String?>(null)
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
                                        Screen.Call.go(it.id, "Cuộc gọi đến", false)
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

                        incomingCallEvents.collectLatest { payload ->
                            val shouldNavigate = handledIncomingSessionId.value != payload.sessionId
                            if (!shouldNavigate) return@collectLatest

                            handledIncomingSessionId.value = payload.sessionId
                            navController.navigate(
                                Screen.Call.go(payload.sessionId, payload.callerName, false)
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }

                    val startDestination = if (authRepository.isLoggedIn())
                        Screen.Home.route
                    else
                        Screen.Login.route

                    SecureChatNavGraph(
                        navController    = navController,
                        startDestination = startDestination,
                        authRepository   = authRepository
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIncomingCallIntent(intent)?.let { incomingCallEvents.tryEmit(it) }
    }

    private fun parseIncomingCallIntent(intent: Intent?): IncomingCallNavigation? {
        if (intent?.getStringExtra("action") != "ACCEPT_CALL") return null
        val sessionId = intent.getStringExtra("sessionId") ?: return null
        val callerName = intent.getStringExtra("callerName") ?: "Cuộc gọi đến"
        val peerId = intent.getStringExtra("peerId") ?: return null
        return IncomingCallNavigation(sessionId = sessionId, callerName = callerName, peerId = peerId)
    }
}
