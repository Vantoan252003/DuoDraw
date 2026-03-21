package com.toan.codraw.data.remote.dto

data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)
