package com.codraw.CoDraw.service;

import com.codraw.CoDraw.dto.ChatMessageResponse;
import com.codraw.CoDraw.entity.ChatMessage;
import com.codraw.CoDraw.entity.Friendship;
import com.codraw.CoDraw.entity.FriendshipStatus;
import com.codraw.CoDraw.entity.User;
import com.codraw.CoDraw.repository.ChatMessageRepository;
import com.codraw.CoDraw.repository.FriendshipRepository;
import com.codraw.CoDraw.repository.UserRepository;
import com.codraw.CoDraw.handler.GlobalWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codraw.CoDraw.entity.MessageType;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final GlobalWebSocketHandler globalWebSocketHandler;
    private final Cloudinary cloudinary;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(String username, String friendUsername) {
        User user1 = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User user2 = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));
                
        // Ensure they are friends
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(user1, user2);
        if (friendship.isEmpty() || friendship.get().getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Users are not friends");
        }
        
        return chatMessageRepository.findChatHistoryOrderAsc(user1, user2).stream()
                .map(ChatMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse saveMessage(String senderUsername, String receiverUsername, String content) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
                
        // Ensure they are friends
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(sender, receiver);
        if (friendship.isEmpty() || friendship.get().getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Users are not friends");
        }
        
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setType(MessageType.TEXT);
        
        ChatMessageResponse response = ChatMessageResponse.fromEntity(chatMessageRepository.save(msg));
        globalWebSocketHandler.pushToUser(receiverUsername, response);
        
        return response;
    }

    @Transactional
    public ChatMessageResponse saveVoiceMessage(String senderUsername, String receiverUsername, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
                
        // Ensure they are friends
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(sender, receiver);
        if (friendship.isEmpty() || friendship.get().getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Users are not friends");
        }
        
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    audioFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "codraw/voices",
                            "resource_type", "auto"
                    )
            );
            String audioUrl = (String) uploadResult.get("secure_url");

            ChatMessage msg = new ChatMessage();
            msg.setSender(sender);
            msg.setReceiver(receiver);
            msg.setContent(audioUrl);
            msg.setType(MessageType.VOICE);
            
            ChatMessageResponse response = ChatMessageResponse.fromEntity(chatMessageRepository.save(msg));
            globalWebSocketHandler.pushToUser(receiverUsername, response);
            
            return response;
        } catch (IOException e) {
            throw new RuntimeException("Could not upload voice message", e);
        }
    }

    @Transactional
    public void markMessagesAsRead(String currentUsername, String friendUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User friend = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));
        chatMessageRepository.markAsRead(friend, currentUser);
    }
}
