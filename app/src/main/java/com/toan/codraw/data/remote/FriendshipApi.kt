package com.toan.codraw.data.remote

import com.toan.codraw.data.remote.dto.FriendChatDto
import com.toan.codraw.data.remote.dto.ProfileResponseDto
import com.toan.codraw.data.remote.dto.FriendshipDto
import retrofit2.http.Header
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendshipApi {
    @POST("api/friends/request")
    suspend fun sendRequest(
        @Header("Authorization") token: String,
        @Query("targetUsername") targetUsername: String
    ): FriendshipDto

    @POST("api/friends/respond/{id}")
    suspend fun respondToRequest(
        @Header("Authorization") token: String,
        @Path("id") id: Long, 
        @Query("accept") accept: Boolean
    ): FriendshipDto

    @GET("api/friends")
    suspend fun getFriends(
        @Header("Authorization") token: String
    ): List<FriendChatDto>

    @GET("api/friends/pending")
    suspend fun getPendingRequests(
        @Header("Authorization") token: String
    ): List<FriendshipDto>

    @GET("api/friends/sent")
    suspend fun getSentRequests(
        @Header("Authorization") token: String
    ): List<FriendshipDto>
}
