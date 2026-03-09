package com.toan.codraw.data.remote

import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val listener: DrawingWebSocketListener
) {
    private var webSocket: WebSocket? = null

    fun connect(url: String) {
        listener.connectionStateFlow.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun send(json: String) {
        webSocket?.send(json)
    }

    fun receiveStrokes(): Flow<Stroke> = listener.strokeFlow

    fun connectionState(): Flow<ConnectionState> = listener.connectionStateFlow
}

