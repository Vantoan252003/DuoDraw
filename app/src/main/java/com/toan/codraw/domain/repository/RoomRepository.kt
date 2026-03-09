package com.toan.codraw.domain.repository

data class RoomResult(
    val id: Long,
    val roomCode: String,
    val hostUsername: String,
    val guestUsername: String?,
    val status: String,
    val roomType: String,
    val playerCount: Int
)

interface RoomRepository {
    suspend fun createRoom(roomType: String): Result<RoomResult>
    suspend fun joinRoom(code: String): Result<RoomResult>
    suspend fun getRoom(code: String): Result<RoomResult>
    suspend fun getPublicRooms(): Result<List<RoomResult>>
}
