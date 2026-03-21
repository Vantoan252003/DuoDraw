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
import com.toan.codraw.presentation.viewmodel.MainViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NavGraph(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    var topNotificationMessage by remember { mutableStateOf<com.toan.codraw.data.remote.dto.ChatMessageResponseDto?>(null) }

    val startDestination = if (authViewModel.isAlreadyLoggedIn()) "home" else "login"

    LaunchedEffect(Unit) {
        val currentUsername = sessionManager.getUsername()
        mainViewModel.globalWebSocketManager.listener.chatMessages.collectLatest { msg ->
            if (msg.senderUsername != currentUsername) {
                val currentEntry = navController.currentBackStackEntry
                val currentRoute = currentEntry?.destination?.route
                
                // Extract arguments correctly
                val inChatWithSender = currentRoute == "chat/{friendUsername}" && currentEntry.arguments?.getString("friendUsername") == msg.senderUsername
                val inDrawingRoom = currentRoute?.startsWith("drawing/") == true || currentRoute?.startsWith("room") == true
                
                if (!inChatWithSender && !inDrawingRoom) {
                    topNotificationMessage = msg
                    launch {
                        delay(3000)
                        if (topNotificationMessage == msg) {
                            topNotificationMessage = null
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.fillMaxSize()) {
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
    } // closes NavHost
        
    // Custom Top Overlay Notification
    AnimatedVisibility(
        visible = topNotificationMessage != null,
            enter = slideInVertically(initialOffsetY = { -it - 100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it - 100 }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            topNotificationMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 48.dp)
                        .fillMaxWidth()
                        .clickable {
                            topNotificationMessage = null
                            navController.navigate("chat/${msg.senderUsername}")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(com.toan.codraw.ui.theme.GradientStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg.senderUsername.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = msg.senderUsername,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            val preview = if (msg.type == "VOICE" || msg.content.startsWith("http")) "[Voice message]" else msg.content
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    } // closes Box
} // closes fun NavGraph
