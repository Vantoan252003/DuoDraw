// presentation/ui/DrawingScreen.kt
package com.toan.codraw.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.presentation.components.DrawingCanvas
import com.toan.codraw.presentation.components.DrawingTools
import com.toan.codraw.presentation.viewmodel.DrawingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    roomCode: String = "",
    localPlayerId: Int = 1,
    playerCount: Int = 1,
    onNavigateBack: () -> Unit = {},
    viewModel: DrawingViewModel = hiltViewModel()
) {
    // Ket noi WS ngay khi vao man hinh
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

    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.respondToCompletionRequest(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.respondToCompletionRequest(true) }) {
                    Text("Agree")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.respondToCompletionRequest(false) }) {
                    Text("Decline")
                }
            },
            title = { Text("Complete drawing?") },
            text = { Text("Player 1 wants to complete and save this drawing. Do you agree?") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CoDraw", fontWeight = FontWeight.Bold)
                        if (roomCode.isNotBlank()) {
                            Text(
                                "Room: $roomCode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (roomCode.isNotBlank()) {
                        Button(
                            onClick = viewModel::onCompleteClicked,
                            enabled = viewModel.canRequestCompletion,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (isCompleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                when {
                                    isCompleted -> "Completed"
                                    awaitingGuestApproval -> "Pending"
                                    else -> "Complete"
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = completionMessage.orEmpty(),
                        modifier = Modifier.padding(12.dp),
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            // ── Player 1 pane (top) ───────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                PlayerLabel("Player 1", localPlayerId == 1, MaterialTheme.colorScheme.primary)
                Box(modifier = Modifier.weight(1f)) {
                    DrawingCanvas(
                        strokes = viewModel.player1Strokes,
                        currentPath = currentPath1,
                        currentPathColor = currentPathColor1,
                        currentStrokeWidth = currentPathWidth1,
                        isCurrentPathEraserMode = currentPathEraser1,
                        onDragStart = viewModel::startDrawing,
                        onDrag = viewModel::updateDrawing,
                        onDragEnd = viewModel::finishDrawing,
                        isInputEnabled = localPlayerId == 1 && !isCompleted
                    )
                }
                if (localPlayerId == 1) DrawingTools(viewModel = viewModel)
            }

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)

            // ── Player 2 pane (bottom) ────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                PlayerLabel("Player 2", localPlayerId == 2, MaterialTheme.colorScheme.secondary)
                Box(modifier = Modifier.weight(1f)) {
                    DrawingCanvas(
                        strokes = viewModel.player2Strokes,
                        currentPath = currentPath2,
                        currentPathColor = currentPathColor2,
                        currentStrokeWidth = currentPathWidth2,
                        isCurrentPathEraserMode = currentPathEraser2,
                        onDragStart = viewModel::startDrawing,
                        onDrag = viewModel::updateDrawing,
                        onDragEnd = viewModel::finishDrawing,
                        isInputEnabled = localPlayerId == 2 && !isCompleted
                    )
                }
                if (localPlayerId == 2) DrawingTools(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PlayerLabel(label: String, isLocalPlayer: Boolean, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (isLocalPlayer) {
            Spacer(Modifier.width(6.dp))
            Surface(shape = MaterialTheme.shapes.extraSmall, color = color.copy(alpha = 0.2f)) {
                Text(
                    "YOU",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}