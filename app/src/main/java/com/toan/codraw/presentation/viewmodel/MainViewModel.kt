package com.toan.codraw.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.data.remote.GlobalWebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val globalWebSocketManager: GlobalWebSocketManager,
    val sessionManager: SessionManager
) : ViewModel()
