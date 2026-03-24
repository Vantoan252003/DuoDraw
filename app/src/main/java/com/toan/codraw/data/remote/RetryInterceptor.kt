package com.toan.codraw.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

/**
 * OkHttp interceptor that automatically retries failed HTTP requests
 * using exponential backoff.
 *
 * Retry policy:
 * - Retries on: IOException (timeout, network error) and 5xx server errors
 * - Does NOT retry on: 4xx client errors (bad request, unauthorized, etc.)
 * - Max retries: 3
 * - Backoff: 500ms → 1s → 2s
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialBackoffMs: Long = 500L,
    private val maxBackoffMs: Long = 5_000L
) : Interceptor {

    companion object {
        private const val TAG = "CoDraw-Retry"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)

                // Don't retry on success or client errors (4xx)
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }

                // Server error (5xx) — close body and retry
                if (response.code in 500..599 && attempt < maxRetries) {
                    response.close()
                    val delayMs = calculateBackoff(attempt)
                    Log.w(TAG, "Server error ${response.code} on ${request.url}. " +
                            "Retry ${attempt + 1}/$maxRetries in ${delayMs}ms")
                    Thread.sleep(delayMs)
                    continue
                }

                return response

            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = calculateBackoff(attempt)
                    Log.w(TAG, "IOException on ${request.url}: ${e.message}. " +
                            "Retry ${attempt + 1}/$maxRetries in ${delayMs}ms")
                    try {
                        Thread.sleep(delayMs)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw e
                    }
                }
            }
        }

        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }

    /**
     * Exponential backoff: 500ms, 1s, 2s, 4s, ...
     */
    private fun calculateBackoff(attempt: Int): Long {
        val delay = initialBackoffMs * 2.0.pow(attempt).toLong()
        return min(delay, maxBackoffMs)
    }
}
