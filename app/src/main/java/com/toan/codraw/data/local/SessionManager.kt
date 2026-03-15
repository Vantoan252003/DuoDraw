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

    fun saveDisplayName(displayName: String) = prefs.edit().putString(KEY_DISPLAY_NAME, displayName).apply()
    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, getUsername())

    fun saveAvatarUrl(avatarUrl: String?) = prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply()
    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun saveLanguageTag(languageTag: String) = prefs.edit().putString(KEY_LANGUAGE_TAG, languageTag).apply()
    fun getLanguageTag(): String = prefs.getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG) ?: DEFAULT_LANGUAGE_TAG

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
        val defaultIps = listOf("192.168.31.154", "192.168.31.154")
        return defaultIps.none { url.contains(it) }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() {
        val baseUrl = getBaseUrl()
        val languageTag = getLanguageTag()
        prefs.edit().clear().apply()
        saveBaseUrl(baseUrl)
        saveLanguageTag(languageTag)
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_LANGUAGE_TAG = "language_tag"
        private const val KEY_BASE_URL = "base_url"

        private const val DEFAULT_LANGUAGE_TAG = "vi"
        const val DEFAULT_BASE_URL = "http://192.168.31.154:8080/"
    }
}
