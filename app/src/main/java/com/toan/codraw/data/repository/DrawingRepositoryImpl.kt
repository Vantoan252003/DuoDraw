package com.toan.codraw.data.repository

import com.google.gson.Gson
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.WebSocketManager
import com.toan.codraw.data.remote.dto.CompleteDrawingRequest
import com.toan.codraw.data.remote.dto.PointDto
import com.toan.codraw.data.remote.dto.StrokeDto
import com.toan.codraw.domain.model.CompletedDrawing
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
        val type = if (stroke.colorHex == "#CLEAR") "CLEAR" else "STROKE"
        val dto = stroke.toDto(type)
        webSocketManager.send(gson.toJson(dto))
    }

    override fun receiveStrokes(): Flow<Stroke> = webSocketManager.receiveStrokes()

    override fun connectionState(): Flow<ConnectionState> = webSocketManager.connectionState()

    override suspend fun completeDrawing(roomCode: String, strokes: List<Stroke>): Result<CompletedDrawing> = runCatching {
        val request = CompleteDrawingRequest(
            roomCode = roomCode,
            strokes = strokes.map { it.toDto(type = "STROKE") }
        )
        val response = apiService.completeDrawing(bearerToken(), request)
        if (response.isSuccessful) {
            response.body()!!.let {
                CompletedDrawing(
                    id = it.id,
                    roomCode = it.roomCode,
                    savedByUsername = it.savedByUsername,
                    strokeCount = it.strokeCount,
                    completedAt = it.completedAt
                )
            }
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
        playerId = playerId
    )
}
