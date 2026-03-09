package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // Form fields
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    // Base URL field - user can edit on the login screen
    var baseUrl by mutableStateOf(sessionManager.getBaseUrl())

    fun isAlreadyLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getSavedUsername(): String = sessionManager.getUsername() ?: ""

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Vui lòng nhập đầy đủ thông tin!")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // Save the base URL first so Retrofit uses updated URL
            sessionManager.saveBaseUrl(baseUrl)
            val result = authRepository.login(username.trim(), password)
            result.fold(
                onSuccess = { auth ->
                    sessionManager.saveToken(auth.token)
                    sessionManager.saveUsername(auth.username)
                    _uiState.value = AuthUiState.Success(auth.username)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Lỗi không xác định")
                }
            )
        }
    }

    fun register() {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Vui lòng nhập đầy đủ thông tin!")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Mật khẩu xác nhận không khớp!")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Mật khẩu phải có ít nhất 6 ký tự!")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            sessionManager.saveBaseUrl(baseUrl)
            val result = authRepository.register(username.trim(), email.trim(), password)
            result.fold(
                onSuccess = { auth ->
                    sessionManager.saveToken(auth.token)
                    sessionManager.saveUsername(auth.username)
                    _uiState.value = AuthUiState.Success(auth.username)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Lỗi không xác định")
                }
            )
        }
    }

    fun logout() {
        sessionManager.logout()
        username = ""
        password = ""
        email = ""
        confirmPassword = ""
        _uiState.value = AuthUiState.Idle
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}

