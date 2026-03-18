package com.toan.codraw.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.model.UserProfile
import com.toan.codraw.domain.repository.DrawingRepository
import com.toan.codraw.domain.repository.ProfileRepository
import com.toan.codraw.domain.repository.RoomRepository
import com.toan.codraw.domain.repository.RoomResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PublicRoomJoinState {
    data object Idle : PublicRoomJoinState
    data class Loading(val roomCode: String) : PublicRoomJoinState
    data class Success(val room: RoomResult) : PublicRoomJoinState
}

data class ActiveRoomInfo(
    val roomCode: String,
    val playerId: Int,
    val playerCount: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val roomRepository: RoomRepository,
    private val drawingRepository: DrawingRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val POLL_INTERVAL_MS = 5000L
    }

    private val _publicRooms = MutableStateFlow<List<RoomResult>>(emptyList())
    val publicRooms: StateFlow<List<RoomResult>> = _publicRooms

    private val _savedDrawings = MutableStateFlow<List<CompletedDrawing>>(emptyList())
    val savedDrawings: StateFlow<List<CompletedDrawing>> = _savedDrawings

    private val _profile = MutableStateFlow(
        UserProfile(
            username = sessionManager.getUsername() ?: "Player",
            email = "",
            displayName = sessionManager.getDisplayName() ?: sessionManager.getUsername() ?: "Player",
            avatarUrl = sessionManager.getAvatarUrl()
        )
    )
    val profile: StateFlow<UserProfile> = _profile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _publicRoomJoinState = MutableStateFlow<PublicRoomJoinState>(PublicRoomJoinState.Idle)
    val publicRoomJoinState: StateFlow<PublicRoomJoinState> = _publicRoomJoinState

    private val _showProfileSheet = MutableStateFlow(false)
    val showProfileSheet: StateFlow<Boolean> = _showProfileSheet

    private val _activeRoom = MutableStateFlow<ActiveRoomInfo?>(null)
    val activeRoom: StateFlow<ActiveRoomInfo?> = _activeRoom

    val loggedInUsername: String get() = sessionManager.getUsername() ?: "Player"

    private var pollingJob: Job? = null

    init {
        loadActiveRoom()
        refreshHomeData()
        startPollingPublicRooms()
    }

    private fun loadActiveRoom() {
        val roomCode = sessionManager.getActiveRoom() ?: return
        // Validate that the room still exists and is not FINISHED before showing resume chip
        viewModelScope.launch {
            roomRepository.getRoom(roomCode).fold(
                onSuccess = { room ->
                    if (room.status == "FINISHED") {
                        // Room is already done — clear it
                        sessionManager.clearActiveRoom()
                        _activeRoom.value = null
                    } else {
                        _activeRoom.value = ActiveRoomInfo(
                            roomCode = roomCode,
                            playerId = sessionManager.getActivePlayerId(),
                            playerCount = sessionManager.getActivePlayerCount()
                        )
                    }
                },
                onFailure = {
                    // Room not found or network error: clear to avoid stale chip
                    sessionManager.clearActiveRoom()
                    _activeRoom.value = null
                }
            )
        }
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            profileRepository.getMyProfile().fold(
                onSuccess = { profile ->
                    _profile.value = profile
                    sessionManager.saveUsername(profile.username)
                    sessionManager.saveDisplayName(profile.displayName)
                    sessionManager.saveAvatarUrl(profile.avatarUrl)
                },
                onFailure = {
                    if (_errorMessage.value == null) {
                        _errorMessage.value = it.message ?: "Could not load profile"
                    }
                }
            )

            roomRepository.getPublicRooms().fold(
                onSuccess = { _publicRooms.value = it },
                onFailure = {
                    _publicRooms.value = emptyList()
                    _errorMessage.value = it.message ?: "Could not load public rooms"
                }
            )

            drawingRepository.getMyCompletedDrawings().fold(
                onSuccess = { _savedDrawings.value = it },
                onFailure = {
                    _savedDrawings.value = emptyList()
                    if (_errorMessage.value == null) {
                        _errorMessage.value = it.message ?: "Could not load saved drawings"
                    }
                }
            )

            // Reload active room state
            loadActiveRoom()

            _isLoading.value = false
        }
    }

    private fun startPollingPublicRooms() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                roomRepository.getPublicRooms().fold(
                    onSuccess = { _publicRooms.value = it },
                    onFailure = { /* silent on polling errors */ }
                )
            }
        }
    }

    fun joinPublicRoom(roomCode: String) {
        if (_publicRoomJoinState.value is PublicRoomJoinState.Loading) return
        viewModelScope.launch {
            _publicRoomJoinState.value = PublicRoomJoinState.Loading(roomCode)
            _errorMessage.value = null
            roomRepository.joinRoom(roomCode).fold(
                onSuccess = { joinedRoom ->
                    _publicRoomJoinState.value = PublicRoomJoinState.Success(joinedRoom)
                    _publicRooms.value = _publicRooms.value.filterNot { it.roomCode == roomCode }
                },
                onFailure = {
                    _publicRoomJoinState.value = PublicRoomJoinState.Idle
                    _errorMessage.value = it.message ?: "Could not join room"
                }
            )
        }
    }

    fun consumePublicRoomJoin() {
        _publicRoomJoinState.value = PublicRoomJoinState.Idle
    }

    fun refreshPublicRooms() = refreshHomeData()

    fun clearError() {
        _errorMessage.value = null
    }

    fun openProfileSheet() {
        _showProfileSheet.value = true
    }

    fun closeProfileSheet() {
        _showProfileSheet.value = false
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
