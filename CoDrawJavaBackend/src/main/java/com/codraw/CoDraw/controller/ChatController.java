package com.codraw.CoDraw.controller;

import com.codraw.CoDraw.dto.ChatMessageResponse;
import com.codraw.CoDraw.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/{friendUsername}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(Authentication auth, @PathVariable String friendUsername) {
        return ResponseEntity.ok(chatService.getChatHistory(auth.getName(), friendUsername));
    }

    @PostMapping("/{receiverUsername}")
    public ResponseEntity<ChatMessageResponse> sendMessage(Authentication auth, @PathVariable String receiverUsername, @RequestBody String content) {
        return ResponseEntity.ok(chatService.saveMessage(auth.getName(), receiverUsername, content));
    }

    @PostMapping("/voice/{receiverUsername}")
    public ResponseEntity<ChatMessageResponse> sendVoiceMessage(
            Authentication auth, 
            @PathVariable String receiverUsername, 
            @RequestParam("audio") MultipartFile audio
    ) {
        return ResponseEntity.ok(chatService.saveVoiceMessage(auth.getName(), receiverUsername, audio));
    }

    @PutMapping("/read/{senderUsername}")
    public ResponseEntity<Void> markAsRead(Authentication auth, @PathVariable String senderUsername) {
        chatService.markMessagesAsRead(auth.getName(), senderUsername);
        return ResponseEntity.ok().build();
    }
}
