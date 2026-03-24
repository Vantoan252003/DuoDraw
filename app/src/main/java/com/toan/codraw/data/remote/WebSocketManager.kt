package com.toan.codraw.data.remote

import android.util.Log
import com.toan.codraw.domain.model.RoomSignal
import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.ConnectionState
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * WebSocket manager with automatic reconnection using exponential backoff.
 *
 * Reconnection strategy:
 * - Initial delay: 1 second
 * - Multiplier: 2x per attempt
 * - Maximum delay: 30 seconds
 * - Maximum retry attempts: 10
 *
 * Reconnection is only triggered for unexpected disconnections (not manual disconnect).
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val listener: DrawingWebSocketListener
) {
    companion object {
        private const val TAG = "CoDraw-WS"
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RETRIES = 10
    }

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var isManualDisconnect = false
    private var retryCount = 0

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var reconnectTask: ScheduledFuture<*>? = null

    init {
        // Observe connection state to trigger auto-reconnect
        listener.onDisconnect = { code, reason ->
            if (!isManualDisconnect && currentUrl != null) {
                Log.w(TAG, "Unexpected disconnect (code=$code, reason=$reason). Scheduling reconnect...")
                scheduleReconnect()
            }
        }
        listener.onFailure = { throwable ->
            if (!isManualDisconnect && currentUrl != null) {
                Log.e(TAG, "Connection failure: ${throwable.message}. Scheduling reconnect...")
                scheduleReconnect()
            }
        }
    }

    fun connect(url: String) {
        isManualDisconnect = false
        retryCount = 0
        currentUrl = url
        cancelReconnect()
        doConnect(url)
    }

    fun disconnect() {
        isManualDisconnect = true
        cancelReconnect()
        retryCount = 0
        currentUrl = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun send(json: String) {
        webSocket?.send(json)
    }

    fun receiveStrokes(): Flow<Stroke> = listener.strokeFlow

    fun receiveSignals(): Flow<RoomSignal> = listener.signalFlow

    fun connectionState(): Flow<ConnectionState> = listener.connectionStateFlow

    // ── Auto-reconnect with exponential backoff ──────────────────────────────

    private fun doConnect(url: String) {
        listener.connectionStateFlow.value = ConnectionState.CONNECTING
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private fun scheduleReconnect() {
        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "Max reconnect attempts ($MAX_RETRIES) reached. Giving up.")
            listener.connectionStateFlow.value = ConnectionState.ERROR
            return
        }

        val delayMs = calculateBackoff(retryCount)
        retryCount++

        Log.i(TAG, "Reconnect attempt $retryCount/$MAX_RETRIES in ${delayMs}ms")
        listener.connectionStateFlow.value = ConnectionState.CONNECTING

        reconnectTask = scheduler.schedule({
            currentUrl?.let { url ->
                Log.i(TAG, "Executing reconnect attempt $retryCount to $url")
                doConnect(url)
            }
        }, delayMs, TimeUnit.MILLISECONDS)
    }

    private fun cancelReconnect() {
        reconnectTask?.cancel(false)
        reconnectTask = null
    }

    /**
     * Exponential backoff: 1s, 2s, 4s, 8s, 16s, 30s, 30s, ...
     */
    private fun calculateBackoff(attempt: Int): Long {
        val exponentialDelay = INITIAL_BACKOFF_MS * 2.0.pow(attempt).toLong()
        return min(exponentialDelay, MAX_BACKOFF_MS)
    }
}
