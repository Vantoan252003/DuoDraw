package com.toan.codraw.data.remote.dto

data class RoomSignalDto(
    val type: String,
    val playerId: Int = 0,
    val approved: Boolean? = null,
    val message: String? = null
)

