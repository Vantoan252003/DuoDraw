package com.toan.codraw.data.remote.dto

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val username: String,
    val email: String,
    val message: String
)

data class ErrorResponse(
    val message: String
)

