package com.securechat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.securechat.domain.repository.AuthRepository
import com.securechat.ui.screens.auth.LoginScreen
import com.securechat.ui.screens.auth.RegisterScreen
import com.securechat.ui.screens.call.VideoCallScreen
import com.securechat.ui.screens.chat.ChatScreen
import com.securechat.ui.screens.home.HomeScreen

sealed class Screen(val route: String) {
    data object Login    : Screen("login")
    data object Register : Screen("register")
    data object Home     : Screen("home")

    data object Chat : Screen("chat/{roomId}/{roomName}") {
        fun go(roomId: String, roomName: String) =
            "chat/$roomId/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    }

    data object Call : Screen("call/{sessionId}/{calleeName}/{isCaller}") {
        fun go(sessionId: String, calleeName: String, isCaller: Boolean) =
            "call/$sessionId/${java.net.URLEncoder.encode(calleeName, "UTF-8")}/$isCaller"
    }
}

@Composable
fun SecureChatNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    authRepository: AuthRepository
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChat = { roomId, roomName ->
                    navController.navigate(Screen.Chat.go(roomId, roomName))
                },
                onSignedOut = {
                    // SỬA ĐỔI: Xóa toàn bộ stack để không quay lại được Home sau logout
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("roomId")   { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId   = backStackEntry.arguments?.getString("roomId") ?: ""
            val roomNameRaw = backStackEntry.arguments?.getString("roomName") ?: ""
            val roomName = java.net.URLDecoder.decode(roomNameRaw, "UTF-8")

            ChatScreen(
                roomName = roomName,
                onBack   = { navController.popBackStack() },
                onStartVideoCall = {
                    val sessionId = java.util.UUID.randomUUID().toString()
                    navController.navigate(Screen.Call.go(sessionId, roomName, true))
                },
                authRepository = authRepository
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("sessionId")  { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("isCaller")   { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val calleeNameRaw = backStackEntry.arguments?.getString("calleeName") ?: ""
            val calleeName = java.net.URLDecoder.decode(calleeNameRaw, "UTF-8")

            VideoCallScreen(
                calleeName  = calleeName,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}
