package com.toan.codraw.presentation.viewmodel

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.model.Point
import com.toan.codraw.domain.model.RoomSignal
import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.DrawingRepository
import com.toan.codraw.domain.usecase.ReceiveDrawEventsUseCase
import com.toan.codraw.domain.usecase.SendDrawEventUseCase
import com.toan.codraw.presentation.model.DrawingToolMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Holds a Compose-friendly stroke: a pre-built Path + visual properties. */
data class StrokeUi(
    val id: String,
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

    companion object {
        private const val SIGNAL_COMPLETE_REQUEST = "COMPLETE_REQUEST"
        private const val SIGNAL_COMPLETE_RESPONSE = "COMPLETE_RESPONSE"
        private const val SIGNAL_COMPLETE_FINALIZED = "COMPLETE_FINALIZED"
        private const val SIGNAL_COMPLETE_CANCELLED = "COMPLETE_CANCELLED"
        private const val SIGNAL_UNDO = "UNDO"
    }

    // ── Player strokes ────────────────────────────────────────────────────────
    val player1Strokes = mutableStateListOf<StrokeUi>()
    val player2Strokes = mutableStateListOf<StrokeUi>()

    private val allStrokeData = mutableListOf<Stroke>()
    private val seenStrokeIds = mutableSetOf<String>()

    private val currentPoints1 = mutableListOf<Point>()
    private val currentPoints2 = mutableListOf<Point>()
    private var currentStrokeId1: String? = null
    private var currentStrokeId2: String? = null

    val currentPath1 = mutableStateOf(Path())
    val currentPath2 = mutableStateOf(Path())
    val currentPathColor1 = mutableStateOf(Color.Black)
    val currentPathColor2 = mutableStateOf(Color.Black)
    val currentPathWidth1 = mutableStateOf(5f)
    val currentPathWidth2 = mutableStateOf(5f)
    val currentPathEraser1 = mutableStateOf(false)
    val currentPathEraser2 = mutableStateOf(false)

    // ── Settings ──────────────────────────────────────────────────────────────
    val currentColorState = mutableStateOf(Color.Black)
    val currentToolState = mutableStateOf(DrawingToolMode.PEN)
    val currentWidthState = mutableStateOf(5f)
    private val opacityByTool = mutableStateMapOf(
        DrawingToolMode.PENCIL to DrawingToolMode.PENCIL.defaultOpacity,
        DrawingToolMode.PEN to DrawingToolMode.PEN.defaultOpacity,
        DrawingToolMode.MARKER to DrawingToolMode.MARKER.defaultOpacity
    )
    val currentOpacityState = mutableStateOf(DrawingToolMode.PEN.defaultOpacity)
    private var lastDrawingTool = DrawingToolMode.PEN

    var localPlayerId = 1
    private var roomCode: String = ""
    private var playerCount: Int = 1

    val isCompleting = mutableStateOf(false)
    val completionMessage = mutableStateOf<String?>(null)
    val isCompleted = mutableStateOf(false)
    val showCompletionApprovalDialog = mutableStateOf(false)
    val awaitingGuestApproval = mutableStateOf(false)

    private var strokeJob: Job? = null
    private var signalJob: Job? = null

    val palette = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green,
        Color(0xFFFF9800), Color(0xFF9C27B0), Color.Cyan, Color.Yellow
    )

    val currentColor: Color get() = currentColorState.value
    val isEraserMode: Boolean get() = currentToolState.value.isEraser
    val currentWidth: Float get() = currentWidthState.value
    val currentOpacity: Float get() = currentOpacityState.value
    val currentTool: DrawingToolMode get() = currentToolState.value
    val canRequestCompletion: Boolean get() = roomCode.isNotBlank() && !isCompleting.value && !isCompleted.value
    val canUndoLocalPlayer: Boolean get() = latestCommittedStrokeFor(localPlayerId) != null && !isCompleted.value

    // ── Connect to WS room ────────────────────────────────────────────────────
    fun connect(roomCode: String, playerId: Int, playerCount: Int) {
        resetCanvasState()
        this.roomCode = roomCode
        this.localPlayerId = playerId
        this.playerCount = playerCount
        val baseUrl = sessionManager.getBaseUrl()
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/')
        drawingRepository.connect("$baseUrl/ws/draw?roomCode=$roomCode")

        strokeJob?.cancel()
        strokeJob = viewModelScope.launch {
            receiveDrawEventsUseCase().collect { stroke ->
                // CLEAR signal: xoa canvas
                if (stroke.colorHex == "#CLEAR") {
                    clearLocalState()
                    return@collect
                }
                if (stroke.isPreview) {
                    updatePreviewStroke(stroke)
                } else {
                    clearPreview(stroke.playerId)
                    appendStrokeIfNeeded(stroke)
                }
            }
        }

        signalJob?.cancel()
        signalJob = viewModelScope.launch {
            drawingRepository.receiveRoomSignals().collect(::handleRoomSignal)
        }
    }

    fun disconnect() {
        strokeJob?.cancel()
        signalJob?.cancel()
        strokeJob = null
        signalJob = null
        drawingRepository.disconnect()
    }

    // ── Drawing events ────────────────────────────────────────────────────────
    fun startDrawing(x: Float, y: Float) {
        if (isCompleted.value) return
        val newId = UUID.randomUUID().toString()
        val points = mutableListOf(Point(x, y))
        val color = currentStrokeColor()
        val width = effectiveStrokeWidth()
        if (localPlayerId == 1) {
            currentStrokeId1 = newId
            currentPoints1.clear()
            currentPoints1.addAll(points)
            currentPath1.value = buildPath(points)
            currentPathColor1.value = color
            currentPathWidth1.value = width
            currentPathEraser1.value = isEraserMode
        } else {
            currentStrokeId2 = newId
            currentPoints2.clear()
            currentPoints2.addAll(points)
            currentPath2.value = buildPath(points)
            currentPathColor2.value = color
            currentPathWidth2.value = width
            currentPathEraser2.value = isEraserMode
        }
        sendPreviewStroke()
    }

    fun undoLastStroke() {
        if (isCompleted.value) return
        val stroke = latestCommittedStrokeFor(localPlayerId) ?: return
        if (roomCode.isBlank()) {
            removeStrokeById(stroke.id, localPlayerId)
            return
        }
        drawingRepository.sendRoomSignal(
            RoomSignal(
                type = SIGNAL_UNDO,
                playerId = localPlayerId,
                message = stroke.id
            )
        )
    }

    fun updateDrawing(x: Float, y: Float) {
        if (isCompleted.value) return
        if (localPlayerId == 1) {
            currentPoints1.add(Point(x, y))
            currentPath1.value = buildPath(currentPoints1)
        } else {
            currentPoints2.add(Point(x, y))
            currentPath2.value = buildPath(currentPoints2)
        }
        sendPreviewStroke()
    }

    fun finishDrawing() {
        if (isCompleted.value) return
        val stroke = createCommittedStroke() ?: return
        appendStrokeIfNeeded(stroke)
        clearPreview(localPlayerId)
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

    fun onCompleteClicked() {
        if (roomCode.isBlank()) {
            completionMessage.value = "Complete is only available for online rooms."
            return
        }
        if (allStrokeData.isEmpty()) {
            completionMessage.value = "Nothing to save yet."
            return
        }
        if (playerCount < 2) {
            finalizeDrawing(notifyPeer = false)
            return
        }
        if (localPlayerId != 1) {
            completionMessage.value = "Only the host can request completion."
            return
        }
        awaitingGuestApproval.value = true
        completionMessage.value = "Waiting for Player 2 to approve completion..."
        drawingRepository.sendRoomSignal(
            RoomSignal(
                type = SIGNAL_COMPLETE_REQUEST,
                playerId = localPlayerId,
                message = "Player 1 wants to complete the drawing."
            )
        )
    }

    fun respondToCompletionRequest(approved: Boolean) {
        showCompletionApprovalDialog.value = false
        completionMessage.value = if (approved) {
            "Approval sent. Waiting for completion..."
        } else {
            "You declined the completion request."
        }
        drawingRepository.sendRoomSignal(
            RoomSignal(
                type = SIGNAL_COMPLETE_RESPONSE,
                playerId = localPlayerId,
                approved = approved,
                message = if (approved) "Player 2 approved completion." else "Player 2 declined completion."
            )
        )
    }

    fun setColor(color: Color) {
        if (isCompleted.value) return
        currentColorState.value = color
        if (currentToolState.value.isEraser) {
            currentToolState.value = lastDrawingTool
            currentOpacityState.value = opacityForTool(lastDrawingTool)
        }
    }

    fun selectTool(tool: DrawingToolMode) {
        if (isCompleted.value) return
        currentToolState.value = tool
        if (!tool.isEraser) {
            lastDrawingTool = tool
            currentOpacityState.value = opacityForTool(tool)
        }
    }

    fun setOpacity(opacity: Float) {
        if (isCompleted.value || isEraserMode) return
        val clamped = opacity.coerceIn(0.1f, 1f)
        currentOpacityState.value = clamped
        opacityByTool[currentTool] = clamped
    }

    fun setStrokeWidth(width: Float) {
        if (isCompleted.value) return
        currentWidthState.value = width
    }

    fun dismissCompletionMessage() {
        completionMessage.value = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun sendPreviewStroke() {
        if (roomCode.isBlank()) return
        val points = if (localPlayerId == 1) currentPoints1.toList() else currentPoints2.toList()
        if (points.isEmpty()) return
        val strokeId = if (localPlayerId == 1) currentStrokeId1 else currentStrokeId2
        drawingRepository.sendStroke(
            Stroke(
                id = strokeId ?: UUID.randomUUID().toString(),
                points = points,
                colorHex = colorToHex(currentStrokeColor()),
                strokeWidth = effectiveStrokeWidth(),
                isEraser = isEraserMode,
                isPreview = true,
                playerId = localPlayerId
            )
        )
    }

    private fun createCommittedStroke(): Stroke? {
        val points = if (localPlayerId == 1) currentPoints1.toList() else currentPoints2.toList()
        if (points.isEmpty()) return null
        val strokeId = if (localPlayerId == 1) {
            currentStrokeId1 ?: UUID.randomUUID().toString()
        } else {
            currentStrokeId2 ?: UUID.randomUUID().toString()
        }
        return Stroke(
            id = strokeId,
            points = points,
            colorHex = colorToHex(currentStrokeColor()),
            strokeWidth = effectiveStrokeWidth(),
            isEraser = isEraserMode,
            isPreview = false,
            playerId = localPlayerId
        )
    }

    private fun handleRoomSignal(signal: RoomSignal) {
        when (signal.type) {
            SIGNAL_UNDO -> {
                signal.message?.takeIf { it.isNotBlank() }?.let { strokeId ->
                    removeStrokeById(strokeId, signal.playerId)
                }
            }
            SIGNAL_COMPLETE_REQUEST -> {
                if (playerCount >= 2 && localPlayerId != signal.playerId) {
                    showCompletionApprovalDialog.value = true
                    completionMessage.value = signal.message ?: "Player 1 wants to complete the drawing."
                }
            }
            SIGNAL_COMPLETE_RESPONSE -> {
                if (localPlayerId == 1) {
                    awaitingGuestApproval.value = false
                    if (signal.approved == true) {
                        finalizeDrawing(notifyPeer = true)
                    } else {
                        completionMessage.value = signal.message ?: "Player 2 declined completion."
                        drawingRepository.sendRoomSignal(
                            RoomSignal(
                                type = SIGNAL_COMPLETE_CANCELLED,
                                playerId = localPlayerId,
                                message = "Completion was cancelled."
                            )
                        )
                    }
                }
            }
            SIGNAL_COMPLETE_FINALIZED -> {
                awaitingGuestApproval.value = false
                showCompletionApprovalDialog.value = false
                isCompleted.value = true
                completionMessage.value = signal.message ?: "Drawing completed successfully."
            }
            SIGNAL_COMPLETE_CANCELLED -> {
                awaitingGuestApproval.value = false
                showCompletionApprovalDialog.value = false
                completionMessage.value = signal.message ?: "Completion was cancelled."
            }
        }
    }

    private fun finalizeDrawing(notifyPeer: Boolean) {
        viewModelScope.launch {
            isCompleting.value = true
            completionMessage.value = null
            drawingRepository.completeDrawing(roomCode, allStrokeData.toList()).fold(
                onSuccess = {
                    isCompleted.value = true
                    completionMessage.value = "Saved ${it.strokeCount} strokes for room ${it.roomCode}."
                    if (notifyPeer) {
                        drawingRepository.sendRoomSignal(
                            RoomSignal(
                                type = SIGNAL_COMPLETE_FINALIZED,
                                playerId = localPlayerId,
                                message = "Both players agreed. The drawing has been completed."
                            )
                        )
                    }
                },
                onFailure = {
                    completionMessage.value = it.message ?: "Failed to complete drawing."
                    if (notifyPeer) {
                        drawingRepository.sendRoomSignal(
                            RoomSignal(
                                type = SIGNAL_COMPLETE_CANCELLED,
                                playerId = localPlayerId,
                                message = "Completion failed. Please try again."
                            )
                        )
                    }
                }
            )
            isCompleting.value = false
            awaitingGuestApproval.value = false
        }
    }

    private fun updatePreviewStroke(stroke: Stroke) {
        val path = buildPath(stroke.points)
        val color = parseColor(stroke.colorHex)
        if (stroke.playerId == 1) {
            currentPath1.value = path
            currentPathColor1.value = color
            currentPathWidth1.value = stroke.strokeWidth
            currentPathEraser1.value = stroke.isEraser
        } else {
            currentPath2.value = path
            currentPathColor2.value = color
            currentPathWidth2.value = stroke.strokeWidth
            currentPathEraser2.value = stroke.isEraser
        }
    }

    private fun appendStrokeIfNeeded(stroke: Stroke) {
        if (!seenStrokeIds.add(stroke.id)) return
        allStrokeData.add(stroke.copy(isPreview = false))
        val ui = StrokeUi(
            id = stroke.id,
            path = buildPath(stroke.points),
            color = parseColor(stroke.colorHex),
            strokeWidth = stroke.strokeWidth,
            isEraser = stroke.isEraser
        )
        if (stroke.playerId == 1) player1Strokes.add(ui) else player2Strokes.add(ui)
    }

    private fun clearPreview(playerId: Int) {
        if (playerId == 1) {
            currentStrokeId1 = null
            currentPoints1.clear()
            currentPath1.value = Path()
        } else {
            currentStrokeId2 = null
            currentPoints2.clear()
            currentPath2.value = Path()
        }
    }

    private fun resetCanvasState() {
        clearLocalState()
        roomCode = ""
        playerCount = 1
        isCompleted.value = false
        completionMessage.value = null
        awaitingGuestApproval.value = false
        showCompletionApprovalDialog.value = false
        currentColorState.value = Color.Black
        currentWidthState.value = 5f
        currentToolState.value = DrawingToolMode.PEN
        opacityByTool[DrawingToolMode.PENCIL] = DrawingToolMode.PENCIL.defaultOpacity
        opacityByTool[DrawingToolMode.PEN] = DrawingToolMode.PEN.defaultOpacity
        opacityByTool[DrawingToolMode.MARKER] = DrawingToolMode.MARKER.defaultOpacity
        currentOpacityState.value = DrawingToolMode.PEN.defaultOpacity
        lastDrawingTool = DrawingToolMode.PEN
    }

    private fun clearLocalState() {
        player1Strokes.clear()
        player2Strokes.clear()
        allStrokeData.clear()
        seenStrokeIds.clear()
        currentStrokeId1 = null
        currentStrokeId2 = null
        currentPoints1.clear()
        currentPoints2.clear()
        currentPath1.value = Path()
        currentPath2.value = Path()
    }

    private fun buildPath(points: List<Point>) = Path().apply {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }

    private fun currentStrokeColor(): Color =
        if (isEraserMode) Color.Transparent else currentColor.copy(alpha = opacityForTool(currentTool))

    private fun opacityForTool(tool: DrawingToolMode): Float =
        if (tool.isEraser) 1f else opacityByTool[tool] ?: tool.defaultOpacity

    private fun effectiveStrokeWidth(): Float =
        if (isEraserMode) (currentWidth * 2.25f).coerceIn(12f, 48f) else currentWidth

    private fun colorToHex(color: Color): String = "#%08X".format(color.toArgb())

    private fun latestCommittedStrokeFor(playerId: Int): Stroke? =
        allStrokeData.lastOrNull { !it.isPreview && it.playerId == playerId }

    private fun removeStrokeById(strokeId: String, playerId: Int) {
        val removed = allStrokeData.removeAll { it.id == strokeId && it.playerId == playerId }
        if (!removed) return
        seenStrokeIds.remove(strokeId)
        if (playerId == 1) {
            player1Strokes.removeAll { it.id == strokeId }
        } else {
            player2Strokes.removeAll { it.id == strokeId }
        }
    }

    private fun parseColor(hex: String): Color {
        if (hex == "#CLEAR") return Color.White
        return try {
            Color(AndroidColor.parseColor(hex))
        } catch (_: Exception) {
            Color.Black
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}