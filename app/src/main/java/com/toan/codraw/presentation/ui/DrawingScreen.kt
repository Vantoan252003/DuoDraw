// presentation/ui/DrawingScreen.kt
package com.toan.codraw.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.R
import com.toan.codraw.presentation.components.ChatOverlay
import com.toan.codraw.presentation.components.DrawingCanvas
import com.toan.codraw.presentation.components.DrawingTools
import com.toan.codraw.presentation.viewmodel.DrawingViewModel

@Composable
fun DrawingScreen(
    roomCode: String = "",
    localPlayerId: Int = 1,
    playerCount: Int = 1,
    onNavigateBack: () -> Unit = {},
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(roomCode, localPlayerId, playerCount) {
        if (roomCode.isNotBlank()) {
            viewModel.connect(roomCode, localPlayerId, playerCount)
        } else {
            viewModel.localPlayerId = localPlayerId
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    BackHandler(onBack = onNavigateBack)

    var isChatOpen by remember { androidx.compose.runtime.mutableStateOf(false) }

    val currentPath1 by viewModel.currentPath1
    val currentPath2 by viewModel.currentPath2
    val currentPathColor1 by viewModel.currentPathColor1
    val currentPathColor2 by viewModel.currentPathColor2
    val currentPathWidth1 by viewModel.currentPathWidth1
    val currentPathWidth2 by viewModel.currentPathWidth2
    val currentPathEraser1 by viewModel.currentPathEraser1
    val currentPathEraser2 by viewModel.currentPathEraser2
    val isCompleting by viewModel.isCompleting
    val completionMessage by viewModel.completionMessage
    val isCompleted by viewModel.isCompleted
    val showApprovalDialog by viewModel.showCompletionApprovalDialog
    val awaitingGuestApproval by viewModel.awaitingGuestApproval
    val sharedOffsetX by viewModel.sharedOffsetX
    val sharedOffsetY by viewModel.sharedOffsetY

    if (showApprovalDialog) {
        val requestingPeerId = if (localPlayerId == 1) 2 else 1
        AlertDialog(
            onDismissRequest = { viewModel.respondToCompletionRequest(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.respondToCompletionRequest(true) }) {
                    Text(stringResource(R.string.agree))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.respondToCompletionRequest(false) }) {
                    Text(stringResource(R.string.decline))
                }
            },
            title = { Text(stringResource(R.string.complete_drawing_prompt_title)) },
            text = { Text(stringResource(R.string.msg_peer_requested_completion, requestingPeerId)) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            CanvasPane(
                strokes = viewModel.player1Strokes,
                currentPath = currentPath1,
                currentPathColor = currentPathColor1,
                currentStrokeWidth = currentPathWidth1,
                isCurrentPathEraserMode = currentPathEraser1,
                isInputEnabled = localPlayerId == 1 && !isCompleted,
                isLocalPane = localPlayerId == 1,
                onDragStart = viewModel::startDrawing,
                onDrag = viewModel::updateDrawing,
                onDragEnd = viewModel::finishDrawing,
                sharedOffsetX = sharedOffsetX,
                sharedOffsetY = sharedOffsetY,
                onPanChanged = viewModel::onPanDelta,
                modifier = Modifier.weight(1f)
            )
            CanvasPane(
                strokes = viewModel.player2Strokes,
                currentPath = currentPath2,
                currentPathColor = currentPathColor2,
                currentStrokeWidth = currentPathWidth2,
                isCurrentPathEraserMode = currentPathEraser2,
                isInputEnabled = localPlayerId == 2 && !isCompleted,
                isLocalPane = localPlayerId == 2,
                onDragStart = viewModel::startDrawing,
                onDrag = viewModel::updateDrawing,
                onDragEnd = viewModel::finishDrawing,
                sharedOffsetX = sharedOffsetX,
                sharedOffsetY = sharedOffsetY,
                onPanChanged = viewModel::onPanDelta,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Text(
                        text = if (roomCode.isBlank()) {
                            stringResource(R.string.offline_player, localPlayerId)
                        } else {
                            stringResource(R.string.room_player, roomCode, localPlayerId)
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (roomCode.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isChatOpen = !isChatOpen
                            if (isChatOpen) {
                                viewModel.markMessagesAsRead()
                            }
                        },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            MaterialTheme.shapes.medium
                        )
                    ) {
                        BadgedBox(
                            badge = {
                                if (viewModel.hasUnreadMessages.value && !isChatOpen) {
                                    Badge { Text("") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (localPlayerId == 1) {
                        Button(onClick = viewModel::onCompleteClicked, enabled = viewModel.canRequestCompletion) {
                            if (isCompleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    when {
                                        isCompleted -> stringResource(R.string.completed)
                                        awaitingGuestApproval -> stringResource(R.string.pending)
                                        else -> stringResource(R.string.complete)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (completionMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = completionMessage?.asString() ?: "",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }

        if (!isCompleted) {
            DrawingTools(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .navigationBarsPadding()
                    .padding(start = 0.dp, top = 60.dp, bottom = 12.dp)
            )
        }

        if (isChatOpen) {
            ChatOverlay(
                messages = viewModel.chatMessages,
                localPlayerId = localPlayerId,
                onSendMessage = viewModel::sendChatMessage,
                onClose = { isChatOpen = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, top = 60.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun CanvasPane(
    strokes: List<com.toan.codraw.presentation.viewmodel.StrokeUi>,
    currentPath: androidx.compose.ui.graphics.Path,
    currentPathColor: androidx.compose.ui.graphics.Color,
    currentStrokeWidth: Float,
    isCurrentPathEraserMode: Boolean,
    isInputEnabled: Boolean,
    isLocalPane: Boolean,
    onDragStart: (Float, Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    sharedOffsetX: Float = 0f,
    sharedOffsetY: Float = 0f,
    onPanChanged: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        DrawingCanvas(
            strokes = strokes,
            currentPath = currentPath,
            currentPathColor = currentPathColor,
            currentStrokeWidth = currentStrokeWidth,
            isCurrentPathEraserMode = isCurrentPathEraserMode,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            isInputEnabled = isInputEnabled,
            sharedOffsetX = sharedOffsetX,
            sharedOffsetY = sharedOffsetY,
            onPanChanged = onPanChanged
        )
        if (isLocalPane) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
            ) {
                Text(
                    text = stringResource(R.string.you_label),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
