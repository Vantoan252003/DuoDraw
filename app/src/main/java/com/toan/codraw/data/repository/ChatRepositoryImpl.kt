package com.toan.codraw.data.repository

import com.toan.codraw.data.remote.ChatApi
import com.toan.codraw.data.remote.dto.ChatMessageResponseDto
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val sessionManager: SessionManager
) : ChatRepository {

    private fun getToken(): String {
        val token = sessionManager.getToken() ?: throw IllegalStateException("Not logged in")
        return "Bearer $token"
    }

    override suspend fun getChatHistory(friendUsername: String): Result<List<ChatMessageResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getChatHistory(getToken(), friendUsername))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendMessage(receiverUsername: String, content: String): Result<ChatMessageResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.sendMessage(getToken(), receiverUsername, content))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
