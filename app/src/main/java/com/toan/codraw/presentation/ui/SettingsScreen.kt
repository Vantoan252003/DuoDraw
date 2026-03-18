package com.toan.codraw.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.toan.codraw.R
import com.toan.codraw.presentation.viewmodel.SettingsViewModel
import com.toan.codraw.ui.theme.GradientEnd
import com.toan.codraw.ui.theme.GradientStart

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                viewModel.uploadAvatar(
                    fileName = uri.fileName(),
                    bytes = bytes,
                    mimeType = context.contentResolver.getType(uri) ?: "image/*"
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Bright gradient header ─────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(GradientStart, Color(0xFF9B59B6), GradientEnd)
                        )
                    )
                    .statusBarsPadding()
                    .padding(vertical = 20.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Profile Section ──────────────────────────────
                SectionHeader(icon = Icons.Default.Person, title = stringResource(R.string.profile_section))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // ── Avatar + Info Row ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar with camera overlay
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable(
                                        enabled = !viewModel.isSaving,
                                        onClick = { avatarPicker.launch("image/*") }
                                    )
                            ) {
                                // Gradient ring
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(GradientStart, GradientEnd))
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    if (profile.avatarUrl.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = profile.displayName.take(1).uppercase(),
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GradientStart
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = profile.avatarUrl,
                                            contentDescription = stringResource(R.string.avatar),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                // Camera badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(GradientStart)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Name + username info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF2B3A67)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "@${profile.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6C7BA8)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = profile.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6C7BA8)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── Display Name Field ──
                        OutlinedTextField(
                            value = viewModel.displayName,
                            onValueChange = viewModel::updateDisplayName,
                            label = { Text(stringResource(R.string.display_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GradientStart,
                                focusedLabelColor = GradientStart
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        // ── Save Button ──
                        Button(
                            onClick = viewModel::saveProfile,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GradientStart,
                                disabledContainerColor = GradientStart.copy(alpha = 0.5f)
                            )
                        ) {
                            if (viewModel.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.saving), color = Color.White)
                            } else {
                                Text(
                                    stringResource(R.string.save_changes),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Status Message ──────────────────────────────
                if (viewModel.statusMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = viewModel.statusMessage.orEmpty(),
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Language Section ─────────────────────────────
                SectionHeader(icon = Icons.Default.Language, title = stringResource(R.string.appearance_section))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.language),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.language_change_restart),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = viewModel.selectedLanguageTag == "en",
                                onClick = {
                                    viewModel.updateLanguage("en")
                                    applyLanguageAndRestart(context, "en")
                                },
                                label = { Text(stringResource(R.string.english)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GradientStart,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = viewModel.selectedLanguageTag == "vi",
                                onClick = {
                                    viewModel.updateLanguage("vi")
                                    applyLanguageAndRestart(context, "vi")
                                },
                                label = { Text(stringResource(R.string.vietnamese)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GradientStart,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = GradientStart
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun applyLanguageAndRestart(context: android.content.Context, tag: String) {
    // Language is saved via sessionManager; we just need to restart the Activity
    // so attachBaseContext picks up the new locale.
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
}



private fun Uri.fileName(): String =
    lastPathSegment?.substringAfterLast('/') ?: "avatar_${System.currentTimeMillis()}.jpg"
