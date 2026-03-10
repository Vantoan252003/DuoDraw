package com.toan.codraw.data.remote

import com.toan.codraw.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val baseUrl = sessionManager.getBaseUrl().toHttpUrlOrNull()

        if (baseUrl == null) {
            return chain.proceed(originalRequest)
        }

        val newUrl = originalUrl.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        return chain.proceed(originalRequest.newBuilder().url(newUrl).build())
    }
}

