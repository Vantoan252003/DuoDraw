package com.toan.codraw.data.remote

import com.toan.codraw.data.remote.dto.AuthResponse
import com.toan.codraw.data.remote.dto.CompleteDrawingRequest
import com.toan.codraw.data.remote.dto.CompletedDrawingResponse
import com.toan.codraw.data.remote.dto.CreateRoomRequest
import com.toan.codraw.data.remote.dto.LoginRequest
import com.toan.codraw.data.remote.dto.ProfileResponse
import com.toan.codraw.data.remote.dto.RegisterRequest
import com.toan.codraw.data.remote.dto.RoomResponse
import com.toan.codraw.data.remote.dto.UpdatePasswordRequest
import com.toan.codraw.data.remote.dto.UpdateProfileRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    @GET("api/drawings/mine")
    suspend fun getMyCompletedDrawings(
        @Header("Authorization") token: String
    ): Response<List<CompletedDrawingResponse>>

    @GET("api/drawings/{roomCode}")
    suspend fun getCompletedDrawing(
        @Header("Authorization") token: String,
        @Path("roomCode") roomCode: String
    ): Response<CompletedDrawingResponse>

    @POST("api/drawings/complete")
    suspend fun completeDrawing(
        @Header("Authorization") token: String,
        @Body request: CompleteDrawingRequest
    ): Response<CompletedDrawingResponse>

    @GET("api/profile/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    @PUT("api/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<ProfileResponse>

    @PUT("api/profile/password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("api/profile/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<ProfileResponse>

    @GET("api/profile/{username}")
    suspend fun getProfileByUsername(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<ProfileResponse>

    @Multipart
    @POST("api/chat/voice/{receiverUsername}")
    suspend fun uploadVoiceMessage(
        @Header("Authorization") token: String,
        @Path("receiverUsername") receiverUsername: String,
        @Part audio: MultipartBody.Part
    ): Response<com.toan.codraw.data.remote.dto.ChatMessageResponseDto>
}
