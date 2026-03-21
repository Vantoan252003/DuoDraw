package com.toan.codraw.data.remote.dto

data class ProfileResponseDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String?
)
