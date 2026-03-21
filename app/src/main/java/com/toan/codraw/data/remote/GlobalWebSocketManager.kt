package com.toan.codraw.data.remote

import com.toan.codraw.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalWebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    val listener: GlobalWebSocketListener,
    private val sessionManager: SessionManager
) {
    private var webSocket: WebSocket? = null

    fun connect() {
        if (webSocket != null) return
        val token = sessionManager.getToken() ?: return
        val baseUrl = sessionManager.getBaseUrl()
        
        val wsBaseUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
        val url = "${wsBaseUrl}ws/global?token=$token"
        
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "User logged out or app background")
        webSocket = null
    }
}
