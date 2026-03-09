package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.repository.RoomRepository
import com.toan.codraw.domain.repository.RoomResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoomUiState {
    data object Idle : RoomUiState()
    data object Loading : RoomUiState()
    data class Success(val room: RoomResult) : RoomUiState()
    data class Error(val message: String) : RoomUiState()
}

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RoomUiState>(RoomUiState.Idle)
    val uiState: StateFlow<RoomUiState> = _uiState

    var joinCode by mutableStateOf("")
    var selectedRoomType by mutableStateOf("PUBLIC")

    val username: String get() = sessionManager.getUsername() ?: "Player"

    fun updateSelectedRoomType(roomType: String) {
        selectedRoomType = roomType
    }

    fun updateJoinCode(code: String) {
        joinCode = code.uppercase().take(6)
    }

    fun createRoom() {
        viewModelScope.launch {
            _uiState.value = RoomUiState.Loading
            roomRepository.createRoom(selectedRoomType).fold(
                onSuccess = { _uiState.value = RoomUiState.Success(it) },
                onFailure = { _uiState.value = RoomUiState.Error(it.message ?: "Loi tao phong") }
            )
        }
    }

    fun joinRoom() {
        if (joinCode.isBlank()) {
            _uiState.value = RoomUiState.Error("Nhap ma phong!")
            return
        }
        viewModelScope.launch {
            _uiState.value = RoomUiState.Loading
            roomRepository.joinRoom(joinCode.trim()).fold(
                onSuccess = { _uiState.value = RoomUiState.Success(it) },
                onFailure = { _uiState.value = RoomUiState.Error(it.message ?: "Loi tham gia phong") }
            )
        }
    }

    fun reset() {
        _uiState.value = RoomUiState.Idle
    }
}
