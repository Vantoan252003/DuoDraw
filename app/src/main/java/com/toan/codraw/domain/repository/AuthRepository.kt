package com.toan.codraw.domain.repository

data class AuthResult(
    val token: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val message: String
)

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthResult>
    suspend fun register(username: String, email: String, password: String): Result<AuthResult>
}
