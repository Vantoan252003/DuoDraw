package com.toan.codraw.data.remote.dto

data class ProfileResponse(
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null
)

data class UpdateProfileRequest(
    val displayName: String
)

