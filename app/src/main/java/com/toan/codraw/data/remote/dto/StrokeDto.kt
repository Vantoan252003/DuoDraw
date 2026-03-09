package com.toan.codraw.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StrokeDto(
    val type: String = "STROKE",
    val id: String = "",
    val points: List<PointDto> = emptyList(),
    val colorHex: String = "#FF000000",
    val strokeWidth: Float = 5f,
    @SerializedName(value = "isEraser", alternate = ["eraser"])
    val isEraser: Boolean = false,
    val preview: Boolean = false,
    val playerId: Int = 1
)
