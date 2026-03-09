package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.model.Point
import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.DrawingRepository
import com.toan.codraw.domain.usecase.ReceiveDrawEventsUseCase
import com.toan.codraw.domain.usecase.SendDrawEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds a Compose-friendly stroke: a pre-built Path + visual properties. */
data class StrokeUi(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean
)

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val sendDrawEventUseCase: SendDrawEventUseCase,
    private val receiveDrawEventsUseCase: ReceiveDrawEventsUseCase,
    private val drawingRepository: DrawingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // ── Player strokes ────────────────────────────────────────────────────────
    val player1Strokes = mutableStateListOf<StrokeUi>()
    private val player1StrokeData = mutableStateListOf<Stroke>()
    private val _currentPoints1 = mutableListOf<Point>()
    val currentPath1 = mutableStateOf(Path())

    val player2Strokes = mutableStateListOf<StrokeUi>()
    private val player2StrokeData = mutableStateListOf<Stroke>()
    private val _currentPoints2 = mutableListOf<Point>()
    val currentPath2 = mutableStateOf(Path())

    private val allStrokeData = mutableListOf<Stroke>()
    private val seenStrokeIds = mutableSetOf<String>()
    private var receiveJob: Job? = null

    // ── Settings ──────────────────────────────────────────────────────────────
    var currentColor = Color.Black
    var isEraserMode = false
    var currentWidth = 5f
    var localPlayerId = 1
    private var roomCode: String = ""

    val isCompleting = mutableStateOf(false)
    val completionMessage = mutableStateOf<String?>(null)
    val isCompleted = mutableStateOf(false)

    val palette = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green,
        Color(0xFFFF9800), Color(0xFF9C27B0), Color.Cyan, Color.Yellow
    )

    // ── Connect to WS room ────────────────────────────────────────────────────
    fun connect(roomCode: String, playerId: Int) {
        resetCanvasState()
        this.roomCode = roomCode
        this.localPlayerId = playerId
        val baseUrl = sessionManager.getBaseUrl()
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        val wsUrl = "$baseUrl/ws/draw?roomCode=$roomCode"
        drawingRepository.connect(wsUrl)

        receiveJob?.cancel()
        receiveJob = viewModelScope.launch {
            receiveDrawEventsUseCase().collect { stroke ->
                // CLEAR signal: xoa canvas
                if (stroke.colorHex == "#CLEAR") {
                    clearLocalState()
                    return@collect
                }
                appendStrokeIfNeeded(stroke)
            }
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        drawingRepository.disconnect()
    }

    // ── Drawing events ────────────────────────────────────────────────────────
    fun startDrawing(x: Float, y: Float) {
        if (isCompleted.value) return
        if (localPlayerId == 1) {
            _currentPoints1.clear()
            _currentPoints1.add(Point(x, y))
            currentPath1.value = Path().apply { moveTo(x, y) }
        } else {
            _currentPoints2.clear()
            _currentPoints2.add(Point(x, y))
            currentPath2.value = Path().apply { moveTo(x, y) }
        }
    }

    fun updateDrawing(x: Float, y: Float) {
        if (isCompleted.value) return
        if (localPlayerId == 1) {
            _currentPoints1.add(Point(x, y))
            currentPath1.value = buildPath(_currentPoints1)
        } else {
            _currentPoints2.add(Point(x, y))
            currentPath2.value = buildPath(_currentPoints2)
        }
    }

    fun finishDrawing() {
        if (isCompleted.value) return
        val points = if (localPlayerId == 1) _currentPoints1.toList() else _currentPoints2.toList()
        if (points.isEmpty()) return
        val color = if (isEraserMode) Color.White else currentColor
        val width = if (isEraserMode) 30f else currentWidth
        val stroke = Stroke(
            points = points,
            colorHex = colorToHex(color),
            strokeWidth = width,
            isEraser = isEraserMode,
            playerId = localPlayerId
        )
        appendStrokeIfNeeded(stroke)
        if (localPlayerId == 1) {
            currentPath1.value = Path()
            _currentPoints1.clear()
        } else {
            currentPath2.value = Path()
            _currentPoints2.clear()
        }
        sendDrawEventUseCase(stroke)
    }

    fun clearCanvas() {
        if (isCompleted.value) return
        clearLocalState()
        // Gui CLEAR event toi server
        drawingRepository.sendStroke(
            Stroke(
                points = emptyList(),
                colorHex = "#CLEAR",
                strokeWidth = 0f,
                isEraser = false,
                playerId = localPlayerId
            )
        )
    }

    fun completeDrawing() {
        if (roomCode.isBlank()) {
            completionMessage.value = "Complete is only available for online rooms."
            return
        }
        if (allStrokeData.isEmpty()) {
            completionMessage.value = "Nothing to save yet."
            return
        }
        viewModelScope.launch {
            isCompleting.value = true
            completionMessage.value = null
            drawingRepository.completeDrawing(roomCode, allStrokeData.toList()).fold(
                onSuccess = {
                    isCompleted.value = true
                    completionMessage.value = "Saved ${it.strokeCount} strokes for room ${it.roomCode}."
                },
                onFailure = {
                    completionMessage.value = it.message ?: "Failed to complete drawing."
                }
            )
            isCompleting.value = false
        }
    }

    fun setColor(color: Color) {
        if (isCompleted.value) return
        currentColor = color
        isEraserMode = false
    }

    fun setEraser() {
        if (isCompleted.value) return
        isEraserMode = true
    }

    fun dismissCompletionMessage() {
        completionMessage.value = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun appendStrokeIfNeeded(stroke: Stroke) {
        if (!seenStrokeIds.add(stroke.id)) return
        allStrokeData.add(stroke)
        val ui = StrokeUi(
            path = buildPath(stroke.points),
            color = parseColor(stroke.colorHex),
            strokeWidth = stroke.strokeWidth,
            isEraser = stroke.isEraser
        )
        if (stroke.playerId == 1) {
            player1StrokeData.add(stroke)
            player1Strokes.add(ui)
        } else {
            player2StrokeData.add(stroke)
            player2Strokes.add(ui)
        }
    }

    private fun resetCanvasState() {
        clearLocalState()
        roomCode = ""
        isCompleted.value = false
        completionMessage.value = null
    }

    private fun clearLocalState() {
        player1Strokes.clear()
        player1StrokeData.clear()
        player2Strokes.clear()
        player2StrokeData.clear()
        allStrokeData.clear()
        seenStrokeIds.clear()
        _currentPoints1.clear()
        _currentPoints2.clear()
        currentPath1.value = Path()
        currentPath2.value = Path()
    }

    private fun buildPath(points: List<Point>) = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }

    private fun colorToHex(color: Color): String {
        val argb = color.value.toLong() ushr 32
        return "#%08X".format(argb)
    }

    private fun parseColor(hex: String): Color {
        if (hex == "#CLEAR") return Color.White
        return try {
            Color(hex.trimStart('#').toLong(16).toInt())
        } catch (_: Exception) {
            Color.Black
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}