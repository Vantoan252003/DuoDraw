package com.toan.codraw.domain.model

import java.util.UUID

/**
 * Pure domain model — no Compose/Android UI dependencies.
 * @param id        unique stroke ID (UUID)
 * @param points    ordered list of touch points
 * @param colorHex  ARGB hex string e.g. "#FFFF0000" for red
 * @param strokeWidth width in dp
 * @param isEraser  true when this stroke erases
 * @param playerId  1 = local player, 2 = remote player
 */
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point> = emptyList(),
    val colorHex: String = "#FF000000",
    val strokeWidth: Float = 5f,
    val isEraser: Boolean = false,
    val playerId: Int = 1
)