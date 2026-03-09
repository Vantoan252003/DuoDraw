package com.toan.codraw.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toan.codraw.presentation.ui.DrawingScreen
import com.toan.codraw.presentation.ui.HomeScreen
import com.toan.codraw.presentation.ui.LoginScreen
import com.toan.codraw.presentation.ui.RegisterScreen
import com.toan.codraw.presentation.ui.RoomScreen
import com.toan.codraw.presentation.ui.SavedDrawingScreen
import com.toan.codraw.presentation.viewmodel.AuthViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Kiểm tra đã đăng nhập chưa để chọn màn hình đầu
    val startDestination = if (authViewModel.isAlreadyLoggedIn()) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login ──��───────────────────────────────────────────────────────
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

        // ── Register ───────────────────────────────────────────────────────
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

        // ── Home ───────────────────────────────────────────────────────────
        composable("home") {
            HomeScreen(
                onNavigateToRoom = { code ->
                    val route = if (code.isNullOrBlank()) "room" else "room?code=$code"
                    navController.navigate(route)
                },
                onOpenSavedDrawing = { roomCode ->
                    navController.navigate("savedDrawing/$roomCode")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // ── Room ────────────────────────────────────────────────────────
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
                    navController.navigate("drawing/$roomCode/$playerId/$playerCount") {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                initialJoinCode = initialCode
            )
        }

        // ── Drawing ────────────────────────────────────────────────────────
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Saved Drawing ────────────────────────────────────────────────────────
        composable(
            route = "savedDrawing/{roomCode}",
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) {
            SavedDrawingScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
