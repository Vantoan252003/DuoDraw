package com.toan.codraw.data.remote.dto

data class CompleteDrawingRequest(
    val roomCode: String,
    val strokes: List<StrokeDto>
)

