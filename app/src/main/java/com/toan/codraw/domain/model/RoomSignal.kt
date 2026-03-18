package com.toan.codraw.domain.model

data class RoomSignal(
    val type: String,
    val playerId: Int,
    val approved: Boolean? = null,
    val message: String? = null,
    val offsetX: Float? = null,
    val offsetY: Float? = null
)

