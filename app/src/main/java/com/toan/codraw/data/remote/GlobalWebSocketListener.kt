package com.toan.codraw.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.toan.codraw.data.remote.dto.ChatMessageResponseDto
import com.toan.codraw.data.remote.dto.FriendshipDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalWebSocketListener @Inject constructor(private val gson: Gson) : WebSocketListener() {

    private val _chatMessages = MutableSharedFlow<ChatMessageResponseDto>(extraBufferCapacity = 64)
    val chatMessages = _chatMessages.asSharedFlow()

    private val _friendshipEvents = MutableSharedFlow<FriendshipDto>(extraBufferCapacity = 64)
    val friendshipEvents = _friendshipEvents.asSharedFlow()

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val jsonObject = gson.fromJson(text, JsonObject::class.java)
            if (jsonObject.has("senderUsername") && jsonObject.has("content")) {
                val msg = gson.fromJson(jsonObject, ChatMessageResponseDto::class.java)
                _chatMessages.tryEmit(msg)
            } else if (jsonObject.has("requester") && jsonObject.has("status")) {
                val friendEvent = gson.fromJson(jsonObject, FriendshipDto::class.java)
                _friendshipEvents.tryEmit(friendEvent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        t.printStackTrace()
    }
}
