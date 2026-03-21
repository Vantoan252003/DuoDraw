package com.toan.codraw.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.toan.codraw.R
import com.toan.codraw.presentation.util.AudioPlayerHelper
import com.toan.codraw.presentation.util.AudioRecorderHelper
import com.toan.codraw.presentation.viewmodel.PrivateChatViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import com.toan.codraw.ui.theme.GradientStart
import com.toan.codraw.ui.theme.GradientEnd
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    friendUsername: String,
    onNavigateBack: () -> Unit,
    viewModel: PrivateChatViewModel = hiltViewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages = viewModel.chatMessages
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(friendUsername) {
        viewModel.initChat(friendUsername)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            tint = GradientStart
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friendUsername.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = friendUsername, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Active now",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    val isMine = msg.senderUsername != friendUsername
                    
                    // Format timestamp if available (Optional enhancement)
                    val timeString = msg.timestamp?.takeLast(8)?.take(5) ?: ""
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        if (!isMine) {
                            Text(
                                text = msg.senderUsername,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (!isMine) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GradientStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = msg.senderUsername.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (isMine) 20.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 20.dp
                                ),
                                color = if (isMine) GradientStart else Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                if (msg.type == "VOICE") {
                                    VoiceMessageBubble(msg.content, isMine)
                                } else {
                                    val displayContent = msg.content.removeSurrounding("\"")
                                    Text(
                                        text = displayContent,
                                        color = if (isMine) Color.White else Color.Black,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        
                        if (timeString.isNotBlank()) {
                            Text(
                                text = timeString,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp, start = if (isMine) 0.dp else 40.dp, end = if (isMine) 8.dp else 0.dp)
                            )
                        }
                    }
                }
            }

            // Input Area
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { 
                            Text(
                                text = if (isRecording) stringResource(R.string.recording) else stringResource(R.string.message),
                                color = if (isRecording) Color.Red else Color.Gray
                            ) 
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        readOnly = isRecording,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedBorderColor = GradientStart,
                            unfocusedContainerColor = Color(0xFFF5F6F8),
                            focusedContainerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (messageText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(messageText.trim())
                                messageText = ""
                            },
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)), CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.send),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp).padding(start = 4.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(if (isRecording) Color.Red else Color(0xFFE8E8E8), CircleShape)
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                isRecording = true
                                                audioRecorder.startRecording()
                                                tryAwaitRelease()
                                                isRecording = false
                                                val file = audioRecorder.stopRecording()
                                                if (file != null && file.exists() && file.length() > 0) {
                                                    viewModel.sendVoiceMessage(file)
                                                }
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_message),
                                tint = if (isRecording) Color.White else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceMessageBubble(url: String, isMine: Boolean) {
    val playerHelper = remember { AudioPlayerHelper() }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        onDispose {
            playerHelper.stopAudio()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    playerHelper.stopAudio()
                    isPlaying = false
                } else {
                    isPlaying = true
                    playerHelper.playAudio(url) {
                        isPlaying = false
                    }
                }
            }
        ) {
            Icon(
                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isMine) Color.White else Color.Black
            )
        }
        Text(
            text = "Voice Message",
            color = if (isMine) Color.White else Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}
