package com.toan.codraw.presentation.viewmodel

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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _publicRooms = MutableStateFlow<List<RoomResult>>(emptyList())
    val publicRooms: StateFlow<List<RoomResult>> = _publicRooms

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val loggedInUsername: String get() = sessionManager.getUsername() ?: "Player"

    init {
        refreshPublicRooms()
    }

    fun refreshPublicRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            roomRepository.getPublicRooms().fold(
                onSuccess = { _publicRooms.value = it },
                onFailure = {
                    _publicRooms.value = emptyList()
                    _errorMessage.value = it.message ?: "Khong the tai danh sach phong"
                }
            )
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
