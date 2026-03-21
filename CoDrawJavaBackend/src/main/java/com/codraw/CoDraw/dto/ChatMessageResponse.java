package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private LocalDateTime timestamp;
    
    public static ChatMessageResponse fromEntity(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSender().getUsername(),
                msg.getReceiver().getUsername(),
                msg.getContent(),
                msg.getTimestamp()
        );
    }
}
