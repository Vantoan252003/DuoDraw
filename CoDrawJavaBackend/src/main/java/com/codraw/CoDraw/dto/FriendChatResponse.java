package com.codraw.CoDraw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendChatResponse {
    private ProfileResponse friend;
    private ChatMessageResponse lastMessage;
    private int unreadCount;
}
