package com.toan.codraw.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
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
}
