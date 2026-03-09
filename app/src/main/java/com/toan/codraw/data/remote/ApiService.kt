package com.toan.codraw.data.remote

import com.toan.codraw.data.remote.dto.AuthResponse
import com.toan.codraw.data.remote.dto.CompleteDrawingRequest
import com.toan.codraw.data.remote.dto.CompletedDrawingResponse
import com.toan.codraw.data.remote.dto.CreateRoomRequest
import com.toan.codraw.data.remote.dto.LoginRequest
import com.toan.codraw.data.remote.dto.RegisterRequest
import com.toan.codraw.data.remote.dto.RoomResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/rooms/create")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): Response<RoomResponse>

    @GET("api/rooms/public")
    suspend fun getPublicRooms(
        @Header("Authorization") token: String
    ): Response<List<RoomResponse>>

    @POST("api/rooms/join")
    suspend fun joinRoom(
        @Header("Authorization") token: String,
        @Query("code") code: String
    ): Response<RoomResponse>

    @GET("api/rooms/{code}")
    suspend fun getRoom(
        @Header("Authorization") token: String,
        @Path("code") code: String
    ): Response<RoomResponse>

    @POST("api/drawings/complete")
    suspend fun completeDrawing(
        @Header("Authorization") token: String,
        @Body request: CompleteDrawingRequest
    ): Response<CompletedDrawingResponse>
}
