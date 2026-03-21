package com.toan.codraw.data.repository

import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.dto.ProfileResponse
import com.toan.codraw.data.remote.dto.UpdatePasswordRequest
import com.toan.codraw.data.remote.dto.UpdateProfileRequest
import com.toan.codraw.domain.model.UserProfile
import com.toan.codraw.domain.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : ProfileRepository {

    private fun bearerToken() = "Bearer ${sessionManager.getToken() ?: ""}"

    override suspend fun getMyProfile(): Result<UserProfile> = runCatching {
        val response = apiService.getMyProfile(bearerToken())
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun updateProfile(displayName: String): Result<UserProfile> = runCatching {
        val response = apiService.updateProfile(
            bearerToken(),
            UpdateProfileRequest(displayName = displayName)
        )
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun updatePassword(oldPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = apiService.updatePassword(
            bearerToken(),
            UpdatePasswordRequest(oldPassword = oldPassword, newPassword = newPassword)
        )
        if (response.isSuccessful) {
            Unit
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun uploadAvatar(fileName: String, bytes: ByteArray, mimeType: String): Result<UserProfile> = runCatching {
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
        val response = apiService.uploadAvatar(bearerToken(), part)
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    override suspend fun getProfileByUsername(username: String): Result<UserProfile> = runCatching {
        val response = apiService.getProfileByUsername(bearerToken(), username)
        if (response.isSuccessful) {
            response.body()!!.toDomain()
        } else {
            throw Exception(response.errorBody()?.string() ?: "Error ${response.code()}")
        }
    }

    private fun ProfileResponse.toDomain() = UserProfile(
        username = username,
        email = email,
        displayName = displayName,
        avatarUrl = avatarUrl
    )
}

