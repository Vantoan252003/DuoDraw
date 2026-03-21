package com.codraw.CoDraw.service;

import com.codraw.CoDraw.dto.FriendshipDto;
import com.codraw.CoDraw.dto.ProfileResponse;
import com.codraw.CoDraw.entity.Friendship;
import com.codraw.CoDraw.entity.FriendshipStatus;
import com.codraw.CoDraw.entity.User;
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
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final GlobalWebSocketHandler globalWebSocketHandler;

    @Transactional
    public FriendshipDto sendRequest(String requesterUsername, String targetUsername) {
        if (requesterUsername.equals(targetUsername)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        User receiver = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUsername));

        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(requester, receiver);
        if (existing.isPresent()) {
            throw new IllegalStateException("Friendship or request already exists");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatus.PENDING);
        
        FriendshipDto dto = FriendshipDto.fromEntity(friendshipRepository.save(friendship));
        globalWebSocketHandler.pushToUser(targetUsername, dto);
        return dto;
    }

    @Transactional
    public FriendshipDto respondToRequest(String responderUsername, Long friendshipId, boolean accept) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
                
        if (!friendship.getReceiver().getUsername().equals(responderUsername)) {
            throw new IllegalArgumentException("Unauthorized to respond to this request");
        }
        
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        
        friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED);
        FriendshipDto dto = FriendshipDto.fromEntity(friendshipRepository.save(friendship));
        globalWebSocketHandler.pushToUser(friendship.getRequester().getUsername(), dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getFriends(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        List<Friendship> friendships = friendshipRepository.findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED);
        
        return friendships.stream()
                .map(f -> f.getRequester().equals(user) ? f.getReceiver() : f.getRequester())
                .map(ProfileResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getPendingRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        List<Friendship> requests = friendshipRepository.findByReceiverAndStatus(user, FriendshipStatus.PENDING);
        return requests.stream()
                .map(FriendshipDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getSentRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        List<Friendship> requests = friendshipRepository.findByRequesterAndStatus(user, FriendshipStatus.PENDING);
        return requests.stream()
                .map(FriendshipDto::fromEntity)
                .collect(Collectors.toList());
    }
}
