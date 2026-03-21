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
        
        ChatMessageResponse response = ChatMessageResponse.fromEntity(chatMessageRepository.save(msg));
        globalWebSocketHandler.pushToUser(receiverUsername, response);
        
        return response;
    }
}
