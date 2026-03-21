package com.toan.codraw.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.R
import com.toan.codraw.data.remote.dto.FriendshipDto
import com.toan.codraw.data.remote.dto.ProfileResponseDto
import com.toan.codraw.presentation.util.UiText
import com.toan.codraw.presentation.viewmodel.FriendsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val friends = viewModel.friendsList
    val requests = viewModel.pendingRequests
    val sentRequests = viewModel.sentRequests
    
    var showAddDialog by remember { mutableStateOf(false) }
    var targetUsername by remember { mutableStateOf("") }
    var errorDialogText by remember { mutableStateOf<UiText?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.friends_and_chat), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.add_friend), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (requests.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            shadowElevation = 2.dp,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.friend_requests, requests.size),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                                Column {
                                    requests.forEach { req ->
                                        FriendRequestItem(
                                            request = req,
                                            onAccept = { viewModel.respondToRequest(req.id, true) },
                                            onReject = { viewModel.respondToRequest(req.id, false) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (sentRequests.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.sent_requests, sentRequests.size),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                )
                                Column {
                                    sentRequests.forEach { req ->
                                        SentRequestItem(request = req)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.my_friends, friends.size),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                if (friends.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PersonAdd, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp), 
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_friends_yet),
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showAddDialog = true }) {
                                    Text(stringResource(R.string.add_friend))
                                }
                            }
                        }
                    }
                } else {
                    items(friends) { friend ->
                        FriendItemUI(
                            friend = friend,
                            onClick = { onNavigateToChat(friend.username) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_friend), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = targetUsername,
                    onValueChange = { targetUsername = it },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetUsername.isNotBlank()) {
                            viewModel.sendFriendRequest(
                                username = targetUsername,
                                onSuccess = {
                                    showAddDialog = false
                                    targetUsername = ""
                                    viewModel.loadFriendsData()
                                    showSuccessDialog = true
                                },
                                onError = { uiTextError ->
                                    errorDialogText = uiTextError
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.send))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    errorDialogText?.let { uiText ->
        AlertDialog(
            onDismissRequest = { errorDialogText = null },
            title = { Text(stringResource(R.string.unknown_error)) },
            text = { Text(uiText.asString(context)) },
            confirmButton = {
                Button(onClick = { errorDialogText = null }) {
                    Text(stringResource(R.string.agree))
                }
            }
        )
    }

    if (showSuccessDialog) {
        val successMessage = stringResource(R.string.request_sent)
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("CoDraw") },
            text = { Text(successMessage) },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text(stringResource(R.string.agree))
                }
            }
        )
    }
}

@Composable
fun FriendItemUI(friend: ProfileResponseDto, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.displayName.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = friend.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "@${friend.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FriendRequestItem(request: FriendshipDto, onAccept: () -> Unit, onReject: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val user = request.requester
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.Bold)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(
            onClick = onAccept,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.accept))
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = onReject,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.reject))
        }
    }
}

@Composable
fun SentRequestItem(request: FriendshipDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val user = request.receiver
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.Bold)
            Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.HourglassEmpty, 
            contentDescription = stringResource(R.string.pending),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}
