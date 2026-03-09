package com.toan.codraw.domain.model

data class CompletedDrawing(
    val id: Long,
    val roomCode: String,
    val savedByUsername: String,
    val strokeCount: Int,
    val completedAt: String
)

