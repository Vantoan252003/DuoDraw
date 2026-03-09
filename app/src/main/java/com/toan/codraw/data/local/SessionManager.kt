package com.toan.codraw.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("codraw_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUsername(username: String) = prefs.edit().putString(KEY_USERNAME, username).apply()
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun saveBaseUrl(url: String) = prefs.edit().putString(KEY_BASE_URL, url).apply()
    fun getBaseUrl(): String {
        val saved = prefs.getString(KEY_BASE_URL, null)
        // Nếu URL đã lưu khác với DEFAULT_BASE_URL (tức là đang dùng IP cũ), reset về mặc định mới
        return if (saved != null && saved != DEFAULT_BASE_URL && !isCustomUrl(saved)) {
            saveBaseUrl(DEFAULT_BASE_URL)
            DEFAULT_BASE_URL
        } else {
            saved ?: DEFAULT_BASE_URL
        }
    }

    /** Trả về true nếu URL là URL tuỳ chỉnh do người dùng tự nhập (không phải IP mặc định cũ) */
    private fun isCustomUrl(url: String): Boolean {
        val defaultIps = listOf("192.168.0.105", "192.168.31.206")
        return defaultIps.none { url.contains(it) }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_BASE_URL = "base_url"

        const val DEFAULT_BASE_URL = "http://192.168.0.105:8080/"
    }
}

