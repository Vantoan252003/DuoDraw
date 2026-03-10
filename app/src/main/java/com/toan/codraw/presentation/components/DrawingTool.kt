// presentation/components/DrawingTools.kt
package com.toan.codraw.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    DrawingToolMode.PENCIL,
                    DrawingToolMode.PEN,
                    DrawingToolMode.MARKER,
                    DrawingToolMode.ERASER
                ).forEach { tool ->
                    FilterChip(
                        selected = currentTool == tool,
                        onClick = { viewModel.selectTool(tool) },
                        label = { Text(stringResource(tool.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.palette.forEach { color ->
                    val isSelected = !currentTool.isEraser && currentColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setColor(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.size_value, currentWidth.roundToInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = currentWidth,
                    onValueChange = viewModel::setStrokeWidth,
                    valueRange = 2f..32f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (currentTool.isEraser) {
                        stringResource(R.string.opacity_disabled)
                    } else {
                        stringResource(R.string.opacity_value, (currentOpacity * 100).roundToInt())
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = currentOpacity,
                    onValueChange = viewModel::setOpacity,
                    valueRange = 0.1f..1f,
                    enabled = !currentTool.isEraser,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                IconButton(
                    onClick = viewModel::undoLastStroke,
                    enabled = viewModel.canUndoLocalPlayer,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.undo))
                }
                IconButton(
                    onClick = viewModel::clearCanvas,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_canvas))
                }
            }
        }
    }
}