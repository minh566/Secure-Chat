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
import com.securechat.ui.screens.contact.ContactListScreen
import com.securechat.ui.screens.settings.SettingsScreen
import com.securechat.ui.screens.splash.SplashScreen

sealed class Screen(val route: String) {
    data object Splash   : Screen("splash")
    data object Login    : Screen("login")
    data object Register : Screen("register")
    data object Home     : Screen("home")
    data object Contacts : Screen("contacts")
    data object Settings : Screen("settings")
    
    data object Chat : Screen("chat/{roomId}/{roomName}") {
        fun go(roomId: String, roomName: String) =
            "chat/$roomId/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    }

    data object Call : Screen("call/{calleeName}") {
        fun go(calleeName: String) = "call/${java.net.URLEncoder.encode(calleeName, "UTF-8")}"
    }
}

@Composable
fun SecureChatNavGraph(
    navController: NavHostController,
    startDestination: String,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onAnimationFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChat = { roomId, roomName ->
                    navController.navigate(Screen.Chat.go(roomId, roomName))
                },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }

        composable(Screen.Contacts.route) {
            ContactListScreen(
                onBack = { navController.popBackStack() },
                onStartChat = { roomId, roomName ->
                    navController.navigate(Screen.Chat.go(roomId, roomName)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("roomName") ?: "Chat", "UTF-8"
            )
            ChatScreen(
                roomName = roomName,
                onBack = { navController.popBackStack() },
                onStartVideoCall = { navController.navigate(Screen.Call.go(roomName)) },
                authRepository = authRepository
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(navArgument("calleeName") { type = NavType.StringType })
        ) { backStackEntry ->
            val calleeName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("calleeName") ?: "User", "UTF-8"
            )
            VideoCallScreen(
                calleeName = calleeName,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}
