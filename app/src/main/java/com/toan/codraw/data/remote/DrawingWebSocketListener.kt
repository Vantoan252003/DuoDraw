package com.toan.codraw.data.remote

import android.util.Log
import com.google.gson.Gson
import com.toan.codraw.data.remote.dto.StrokeDto
import com.toan.codraw.domain.model.Point
import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.ConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class DrawingWebSocketListener(
    private val gson: Gson,
    val strokeFlow: MutableSharedFlow<Stroke> = MutableSharedFlow(extraBufferCapacity = 64),
    val connectionStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.IDLE)
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connectionStateFlow.value = ConnectionState.CONNECTED
        Log.d("CoDraw-WS", "WebSocket opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val dto = gson.fromJson(text, StrokeDto::class.java)
            val stroke = Stroke(
                id = dto.id,
                points = dto.points.map { Point(it.x, it.y) },
                colorHex = if (dto.type == "CLEAR") "#CLEAR" else dto.colorHex,
                strokeWidth = dto.strokeWidth,
                isEraser = dto.isEraser,
                playerId = dto.playerId
            )
            runBlocking { strokeFlow.emit(stroke) }
        } catch (e: Exception) {
            Log.e("CoDraw-WS", "Failed to parse message: $text", e)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        connectionStateFlow.value = ConnectionState.DISCONNECTED
        webSocket.close(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        connectionStateFlow.value = ConnectionState.ERROR
        Log.e("CoDraw-WS", "WebSocket error", t)
    }
}

