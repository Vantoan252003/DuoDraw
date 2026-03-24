package com.toan.codraw.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.positionChange
import com.toan.codraw.presentation.viewmodel.StrokeUi

@Composable
fun DrawingCanvas(
    strokes: List<StrokeUi>,
    currentPath1: Path,
    currentPathColor1: Color,
    currentStrokeWidth1: Float,
    isCurrentPathEraserMode1: Boolean,
    currentPath2: Path,
    currentPathColor2: Color,
    currentStrokeWidth2: Float,
    isCurrentPathEraserMode2: Boolean,
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

    val drawGestureModifier = if (isInputEnabled) {
        Modifier.pointerInput(scale, offsetX, offsetY) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isDrawing = true

                val canvasX = (down.position.x - offsetX) / scale
                val canvasY = (down.position.y - offsetY) / scale
                onDragStart(canvasX, canvasY)

                do {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }
                    val pointerCount = pressedChanges.size

                    if (pointerCount > 1) {
                        if (isDrawing) {
                            onDragEnd()
                            isDrawing = false
                        }
                    } else if (isDrawing && pointerCount == 1) {
                        val change = pressedChanges.firstOrNull { it.id == down.id }
                        if (change != null) {
                            val cx = (change.position.x - offsetX) / scale
                            val cy = (change.position.y - offsetY) / scale
                            onDrag(cx, cy)
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                        }
                    }
                } while (pressedChanges.isNotEmpty())

                if (isDrawing) {
                    onDragEnd()
                }
            }
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

            // Draw the in-progress stroke for player 1
            if (!currentPath1.isEmpty) {
                drawPath(
                    path = currentPath1,
                    color = if (isCurrentPathEraserMode1) Color.Transparent else currentPathColor1,
                    style = Stroke(width = currentStrokeWidth1, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (isCurrentPathEraserMode1) BlendMode.Clear else BlendMode.SrcOver
                )
            }

            // Draw the in-progress stroke for player 2
            if (!currentPath2.isEmpty) {
                drawPath(
                    path = currentPath2,
                    color = if (isCurrentPathEraserMode2) Color.Transparent else currentPathColor2,
                    style = Stroke(width = currentStrokeWidth2, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (isCurrentPathEraserMode2) BlendMode.Clear else BlendMode.SrcOver
                )
            }
        }
    }
}
