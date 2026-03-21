package com.toan.codraw.data.remote.dto

data class ChatMessageResponseDto(
    val id: Long,
    val senderUsername: String,
    val receiverUsername: String,
    val content: String,
    val timestamp: String
)
