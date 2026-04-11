package com.securechat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.*
import androidx.navigation.compose.*
import com.securechat.domain.repository.AuthRepository
import com.securechat.ui.screens.auth.LoginScreen
import com.securechat.ui.screens.auth.RegisterScreen
import com.securechat.ui.screens.call.VideoCallScreen
import com.securechat.ui.screens.chat.ChatScreen
import com.securechat.ui.screens.chat.ImageGalleryScreen
import com.securechat.ui.screens.home.HomeScreen
import com.securechat.ui.screens.profile.EditProfileScreen
import com.securechat.ui.screens.settings.PrivacySettingsScreen
import com.securechat.ui.screens.settings.SecuritySettingsScreen
import com.securechat.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Login    : Screen("login")
    data object Register : Screen("register")
    data object Home     : Screen("home")
    data object Settings : Screen("settings")
    data object PrivacySettings : Screen("settings/privacy")
    data object SecuritySettings : Screen("settings/security")
    data object EditProfile : Screen("edit_profile")

    data object Chat : Screen("chat/{roomId}/{roomName}") {
        fun go(roomId: String, roomName: String) =
            "chat/$roomId/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    }

    data object Call : Screen("call/{sessionId}/{calleeName}/{isCaller}/{peerId}/{isGroup}") {
        fun go(sessionId: String, calleeName: String, isCaller: Boolean, peerId: String, isGroup: Boolean) =
            "call/$sessionId/${java.net.URLEncoder.encode(calleeName, "UTF-8")}/$isCaller/${java.net.URLEncoder.encode(peerId, "UTF-8")}/$isGroup"
    }

    data object ImageGallery : Screen("image_gallery/{token}") {
        fun go(token: String) = "image_gallery/$token"
    }
}

private data class ImageGalleryPayload(
    val imageSources: List<String>,
    val startIndex: Int
)

private object ImageGalleryStore {
    var payload: ImageGalleryPayload? = null
}

@Composable
fun SecureChatNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    authRepository: AuthRepository
) {
    val scope = rememberCoroutineScope()

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
                { navController.navigate(Screen.PrivacySettings.route) },
                { navController.navigate(Screen.SecuritySettings.route) },
                {
                    scope.launch {
                        authRepository.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = { navController.popBackStack() })
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
                onStartVideoCall = { calleeId, isGroup ->
                    val sessionId = java.util.UUID.randomUUID().toString()
                    navController.navigate(Screen.Call.go(sessionId, roomName, true, calleeId, isGroup))
                },
                onOpenImageViewer = { imageSources, startIndex ->
                    ImageGalleryStore.payload = ImageGalleryPayload(
                        imageSources = imageSources,
                        startIndex = startIndex
                    )
                    navController.navigate(Screen.ImageGallery.go(System.currentTimeMillis().toString()))
                },
                authRepository = authRepository
            )
        }

        composable(
            route = Screen.ImageGallery.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) {
            val payload = ImageGalleryStore.payload
            if (payload == null) {
                navController.popBackStack()
            } else {
                ImageGalleryScreen(
                    imageSources = payload.imageSources,
                    startIndex = payload.startIndex,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("sessionId")  { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("isCaller")   { type = NavType.BoolType },
                navArgument("peerId")     { type = NavType.StringType },
                navArgument("isGroup")    { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val calleeNameRaw = backStackEntry.arguments?.getString("calleeName") ?: ""
            val calleeName = java.net.URLDecoder.decode(calleeNameRaw, "UTF-8")
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false

            VideoCallScreen(
                calleeName  = calleeName,
                isGroupCall = isGroup,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}
