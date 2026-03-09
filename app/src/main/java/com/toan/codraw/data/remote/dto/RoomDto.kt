package com.toan.codraw.data.remote.dto

data class RoomResponse(
    val id: Long,
    val roomCode: String,
    val hostUsername: String,
    val guestUsername: String?,
    val status: String,
    val roomType: String,
    val playerCount: Int
)
