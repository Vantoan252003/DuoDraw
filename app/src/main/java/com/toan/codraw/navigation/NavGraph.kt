package com.toan.codraw.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.presentation.ui.DrawingScreen
import com.toan.codraw.presentation.ui.HomeScreen
import com.toan.codraw.presentation.ui.LoginScreen
import com.toan.codraw.presentation.ui.RegisterScreen
import com.toan.codraw.presentation.ui.RoomScreen
import com.toan.codraw.presentation.ui.SavedDrawingScreen
import com.toan.codraw.presentation.ui.SettingsScreen
import com.toan.codraw.presentation.viewmodel.AuthViewModel

@Composable
fun NavGraph(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val startDestination = if (authViewModel.isAlreadyLoggedIn()) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                viewModel = authViewModel
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToRoom = { code ->
                    val route = if (code.isNullOrBlank()) "room" else "room?code=$code"
                    navController.navigate(route)
                },
                onEnterDrawingRoom = { roomCode, playerId, playerCount ->
                    // Save active room before navigating
                    sessionManager.saveActiveRoom(roomCode, playerId, playerCount)
                    navController.navigate("drawing/$roomCode/$playerId/$playerCount") {
                        launchSingleTop = true
                    }
                },
                onOpenSavedDrawing = { roomCode ->
                    navController.navigate("savedDrawing/$roomCode")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "room?code={code}",
            arguments = listOf(
                navArgument("code") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val initialCode = backStackEntry.arguments?.getString("code").orEmpty()
            RoomScreen(
                onRoomReady = { roomCode, playerId, playerCount ->
                    // Save active room before navigating
                    sessionManager.saveActiveRoom(roomCode, playerId, playerCount)
                    navController.navigate("drawing/$roomCode/$playerId/$playerCount") {
                        launchSingleTop = true
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                initialJoinCode = initialCode
            )
        }

        composable(
            route = "drawing/{roomCode}/{playerId}/{playerCount}",
            arguments = listOf(
                navArgument("roomCode") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.IntType },
                navArgument("playerCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            val playerId = backStackEntry.arguments?.getInt("playerId") ?: 1
            val playerCount = backStackEntry.arguments?.getInt("playerCount") ?: 1
            DrawingScreen(
                roomCode = roomCode,
                localPlayerId = playerId,
                playerCount = playerCount,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "savedDrawing/{roomCode}",
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) {
            SavedDrawingScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("friends") {
            com.toan.codraw.presentation.ui.FriendsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { friendUsername ->
                    navController.navigate("chat/$friendUsername")
                }
            )
        }

        composable(
            route = "chat/{friendUsername}",
            arguments = listOf(navArgument("friendUsername") { type = NavType.StringType })
        ) { backStackEntry ->
            val friendUsername = backStackEntry.arguments?.getString("friendUsername") ?: ""
            com.toan.codraw.presentation.ui.PrivateChatScreen(
                friendUsername = friendUsername,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
