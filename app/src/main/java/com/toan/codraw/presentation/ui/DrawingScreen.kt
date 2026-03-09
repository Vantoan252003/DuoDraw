// presentation/ui/DrawingScreen.kt
package com.toan.codraw.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onNavigateBack: () -> Unit = {},
    viewModel: DrawingViewModel = hiltViewModel()
) {
    // Ket noi WS ngay khi vao man hinh
    LaunchedEffect(roomCode, localPlayerId) {
        if (roomCode.isNotBlank()) {
            viewModel.connect(roomCode, localPlayerId)
        } else {
            viewModel.localPlayerId = localPlayerId
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    val currentPath1 by viewModel.currentPath1
    val currentPath2 by viewModel.currentPath2
    val isCompleting by viewModel.isCompleting
    val completionMessage by viewModel.completionMessage
    val isCompleted by viewModel.isCompleted

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
                            onClick = viewModel::completeDrawing,
                            enabled = !isCompleting && !isCompleted,
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
                            Text(if (isCompleted) "Completed" else "Complete")
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
                        currentColor = viewModel.currentColor,
                        currentStrokeWidth = viewModel.currentWidth,
                        isEraserMode = viewModel.isEraserMode,
                        onDragStart = { x, y -> if (localPlayerId == 1) viewModel.startDrawing(x, y) },
                        onDrag = { x, y -> if (localPlayerId == 1) viewModel.updateDrawing(x, y) },
                        onDragEnd = { if (localPlayerId == 1) viewModel.finishDrawing() }
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
                        currentColor = viewModel.currentColor,
                        currentStrokeWidth = viewModel.currentWidth,
                        isEraserMode = viewModel.isEraserMode,
                        onDragStart = { x, y -> if (localPlayerId == 2) viewModel.startDrawing(x, y) },
                        onDrag = { x, y -> if (localPlayerId == 2) viewModel.updateDrawing(x, y) },
                        onDragEnd = { if (localPlayerId == 2) viewModel.finishDrawing() }
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