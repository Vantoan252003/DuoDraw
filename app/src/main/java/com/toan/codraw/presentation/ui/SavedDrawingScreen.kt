package com.toan.codraw.presentation.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import com.toan.codraw.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.presentation.components.DrawingCanvas
import com.toan.codraw.presentation.viewmodel.SavedDrawingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDrawingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedDrawingViewModel = hiltViewModel()
) {
    val drawing by viewModel.drawing.collectAsState()
    val allStrokes by viewModel.allStrokes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_drawing_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (errorMessage != null) {
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            drawing?.let { saved ->
                Text(stringResource(R.string.room_code, saved.roomCode), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(stringResource(R.string.saved_by, saved.savedByUsername))
                Text(stringResource(R.string.completed_at, saved.completedAt))
                Spacer(Modifier.height(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    SavedDrawingPlayerLabel(stringResource(R.string.completed), MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.weight(1f)) {
                        DrawingCanvas(
                            strokes = allStrokes,
                            currentPath1 = Path(),
                            currentPathColor1 = Color.Black,
                            currentStrokeWidth1 = 5f,
                            isCurrentPathEraserMode1 = false,
                            currentPath2 = Path(),
                            currentPathColor2 = Color.Black,
                            currentStrokeWidth2 = 5f,
                            isCurrentPathEraserMode2 = false,
                            onDragStart = { _, _ -> },
                            onDrag = { _, _ -> },
                            onDragEnd = {},
                            isInputEnabled = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedDrawingPlayerLabel(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
