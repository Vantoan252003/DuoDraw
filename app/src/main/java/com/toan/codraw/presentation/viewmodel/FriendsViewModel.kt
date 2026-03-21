package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.remote.GlobalWebSocketManager
import com.toan.codraw.data.remote.dto.FriendChatDto
import com.toan.codraw.data.remote.dto.FriendshipDto
import com.toan.codraw.data.remote.dto.ProfileResponseDto
import com.toan.codraw.presentation.util.UiText
import com.toan.codraw.domain.repository.FriendshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toan.codraw.domain.model.UserProfile
import com.toan.codraw.domain.repository.ProfileRepository

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendshipRepository: FriendshipRepository,
    private val profileRepository: ProfileRepository,
    private val globalWebSocketManager: GlobalWebSocketManager
) : ViewModel() {

    val friendsList = mutableStateListOf<FriendChatDto>()
    val pendingRequests = mutableStateListOf<FriendshipDto>()
    val sentRequests = mutableStateListOf<FriendshipDto>()

    var searchedProfile by mutableStateOf<UserProfile?>(null)
        private set
    var isSearching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<UiText?>(null)
        private set

    init {
        loadFriendsData()
        listenForFriendEvents()
    }

    fun loadFriendsData() {
        viewModelScope.launch {
            friendshipRepository.getFriends().onSuccess {
                friendsList.clear()
                friendsList.addAll(it)
            }
            friendshipRepository.getPendingRequests().onSuccess {
                pendingRequests.clear()
                pendingRequests.addAll(it)
            }
            friendshipRepository.getSentRequests().onSuccess {
                sentRequests.clear()
                sentRequests.addAll(it)
            }
        }
    }

    private fun listenForFriendEvents() {
        viewModelScope.launch {
            globalWebSocketManager.listener.friendshipEvents.collectLatest { 
                loadFriendsData()
            }
        }
    }

    fun sendFriendRequest(username: String, onSuccess: () -> Unit = {}, onError: (UiText) -> Unit = {}) {
        viewModelScope.launch {
            friendshipRepository.sendRequest(username).onSuccess {
                onSuccess()
            }.onFailure {
                onError(mapErrorToUiText(it))
            }
        }
    }

    fun searchProfile(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            searchError = null
            searchedProfile = null
            profileRepository.getProfileByUsername(username.trim())
                .onSuccess { profile ->
                    searchedProfile = profile
                }
                .onFailure {
                    searchError = mapErrorToUiText(it)
                }
            isSearching = false
        }
    }

    fun clearSearch() {
        searchedProfile = null
        searchError = null
    }

    private fun mapErrorToUiText(error: Throwable): UiText {
        if (error is retrofit2.HttpException) {
            val errorBodyStr = try {
                error.response()?.errorBody()?.string() ?: ""
            } catch (e: Exception) { "" }
            
            return when {
                errorBodyStr.contains("User not found") -> UiText.StringResource(com.toan.codraw.R.string.error_user_not_found)
                errorBodyStr.contains("Cannot send friend request to yourself") -> UiText.StringResource(com.toan.codraw.R.string.error_cannot_add_self)
                errorBodyStr.contains("Friendship or request already exists") -> UiText.StringResource(com.toan.codraw.R.string.error_request_exists)
                else -> UiText.DynamicString(errorBodyStr.ifBlank { error.message() })
            }
        }
        return UiText.DynamicString(error.message ?: "Unknown error")
    }

    fun respondToRequest(id: Long, accept: Boolean) {
        viewModelScope.launch {
            friendshipRepository.respondToRequest(id, accept).onSuccess {
                loadFriendsData()
            }
        }
    }
}
