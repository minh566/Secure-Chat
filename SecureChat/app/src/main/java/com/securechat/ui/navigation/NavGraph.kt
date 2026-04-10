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
import com.securechat.ui.screens.profile.EditProfileScreen
<<<<<<< Updated upstream
import com.securechat.ui.screens.settings.SettingsScreen
=======
>>>>>>> Stashed changes

sealed class Screen(val route: String) {
    data object Login    : Screen("login")
    data object Register : Screen("register")
    data object Home     : Screen("home")
<<<<<<< Updated upstream
    data object Settings : Screen("settings")
    data object EditProfile : Screen("edit_profile")
=======
    data object Profile  : Screen("profile")
>>>>>>> Stashed changes

    data object Chat : Screen("chat/{roomId}/{roomName}") {
        fun go(roomId: String, roomName: String) =
            "chat/$roomId/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    }

<<<<<<< Updated upstream
    data object Call : Screen("call/{sessionId}/{calleeName}/{isCaller}/{peerId}") {
        fun go(sessionId: String, calleeName: String, isCaller: Boolean, peerId: String) =
            "call/$sessionId/${java.net.URLEncoder.encode(calleeName, "UTF-8")}/$isCaller/${java.net.URLEncoder.encode(peerId, "UTF-8")}"
=======
    data object Call : Screen("call/{sessionId}/{calleeName}/{isCaller}/{callerId}") {
        fun go(sessionId: String, calleeName: String, isCaller: Boolean, callerId: String) =
            "call/$sessionId/${java.net.URLEncoder.encode(calleeName, "UTF-8")}/$isCaller/$callerId"
>>>>>>> Stashed changes
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
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                { navController.popBackStack() },
                { navController.navigate(Screen.EditProfile.route) },
                {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

<<<<<<< Updated upstream
        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = { navController.popBackStack() })
=======
        composable(Screen.Profile.route) {
            EditProfileScreen(
                onBack = { navController.popBackStack() }
            )
>>>>>>> Stashed changes
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("roomId")   { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomNameRaw = backStackEntry.arguments?.getString("roomName") ?: ""
            val roomName = java.net.URLDecoder.decode(roomNameRaw, "UTF-8")

            ChatScreen(
                roomName = roomName,
                onBack   = { navController.popBackStack() },
                onStartVideoCall = { calleeId ->
                    val sessionId = java.util.UUID.randomUUID().toString()
<<<<<<< Updated upstream
                    navController.navigate(Screen.Call.go(sessionId, roomName, true, calleeId))
=======
                    val callerId = authRepository.currentUser?.uid ?: ""
                    navController.navigate(Screen.Call.go(sessionId, roomName, true, callerId))
>>>>>>> Stashed changes
                },
                authRepository = authRepository
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("sessionId")  { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("isCaller")   { type = NavType.BoolType },
<<<<<<< Updated upstream
                navArgument("peerId")     { type = NavType.StringType }
=======
                navArgument("callerId")   { type = NavType.StringType }
>>>>>>> Stashed changes
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val calleeNameRaw = backStackEntry.arguments?.getString("calleeName") ?: ""
            val calleeName = java.net.URLDecoder.decode(calleeNameRaw, "UTF-8")
            val isCaller = backStackEntry.arguments?.getBoolean("isCaller") ?: false
            val callerId = backStackEntry.arguments?.getString("callerId") ?: ""

            VideoCallScreen(
                sessionId = sessionId,
                calleeName = calleeName,
                isCaller = isCaller,
                callerId = callerId,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}
