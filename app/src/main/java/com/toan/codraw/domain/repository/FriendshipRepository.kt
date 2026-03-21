package com.toan.codraw.domain.repository

import com.toan.codraw.data.remote.dto.FriendshipDto
import com.toan.codraw.data.remote.dto.FriendChatDto
import com.toan.codraw.data.remote.dto.ProfileResponseDto

interface FriendshipRepository {
    suspend fun sendRequest(targetUsername: String): Result<FriendshipDto>
    suspend fun respondToRequest(id: Long, accept: Boolean): Result<FriendshipDto>
    suspend fun getFriends(): Result<List<FriendChatDto>>
    suspend fun getPendingRequests(): Result<List<FriendshipDto>>
    suspend fun getSentRequests(): Result<List<FriendshipDto>>
}
