package com.codraw.CoDraw.controller;

import com.codraw.CoDraw.dto.FriendshipDto;
import com.codraw.CoDraw.dto.FriendChatResponse;
import com.codraw.CoDraw.dto.ProfileResponse;
import com.codraw.CoDraw.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {
    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<FriendshipDto> sendRequest(Authentication auth, @RequestParam String targetUsername) {
        return ResponseEntity.ok(friendshipService.sendRequest(auth.getName(), targetUsername));
    }

    @PostMapping("/respond/{id}")
    public ResponseEntity<FriendshipDto> respondToRequest(Authentication auth, @PathVariable Long id, @RequestParam boolean accept) {
        return ResponseEntity.ok(friendshipService.respondToRequest(auth.getName(), id, accept));
    }

    @GetMapping
    public ResponseEntity<List<FriendChatResponse>> getFriends(Authentication auth) {
        return ResponseEntity.ok(friendshipService.getFriends(auth.getName()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendshipDto>> getPendingRequests(Authentication auth) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(auth.getName()));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<FriendshipDto>> getSentRequests(Authentication auth) {
        return ResponseEntity.ok(friendshipService.getSentRequests(auth.getName()));
    }
}
