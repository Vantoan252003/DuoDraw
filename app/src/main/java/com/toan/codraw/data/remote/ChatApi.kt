package com.toan.codraw.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Header
import com.toan.codraw.data.remote.dto.ChatMessageResponseDto

interface ChatApi {
    @GET("api/chat/{friendUsername}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("friendUsername") friendUsername: String
    ): List<ChatMessageResponseDto>

    @POST("api/chat/{receiverUsername}")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("receiverUsername") receiverUsername: String, 
        @retrofit2.http.Body content: String
    ): ChatMessageResponseDto

    @retrofit2.http.Multipart
    @POST("api/chat/voice/{receiverUsername}")
    suspend fun uploadVoiceMessage(
        @Header("Authorization") token: String,
        @Path("receiverUsername") receiverUsername: String,
        @retrofit2.http.Part audio: okhttp3.MultipartBody.Part
    ): ChatMessageResponseDto

    @PUT("api/chat/read/{senderUsername}")
    suspend fun markAsRead(
        @Header("Authorization") token: String,
        @Path("senderUsername") senderUsername: String
    )
}
