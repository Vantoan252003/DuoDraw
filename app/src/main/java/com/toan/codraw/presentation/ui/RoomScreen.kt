package com.toan.codraw.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.toan.codraw.R
import com.toan.codraw.presentation.viewmodel.RoomUiState
import com.toan.codraw.presentation.viewmodel.RoomViewModel
import com.toan.codraw.ui.theme.GradientEnd
import com.toan.codraw.ui.theme.GradientMint
import com.toan.codraw.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    onRoomReady: (roomCode: String, playerId: Int, playerCount: Int) -> Unit,
    onNavigateBack: () -> Unit,
    initialJoinCode: String = "",
    viewModel: RoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialJoinCode) {
        if (initialJoinCode.isNotBlank()) {
            viewModel.updateJoinCode(initialJoinCode)
            viewModel.joinRoom()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is RoomUiState.Success) {
            val room = (uiState as RoomUiState.Success).room
            val playerId = if (room.hostUsername == viewModel.username) 1 else 2
            onRoomReady(room.roomCode, playerId, room.playerCount)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawing_room), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            GradientStart.copy(alpha = 0.15f),
                            GradientEnd.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

            Text(
                stringResource(R.string.home_greeting, viewModel.username),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // === TAO PHONG ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.create_room_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.create_room_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = viewModel.selectedRoomType == "PUBLIC",
                            onClick = { viewModel.updateSelectedRoomType("PUBLIC") },
                            label = { Text(stringResource(R.string.public_room)) }
                        )
                        FilterChip(
                            selected = viewModel.selectedRoomType == "PRIVATE",
                            onClick = { viewModel.updateSelectedRoomType("PRIVATE") },
                            label = { Text(stringResource(R.string.private_room)) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.createRoom() },
                        enabled = uiState !is RoomUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is RoomUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(
                                R.string.create_room_button,
                                if (viewModel.selectedRoomType == "PUBLIC") {
                                    stringResource(R.string.public_room).lowercase()
                                } else {
                                    stringResource(R.string.private_room).lowercase()
                                }
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  ${stringResource(R.string.or_label)}  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // === THAM GIA PHONG ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.join_room_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.join_room_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.joinCode,
                        onValueChange = viewModel::updateJoinCode,
                        label = { Text(stringResource(R.string.join_room_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.joinRoom() },
                        enabled = uiState !is RoomUiState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (uiState is RoomUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (initialJoinCode.isNotBlank()) stringResource(R.string.joining_room_button) else stringResource(R.string.join_room_button))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Error
            AnimatedVisibility(visible = uiState is RoomUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as? RoomUiState.Error)?.message ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
}
