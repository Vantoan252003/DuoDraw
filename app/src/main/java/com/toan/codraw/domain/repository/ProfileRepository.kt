package com.toan.codraw.domain.repository

import com.toan.codraw.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getMyProfile(): Result<UserProfile>
    suspend fun updateProfile(displayName: String): Result<UserProfile>
    suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit>
    suspend fun uploadAvatar(
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<UserProfile>
    suspend fun getProfileByUsername(username: String): Result<UserProfile>
}

