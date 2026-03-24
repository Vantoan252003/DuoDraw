package com.codraw.CoDraw.controller;

import com.codraw.CoDraw.dto.ProfileResponse;
import com.codraw.CoDraw.dto.UpdateProfileRequest;
import com.codraw.CoDraw.dto.UpdatePasswordRequest;
import com.codraw.CoDraw.entity.User;
import com.codraw.CoDraw.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(profileService.getProfile(auth.getName()));
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getProfileByUsername(@PathVariable String username) {
        try {
            return ResponseEntity.ok(profileService.getProfileByUsername(username));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        try {
            return ResponseEntity.ok(profileService.updateProfile(auth.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            Authentication auth,
            @RequestBody UpdatePasswordRequest request
    ) {
        try {
            profileService.updatePassword(auth.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            Authentication auth,
            @RequestParam("avatar") MultipartFile avatar
    ) {
        try {
            return ResponseEntity.ok(profileService.uploadAvatar(auth.getName(), avatar));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

