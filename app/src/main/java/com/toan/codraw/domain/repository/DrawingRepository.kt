package com.toan.codraw.domain.repository

import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.model.Stroke
import kotlinx.coroutines.flow.Flow

interface DrawingRepository {
    fun connect(url: String)
    fun disconnect()
    fun sendStroke(stroke: Stroke)
    fun receiveStrokes(): Flow<Stroke>
    fun connectionState(): Flow<ConnectionState>
    suspend fun completeDrawing(roomCode: String, strokes: List<Stroke>): Result<CompletedDrawing>
}

enum class ConnectionState {
    IDLE, CONNECTING, CONNECTED, DISCONNECTED, ERROR
}
