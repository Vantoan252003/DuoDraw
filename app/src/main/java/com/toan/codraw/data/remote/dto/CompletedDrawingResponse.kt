package com.toan.codraw.data.remote.dto

data class CompletedDrawingResponse(
    val id: Long,
    val roomCode: String,
    val hostUsername: String,
    val guestUsername: String?,
    val roomType: String,
    val savedByUsername: String,
    val strokeCount: Int,
    val completedAt: String,
    val strokes: List<StrokeDto>
)

