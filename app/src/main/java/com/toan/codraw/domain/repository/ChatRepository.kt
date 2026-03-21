package com.toan.codraw.domain.repository

import com.toan.codraw.data.remote.dto.ChatMessageResponseDto

interface ChatRepository {
    suspend fun getChatHistory(friendUsername: String): Result<List<ChatMessageResponseDto>>
    suspend fun sendMessage(receiverUsername: String, content: String): Result<ChatMessageResponseDto>
    suspend fun sendVoiceMessage(receiverUsername: String, fileName: String, bytes: ByteArray, mimeType: String): Result<ChatMessageResponseDto>
    suspend fun markAsRead(senderUsername: String): Result<Unit>
}
