package com.toan.codraw.presentation.model

import androidx.annotation.StringRes
import com.toan.codraw.R

enum class DrawingToolMode(
    @StringRes val labelRes: Int,
    val defaultOpacity: Float
) {
    PENCIL(labelRes = R.string.tool_pencil, defaultOpacity = 0.35f),
    PEN(labelRes = R.string.tool_pen, defaultOpacity = 1f),
    MARKER(labelRes = R.string.tool_marker, defaultOpacity = 0.65f),
    ERASER(labelRes = R.string.tool_eraser, defaultOpacity = 1f);

    val isEraser: Boolean
        get() = this == ERASER
}
