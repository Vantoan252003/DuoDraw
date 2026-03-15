// presentation/components/DrawingTools.kt
package com.toan.codraw.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toan.codraw.R
import com.toan.codraw.presentation.model.DrawingToolMode
import com.toan.codraw.presentation.viewmodel.DrawingViewModel
import kotlin.math.roundToInt

@Composable
fun DrawingTools(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val currentColor by viewModel.currentColorState
    val currentTool by viewModel.currentToolState
    val currentWidth by viewModel.currentWidthState
    val currentOpacity by viewModel.currentOpacityState

    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var showColorWheel by rememberSaveable { mutableStateOf(false) }

    // Color Wheel Dialog
    if (showColorWheel) {
        ColorWheelPickerDialog(
            currentColor = currentColor,
            onColorSelected = { color ->
                viewModel.setColor(color)
                showColorWheel = false
            },
            onDismiss = { showColorWheel = false }
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle button
        Surface(
            shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
            tonalElevation = 8.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            modifier = Modifier.clickable { isExpanded = !isExpanded }
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 0f else 180f,
                label = "toggleRotation"
            )
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = if (isExpanded) {
                    stringResource(R.string.collapse_tools)
                } else {
                    stringResource(R.string.expand_tools)
                },
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 4.dp)
                    .size(20.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Expandable tool panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            Surface(
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .width(200.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Tool Selection ──
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            DrawingToolMode.PENCIL,
                            DrawingToolMode.PEN,
                            DrawingToolMode.MARKER,
                            DrawingToolMode.ERASER
                        ).forEach { tool ->
                            FilterChip(
                                selected = currentTool == tool,
                                onClick = { viewModel.selectTool(tool) },
                                label = {
                                    Text(
                                        stringResource(tool.labelRes),
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }

                    // ── Color Palette ──
                    Text(
                        text = "Colors",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Color swatches in grid (2 rows of 4)
                    val colors = viewModel.palette
                    for (rowStart in colors.indices step 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in rowStart until (rowStart + 4).coerceAtMost(colors.size)) {
                                val color = colors[i]
                                val isSelected = !currentTool.isEraser && currentColor == color
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setColor(color) }
                                )
                            }
                        }
                    }

                    // Color wheel button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clickable { showColorWheel = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = stringResource(R.string.custom_color),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.custom_color),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // ── Size Slider ──
                    Text(
                        text = stringResource(R.string.size_value, currentWidth.roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = currentWidth,
                        onValueChange = viewModel::setStrokeWidth,
                        valueRange = 2f..32f,
                        modifier = Modifier.width(176.dp)
                    )

                    // ── Opacity Slider ──
                    Text(
                        text = if (currentTool.isEraser) {
                            stringResource(R.string.opacity_disabled)
                        } else {
                            stringResource(R.string.opacity_value, (currentOpacity * 100).roundToInt())
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = currentOpacity,
                        onValueChange = viewModel::setOpacity,
                        valueRange = 0.1f..1f,
                        enabled = !currentTool.isEraser,
                        modifier = Modifier.width(176.dp)
                    )

                    // ── Action Buttons ──
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = viewModel::undoLastStroke,
                            enabled = viewModel.canUndoLocalPlayer,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.undo),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = viewModel::clearCanvas,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_canvas),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}