package com.toan.codraw.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.toan.codraw.presentation.viewmodel.StrokeUi

@Composable
fun DrawingCanvas(
    strokes: List<StrokeUi>,
    currentPath: Path,
    currentPathColor: Color,
    currentStrokeWidth: Float,
    isCurrentPathEraserMode: Boolean,
    onDragStart: (Float, Float) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isInputEnabled: Boolean = true
) {
    val gestureModifier = if (isInputEnabled) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset -> onDragStart(offset.x, offset.y) },
                onDrag = { change, _ ->
                    onDrag(change.position.x, change.position.y)
                    change.consume()
                },
                onDragEnd = { onDragEnd() }
            )
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .then(gestureModifier)
    ) {
        val strokeStyle = Stroke(
            width = currentStrokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Draw committed strokes
        strokes.forEach { stroke ->
            drawPath(
                path = stroke.path,
                color = if (stroke.isEraser) Color.Transparent else stroke.color,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
            )
        }

        // Draw the in-progress stroke
        if (!currentPath.isEmpty) {
            drawPath(
                path = currentPath,
                color = if (isCurrentPathEraserMode) Color.Transparent else currentPathColor,
                style = strokeStyle,
                blendMode = if (isCurrentPathEraserMode) BlendMode.Clear else BlendMode.SrcOver
            )
        }
    }
}
