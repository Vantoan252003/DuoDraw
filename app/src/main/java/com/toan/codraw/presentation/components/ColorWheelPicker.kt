package com.toan.codraw.presentation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.toan.codraw.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ColorWheelPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // Start from current color's HSV
    val hsv = remember(currentColor) {
        val argb = currentColor.hashCode()
        val arr = FloatArray(3)
        AndroidColor.colorToHSV(argb, arr)
        arr
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var brightness by remember { mutableFloatStateOf(1f) }

    val selectedColor = remember(hue, saturation, brightness) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_color),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(20.dp))

                // Color Wheel
                ColorWheel(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onHueSatChanged = { h, s ->
                        hue = h
                        saturation = s
                    },
                    modifier = Modifier.size(220.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Brightness slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.brightness),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    Text(
                        text = "${(brightness * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueSatChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = min(cx, cy)
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist <= radius) {
                        val angle =
                            (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360
                        val sat = (dist / radius).coerceIn(0f, 1f)
                        onHueSatChanged(angle.toFloat(), sat)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = min(cx, cy)
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist <= radius) {
                        val angle =
                            (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360
                        val sat = (dist / radius).coerceIn(0f, 1f)
                        onHueSatChanged(angle.toFloat(), sat)
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)

        // Draw the wheel using concentric rings with hue sweeps
        val steps = 360
        val ringSteps = 20
        for (r in ringSteps downTo 1) {
            val ringRadius = radius * r / ringSteps
            val ringWidth = radius / ringSteps + 1f
            for (i in 0 until steps) {
                val angle = i.toFloat()
                val startAngle = angle - 0.5f
                val sat = r.toFloat() / ringSteps
                val color = Color(AndroidColor.HSVToColor(floatArrayOf(angle, sat, brightness)))
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = 1.5f,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, cy - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = ringWidth)
                )
            }
        }

        // Selection indicator
        val selAngle = hue * PI.toFloat() / 180f
        val selDist = saturation * radius
        val selX = cx + cos(selAngle) * selDist
        val selY = cy + sin(selAngle) * selDist

        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(selX, selY),
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = Color.Black,
            radius = 14f,
            center = Offset(selX, selY),
            style = Stroke(width = 2f)
        )
    }
}
