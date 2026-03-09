package com.toan.codraw.data.repository

import com.google.gson.Gson
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.WebSocketManager
import com.toan.codraw.data.remote.dto.CompleteDrawingRequest
import com.toan.codraw.data.remote.dto.CompletedDrawingResponse
import com.toan.codraw.data.remote.dto.PointDto
import com.toan.codraw.data.remote.dto.RoomSignalDto
import com.toan.codraw.data.remote.dto.StrokeDto
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.model.Point
import com.toan.codraw.domain.model.RoomSignal
import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.ConnectionState
import com.toan.codraw.domain.repository.DrawingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawingRepositoryImpl @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val gson: Gson,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : DrawingRepository {

    private fun bearerToken() = "Bearer ${sessionManager.getToken() ?: ""}"

    override fun connect(url: String) {
        webSocketManager.connect(url)
    }

    override fun disconnect() {
        webSocketManager.disconnect()
    }

    override fun sendStroke(stroke: Stroke) {
        val type = when {
            stroke.colorHex == "#CLEAR" -> "CLEAR"
            stroke.isPreview -> "STROKE_PREVIEW"
            else -> "STROKE"
        }
        val dto = stroke.toDto(type)
        webSocketManager.send(gson.toJson(dto))
    }

    override fun sendRoomSignal(signal: RoomSignal) {
        webSocketManager.send(
            gson.toJson(
                RoomSignalDto(
                    type = signal.type,
                    playerId = signal.playerId,
                    approved = signal.approved,
                    message = signal.message
                )
            )
        )
    }

    override fun receiveStrokes(): Flow<Stroke> = webSocketManager.receiveStrokes()

    override fun receiveRoomSignals(): Flow<RoomSignal> = webSocketManager.receiveSignals()

    override fun connectionState(): Flow<ConnectionState> = webSocketManager.connectionState()

    override suspend fun completeDrawing(roomCode: String, strokes: List<Stroke>): Result<CompletedDrawing> = runCatching {
        val request = CompleteDrawingRequest(
            roomCode = roomCode,
            strokes = strokes.map { it.toDto(type = "STROKE") }
        )
        val response = apiService.completeDrawing(bearerToken(), request)
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun getCompletedDrawing(roomCode: String): Result<CompletedDrawing> = runCatching {
        val response = apiService.getCompletedDrawing(bearerToken(), roomCode)
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun getMyCompletedDrawings(): Result<List<CompletedDrawing>> = runCatching {
        val response = apiService.getMyCompletedDrawings(bearerToken())
        if (response.isSuccessful) {
            response.body().orEmpty().map { it.toDomain() }
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    private fun Stroke.toDto(type: String) = StrokeDto(
        type = type,
        id = id,
        points = points.map { PointDto(it.x, it.y) },
        colorHex = colorHex,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        preview = isPreview,
        playerId = playerId
    )

    private fun CompletedDrawingResponse.toDomain() = CompletedDrawing(
        id = id,
        roomCode = roomCode,
        hostUsername = hostUsername,
        guestUsername = guestUsername,
        roomType = roomType,
        savedByUsername = savedByUsername,
        strokeCount = strokeCount,
        completedAt = completedAt,
        strokes = strokes.map {
            Stroke(
                id = it.id,
                points = it.points.map { point -> Point(point.x, point.y) },
                colorHex = it.colorHex,
                strokeWidth = it.strokeWidth,
                isEraser = it.isEraser,
                isPreview = it.preview,
                playerId = it.playerId
            )
        }
    )
}
