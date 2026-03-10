package com.toan.codraw.data.repository

import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.dto.LoginRequest
import com.toan.codraw.data.remote.dto.RegisterRequest
import com.toan.codraw.domain.repository.AuthRepository
import com.toan.codraw.domain.repository.AuthResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthResult> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    AuthResult(
                        token = body.token,
                        username = body.username,
                        email = body.email,
                        displayName = body.displayName,
                        avatarUrl = body.avatarUrl,
                        message = body.message
                    )
                )
            } else {
                val errorMsg = response.errorBody()?.string()
                    ?.let { parseErrorMessage(it) } ?: "Đăng nhập thất bại (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không kết nối được server: ${e.message}"))
        }
    }

    override suspend fun register(username: String, email: String, password: String): Result<AuthResult> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    AuthResult(
                        token = body.token,
                        username = body.username,
                        email = body.email,
                        displayName = body.displayName,
                        avatarUrl = body.avatarUrl,
                        message = body.message
                    )
                )
            } else {
                val errorMsg = response.errorBody()?.string()
                    ?.let { parseErrorMessage(it) } ?: "Đăng ký thất bại (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không kết nối được server: ${e.message}"))
        }
    }

    private fun parseErrorMessage(json: String): String {
        return try {
            // {"message":"..."} → extract value
            val start = json.indexOf("\":\"") + 3
            val end = json.indexOf("\"", start)
            if (start > 2 && end > start) json.substring(start, end) else json
        } catch (e: Exception) {
            json
        }
    }
}
