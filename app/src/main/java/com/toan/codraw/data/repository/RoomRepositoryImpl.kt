package com.toan.codraw.data.repository

import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.ApiService
import com.toan.codraw.data.remote.dto.CreateRoomRequest
import com.toan.codraw.domain.repository.RoomRepository
import com.toan.codraw.domain.repository.RoomResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : RoomRepository {

    private fun bearerToken() = "Bearer ${sessionManager.getToken() ?: ""}"

    override suspend fun createRoom(roomType: String): Result<RoomResult> = runCatching {
        val r = apiService.createRoom(bearerToken(), CreateRoomRequest(roomType = roomType))
        if (r.isSuccessful) r.body()!!.toResult()
        else throw Exception(r.errorBody()?.string() ?: "Error ${r.code()}")
    }

    override suspend fun joinRoom(code: String): Result<RoomResult> = runCatching {
        val r = apiService.joinRoom(bearerToken(), code.uppercase())
        if (r.isSuccessful) r.body()!!.toResult()
        else throw Exception(r.errorBody()?.string() ?: "Error ${r.code()}")
    }

    override suspend fun getRoom(code: String): Result<RoomResult> = runCatching {
        val r = apiService.getRoom(bearerToken(), code.uppercase())
        if (r.isSuccessful) r.body()!!.toResult()
        else throw Exception(r.errorBody()?.string() ?: "Error ${r.code()}")
    }

    override suspend fun getPublicRooms(): Result<List<RoomResult>> = runCatching {
        val r = apiService.getPublicRooms(bearerToken())
        if (r.isSuccessful) r.body().orEmpty().map { it.toResult() }
        else throw Exception(r.errorBody()?.string() ?: "Error ${r.code()}")
    }

    private fun com.toan.codraw.data.remote.dto.RoomResponse.toResult() = RoomResult(
        id = id,
        roomCode = roomCode,
        hostUsername = hostUsername,
        guestUsername = guestUsername,
        status = status,
        roomType = roomType,
        playerCount = playerCount
    )
}
