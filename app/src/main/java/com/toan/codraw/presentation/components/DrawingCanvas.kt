package com.toan.codraw.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.toan.codraw.presentation.viewmodel.StrokeUi

@Composable
fun DrawingCanvas(
    strokes: List<StrokeUi>,
    currentPath: Path,
    currentColor: Color,
    currentStrokeWidth: Float,
    isEraserMode: Boolean,
    onDragStart: (Float, Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onDragStart(offset.x, offset.y) },
                    onDrag = { change, _ ->
                        onDrag(change.position.x, change.position.y)
                        change.consume()
                    },
                    onDragEnd = { onDragEnd() }
                )
            }
    ) {
        // Draw committed strokes
        strokes.forEach { stroke ->
            drawPath(
                path = stroke.path,
                color = stroke.color,
                style = Stroke(width = stroke.strokeWidth)
            )
        }
        // Draw the in-progress stroke
        if (currentPath.isEmpty.not()) {
            val inProgressColor = if (isEraserMode) Color.White else currentColor
            val inProgressWidth = if (isEraserMode) 30f else currentStrokeWidth
            drawPath(
                path = currentPath,
                color = inProgressColor,
                style = Stroke(width = inProgressWidth)
            )
        }
    }
}

