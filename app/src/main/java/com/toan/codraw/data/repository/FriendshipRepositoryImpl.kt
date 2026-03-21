package com.toan.codraw.data.repository

import com.toan.codraw.data.remote.FriendshipApi
import com.toan.codraw.data.remote.dto.FriendshipDto
import com.toan.codraw.data.remote.dto.FriendChatDto
import com.toan.codraw.data.remote.dto.ProfileResponseDto
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.repository.FriendshipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FriendshipRepositoryImpl @Inject constructor(
    private val api: FriendshipApi,
    private val sessionManager: SessionManager
) : FriendshipRepository {

    private fun getToken(): String {
        val token = sessionManager.getToken() ?: throw IllegalStateException("Not logged in")
        return "Bearer $token"
    }

    override suspend fun sendRequest(targetUsername: String): Result<FriendshipDto> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.sendRequest(getToken(), targetUsername))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun respondToRequest(id: Long, accept: Boolean): Result<FriendshipDto> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.respondToRequest(getToken(), id, accept))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getFriends(): Result<List<FriendChatDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getFriends(getToken()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPendingRequests(): Result<List<FriendshipDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getPendingRequests(getToken()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSentRequests(): Result<List<FriendshipDto>> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(api.getSentRequests(getToken()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
