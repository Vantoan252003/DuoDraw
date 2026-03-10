package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.domain.model.UserProfile
import com.toan.codraw.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _profile = MutableStateFlow(
        UserProfile(
            username = sessionManager.getUsername() ?: "Player",
            email = "",
            displayName = sessionManager.getDisplayName() ?: sessionManager.getUsername() ?: "Player",
            avatarUrl = sessionManager.getAvatarUrl()
        )
    )
    val profile: StateFlow<UserProfile> = _profile

    var displayName by mutableStateOf(sessionManager.getDisplayName() ?: sessionManager.getUsername().orEmpty())
        private set

    var selectedLanguageTag by mutableStateOf(sessionManager.getLanguageTag())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadProfile()
    }

    fun updateDisplayName(value: String) {
        displayName = value
    }

    fun updateLanguage(languageTag: String) {
        selectedLanguageTag = languageTag
        sessionManager.saveLanguageTag(languageTag)
    }

    fun clearStatus() {
        statusMessage = null
    }

    fun loadProfile() {
        viewModelScope.launch {
            profileRepository.getMyProfile().fold(
                onSuccess = { applyProfile(it) },
                onFailure = { statusMessage = it.message ?: "Could not load profile" }
            )
        }
    }

    fun saveProfile() {
        val value = displayName.trim()
        if (value.isBlank()) {
            statusMessage = "Display name cannot be empty"
            return
        }
        viewModelScope.launch {
            isSaving = true
            profileRepository.updateProfile(value).fold(
                onSuccess = {
                    applyProfile(it)
                    statusMessage = "Profile updated successfully."
                },
                onFailure = {
                    statusMessage = it.message ?: "Could not update profile"
                }
            )
            isSaving = false
        }
    }

    fun uploadAvatar(fileName: String, bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            isSaving = true
            profileRepository.uploadAvatar(fileName, bytes, mimeType).fold(
                onSuccess = {
                    applyProfile(it)
                    statusMessage = "Profile updated successfully."
                },
                onFailure = {
                    statusMessage = it.message ?: "Could not upload avatar"
                }
            )
            isSaving = false
        }
    }

    private fun applyProfile(profile: UserProfile) {
        _profile.value = profile
        displayName = profile.displayName
        sessionManager.saveUsername(profile.username)
        sessionManager.saveDisplayName(profile.displayName)
        sessionManager.saveAvatarUrl(profile.avatarUrl)
    }
}

