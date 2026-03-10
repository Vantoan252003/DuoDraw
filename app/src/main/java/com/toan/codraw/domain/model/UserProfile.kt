package com.toan.codraw.domain.model

data class UserProfile(
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null
)

