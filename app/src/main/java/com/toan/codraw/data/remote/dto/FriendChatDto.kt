package com.toan.codraw.data.remote.dto

data class FriendChatDto(
    val friend: ProfileResponseDto,
    val lastMessage: ChatMessageResponseDto?,
    val unreadCount: Int
)
