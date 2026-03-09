// presentation/components/DrawingTools.kt
package com.toan.codraw.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toan.codraw.presentation.viewmodel.DrawingViewModel

@Composable
fun DrawingTools(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val currentColor by viewModel.currentColorState
    val isEraserMode by viewModel.isEraserModeState
    val currentWidth by viewModel.currentWidthState

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Color palette row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.palette.forEach { color ->
                    val isSelected = !isEraserMode && currentColor == color
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

            Spacer(modifier = Modifier.height(6.dp))

            // Tools row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Stroke width slider
                Text("Size", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = currentWidth,
                    onValueChange = viewModel::setStrokeWidth,
                    valueRange = 2f..40f,
                    modifier = Modifier.weight(1f)
                )

                // Eraser button
                IconButton(
                    onClick = { viewModel.setEraser() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEraserMode)
                            MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Eraser")
                }

                // Clear button
                IconButton(onClick = { viewModel.clearCanvas() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        }
    }
}