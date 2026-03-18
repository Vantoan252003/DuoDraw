package com.toan.codraw.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
    isInputEnabled: Boolean = true,
    // External shared pan offset (synced between players). Null = use local state.
    sharedOffsetX: Float = 0f,
    sharedOffsetY: Float = 0f,
    onPanChanged: ((dx: Float, dy: Float) -> Unit)? = null
) {
    // Local scale state (zoom is personal, not synced)
    var scale by remember { mutableFloatStateOf(1f) }

    // Two-finger gesture: pinch to zoom (local) + pan (shared/synced)
    val transformGestureModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(0.5f, 5f)
            // Notify parent of pan delta so it can sync with peer
            onPanChanged?.invoke(pan.x, pan.y)
        }
    }

    val offsetX = sharedOffsetX
    val offsetY = sharedOffsetY

    // Single-finger drawing gesture
    val drawGestureModifier = if (isInputEnabled) {
        Modifier.pointerInput(scale, offsetX, offsetY) {
            detectDragGestures(
                onDragStart = { offset ->
                    val canvasX = (offset.x - offsetX) / scale
                    val canvasY = (offset.y - offsetY) / scale
                    onDragStart(canvasX, canvasY)
                },
                onDrag = { change, _ ->
                    val canvasX = (change.position.x - offsetX) / scale
                    val canvasY = (change.position.y - offsetY) / scale
                    onDrag(canvasX, canvasY)
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
            .then(transformGestureModifier)
            .then(drawGestureModifier)
    ) {
        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
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
}
