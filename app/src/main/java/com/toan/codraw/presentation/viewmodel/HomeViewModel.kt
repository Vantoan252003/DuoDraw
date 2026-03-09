package com.toan.codraw.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.model.CompletedDrawing
import com.toan.codraw.domain.repository.DrawingRepository
import com.toan.codraw.domain.repository.RoomRepository
import com.toan.codraw.domain.repository.RoomResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val roomRepository: RoomRepository,
    private val drawingRepository: DrawingRepository
) : ViewModel() {

    private val _publicRooms = MutableStateFlow<List<RoomResult>>(emptyList())
    val publicRooms: StateFlow<List<RoomResult>> = _publicRooms

    private val _savedDrawings = MutableStateFlow<List<CompletedDrawing>>(emptyList())
    val savedDrawings: StateFlow<List<CompletedDrawing>> = _savedDrawings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val loggedInUsername: String get() = sessionManager.getUsername() ?: "Player"

    init {
        refreshHomeData()
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

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

            _isLoading.value = false
        }
    }

    fun refreshPublicRooms() = refreshHomeData()

    fun clearError() {
        _errorMessage.value = null
    }
}
