package com.toan.codraw.domain.model

data class CompletedDrawing(
    val id: Long,
    val roomCode: String,
    val hostUsername: String,
    val guestUsername: String?,
    val roomType: String,
    val savedByUsername: String,
    val strokeCount: Int,
    val completedAt: String,
    val strokes: List<Stroke> = emptyList()
)
