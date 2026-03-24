package com.toan.codraw.presentation.viewmodel

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.repository.DrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedDrawingViewModel @Inject constructor(
    private val drawingRepository: DrawingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomCode: String = savedStateHandle["roomCode"] ?: ""

    private val _drawing = MutableStateFlow<CompletedDrawing?>(null)
    val drawing: StateFlow<CompletedDrawing?> = _drawing

    private val _allStrokes = MutableStateFlow<List<StrokeUi>>(emptyList())
    val allStrokes: StateFlow<List<StrokeUi>> = _allStrokes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadDrawing()
    }

    fun loadDrawing() {
        if (roomCode.isBlank()) {
            _errorMessage.value = "Missing room code"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            drawingRepository.getCompletedDrawing(roomCode).fold(
                onSuccess = { drawing ->
                    _drawing.value = drawing
                    _allStrokes.value = drawing.strokes.map(::toStrokeUi)
                },
                onFailure = {
                    _errorMessage.value = it.message ?: "Could not load drawing"
                }
            )
            _isLoading.value = false
        }
    }

    private fun toStrokeUi(stroke: com.toan.codraw.domain.model.Stroke): StrokeUi = StrokeUi(
        id = stroke.id,
        path = Path().apply {
            stroke.points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        },
        color = parseColor(stroke.colorHex),
        strokeWidth = stroke.strokeWidth,
        isEraser = stroke.isEraser
    )

    private fun parseColor(hex: String): Color {
        return try {
            Color(AndroidColor.parseColor(hex))
        } catch (_: Exception) {
            Color.Black
        }
    }
}
