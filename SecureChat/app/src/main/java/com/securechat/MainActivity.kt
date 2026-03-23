package com.securechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.securechat.domain.repository.AuthRepository
import com.securechat.ui.navigation.Screen
import com.securechat.ui.navigation.SecureChatNavGraph
import com.securechat.ui.theme.SecureChatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecureChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Nếu đã đăng nhập → vào Home, chưa → vào Login
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
}
