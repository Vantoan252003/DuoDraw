package com.toan.codraw.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.repository.RoomResult
import com.toan.codraw.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoom: (String?) -> Unit,
    onOpenSavedDrawing: (String) -> Unit,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val publicRooms by viewModel.publicRooms.collectAsState()
    val savedDrawings by viewModel.savedDrawings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CoDraw", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::refreshHomeData) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh data")
                        }
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            viewModel.loggedInUsername,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onLogout) { Text("Đăng xuất") }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Xin chào, ${viewModel.loggedInUsername}!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vẽ tranh cùng bạn bè theo thời gian thực",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onNavigateToRoom(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Tạo / Tham gia phòng vẽ", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onNavigateToRoom(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Vẽ offline (không cần phòng)")
                }
            }

            item {
                SectionHeader(
                    title = "Saved drawings",
                    subtitle = "Open and review the drawings you completed"
                )
            }

            if (!isLoading && savedDrawings.isEmpty()) {
                item {
                    EmptyStateCard("You have no saved drawings yet.")
                }
            }

            items(savedDrawings, key = { it.id }) { drawing ->
                SavedDrawingCard(drawing = drawing, onOpen = { onOpenSavedDrawing(drawing.roomCode) })
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(
                        title = "Public rooms",
                        subtitle = "Rooms waiting for a second player"
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.width(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (!isLoading && publicRooms.isEmpty()) {
                item {
                    EmptyStateCard("No public room is waiting right now. Create one to get started.")
                }
            }

            items(publicRooms, key = { it.id }) { room ->
                PublicRoomCard(room = room, onJoin = { onNavigateToRoom(room.roomCode) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SavedDrawingCard(
    drawing: CompletedDrawing,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Room: ${drawing.roomCode}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Saved by: ${drawing.savedByUsername}")
            Text("Strokes: ${drawing.strokeCount}")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text("Open drawing")
            }
        }
    }
}

@Composable
private fun PublicRoomCard(
    room: RoomResult,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mã phòng: ${room.roomCode}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Chủ phòng: ${room.hostUsername}")
            Text("Loại phòng: ${room.roomType}")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                Text("Tham gia ngay")
            }
        }
    }
}
