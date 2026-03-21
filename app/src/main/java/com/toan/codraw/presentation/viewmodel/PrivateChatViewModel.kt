package com.toan.codraw.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toan.codraw.data.remote.GlobalWebSocketManager
import com.toan.codraw.data.remote.dto.ChatMessageResponseDto
import com.toan.codraw.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivateChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val globalWebSocketManager: GlobalWebSocketManager
) : ViewModel() {

    var currentFriendUsername: String = ""
    val chatMessages = mutableStateListOf<ChatMessageResponseDto>()

    fun initChat(friendUsername: String) {
        currentFriendUsername = friendUsername
        loadChatHistory()
        listenForMessages()
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            chatRepository.getChatHistory(currentFriendUsername).onSuccess {
                chatMessages.clear()
                chatMessages.addAll(it)
            }
        }
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            globalWebSocketManager.listener.chatMessages.collectLatest { msg ->
                if (msg.senderUsername == currentFriendUsername || msg.receiverUsername == currentFriendUsername) {
                    if (chatMessages.none { it.id == msg.id }) {
                        chatMessages.add(msg)
                    }
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendMessage(currentFriendUsername, content).onSuccess { msg ->
                if (chatMessages.none { it.id == msg.id }) {
                    chatMessages.add(msg)
                }
            }
        }
    }
}
