package com.example.securechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.securechat.presentation.auth.LoginScreen
import com.example.securechat.presentation.home.HomeScreen
import com.example.securechat.presentation.home.GroupPreview
import com.example.securechat.presentation.chat.GroupChatScreen
import com.example.securechat.presentation.video.VideoCallScreen
import com.example.securechat.ui.theme.ConnectNowTheme
import com.example.securechat.viewmodel.ChatViewModel
import com.example.securechat.viewmodel.VideoCallViewModel

// ── Routes ────────────────────────────────────────────────────────────────────
object Routes {
    const val LOGIN   = "login"
    const val HOME    = "home"
    const val CHAT    = "chat"
    const val VIDEO   = "video"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectNowTheme {
                AppNavGraph()
            }
        }
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        // ── Màn hình đăng nhập ──────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // TODO: màn hình đăng ký
                }
            )
        }

        // ── Trang chủ danh sách nhóm ────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onGroupClick = { group ->
                    navController.navigate(Routes.CHAT)
                },
                onProfileClick = {
                    // TODO: màn hình profile
                },
                onCreateGroup = {
                    // TODO: màn hình tạo nhóm
                }
            )
        }

        // ── Màn hình chat nhóm ──────────────────────────────────────────────
        composable(Routes.CHAT) {
            val chatViewModel: ChatViewModel = viewModel()
            GroupChatScreen(
                groupName    = "Nhóm Công Nghệ",
                memberCount  = 8,
                onBack       = { navController.popBackStack() },
                onStartVideoCall = {
                    navController.navigate(Routes.VIDEO)
                },
                onViewMembers = {
                    // TODO: màn hình thành viên
                },
                viewModel = chatViewModel
            )
        }

        // ── Màn hình gọi video ──────────────────────────────────────────────
        composable(Routes.VIDEO) {
            val videoViewModel: VideoCallViewModel = viewModel()
            VideoCallScreen(
                roomId    = "room_demo_001",
                groupName = "Nhóm Công Nghệ",
                onEndCall = { navController.popBackStack() },
                viewModel = videoViewModel
            )
        }
    }
}