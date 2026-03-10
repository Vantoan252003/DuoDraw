package com.toan.codraw.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.toan.codraw.R
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.model.UserProfile
import com.toan.codraw.domain.repository.RoomResult
import com.toan.codraw.presentation.viewmodel.HomeViewModel
import com.toan.codraw.presentation.viewmodel.PublicRoomJoinState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoom: (String?) -> Unit,
    onEnterDrawingRoom: (roomCode: String, playerId: Int, playerCount: Int) -> Unit,
    onOpenSavedDrawing: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val publicRooms by viewModel.publicRooms.collectAsState()
    val savedDrawings by viewModel.savedDrawings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val publicRoomJoinState by viewModel.publicRoomJoinState.collectAsState()
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(publicRoomJoinState) {
        val joinState = publicRoomJoinState as? PublicRoomJoinState.Success ?: return@LaunchedEffect
        val playerId = if (joinState.room.hostUsername == viewModel.loggedInUsername) 1 else 2
        onEnterDrawingRoom(joinState.room.roomCode, playerId, joinState.room.playerCount)
        viewModel.consumePublicRoomJoin()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = viewModel::refreshHomeData) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_data))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    TextButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroProfileCard(
                    profile = profile,
                    isLoading = isLoading,
                    onOpenSettings = onOpenSettings
                )
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.quick_actions),
                    subtitle = stringResource(R.string.profile_card_subtitle)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { onNavigateToRoom(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.create_or_join_room))
                    }
                    OutlinedButton(
                        onClick = { onNavigateToRoom(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.draw_offline))
                    }
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.saved_drawings),
                    subtitle = stringResource(R.string.saved_drawings_subtitle)
                )
            }

            if (!isLoading && savedDrawings.isEmpty()) {
                item { EmptyStateCard(stringResource(R.string.no_saved_drawings)) }
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
                        title = stringResource(R.string.public_rooms),
                        subtitle = stringResource(R.string.public_rooms_subtitle)
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (!isLoading && publicRooms.isEmpty()) {
                item { EmptyStateCard(stringResource(R.string.no_public_rooms)) }
            }

            items(publicRooms, key = { it.id }) { room ->
                val isJoiningThisRoom = (publicRoomJoinState as? PublicRoomJoinState.Loading)?.roomCode == room.roomCode
                PublicRoomCard(
                    room = room,
                    isJoining = isJoiningThisRoom,
                    onJoin = {
                        viewModel.clearError()
                        viewModel.joinPublicRoom(room.roomCode)
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroProfileCard(
    profile: UserProfile,
    isLoading: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (profile.avatarUrl.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.displayName.take(1).uppercase(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_greeting, profile.displayName),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenSettings) {
                        Text(stringResource(R.string.settings))
                    }
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
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
            Text(stringResource(R.string.room_code, drawing.roomCode), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.saved_by, drawing.savedByUsername))
            Text(stringResource(R.string.strokes_count, drawing.strokeCount))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_drawing))
            }
        }
    }
}

@Composable
private fun PublicRoomCard(
    room: RoomResult,
    isJoining: Boolean,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.room_code_label, room.roomCode), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.host_label, room.hostUsername))
            Text(stringResource(R.string.room_type_label, room.roomType))
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onJoin,
                enabled = !isJoining,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.joining_room))
                } else {
                    Text(stringResource(R.string.join_now))
                }
            }
        }
    }
}
