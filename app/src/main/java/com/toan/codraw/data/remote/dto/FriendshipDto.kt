package com.toan.codraw.data.remote.dto

data class FriendshipDto(
    val id: Long,
    val requester: ProfileResponseDto,
    val receiver: ProfileResponseDto,
    val status: String, // PENDING, ACCEPTED, REJECTED
    val createdAt: String
)
