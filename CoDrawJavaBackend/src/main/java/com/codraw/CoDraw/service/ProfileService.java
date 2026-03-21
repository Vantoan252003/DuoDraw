package com.codraw.CoDraw.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.codraw.CoDraw.dto.ProfileResponse;
import com.codraw.CoDraw.dto.UpdateProfileRequest;
import com.codraw.CoDraw.entity.User;
import com.codraw.CoDraw.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ProfileService {

    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;

    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, Cloudinary cloudinary, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cloudinary = cloudinary;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile(String username) {
        return getProfileByUsername(username);
    }

    public ProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileResponse.fromUser(user);
    }

    public ProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        if (displayName.isBlank()) {
            throw new RuntimeException("Display name is required");
        }
        if (displayName.length() > 80) {
            throw new RuntimeException("Display name is too long");
        }
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        currentUser.setDisplayName(displayName);
        userRepository.save(currentUser);
        return ProfileResponse.fromUser(currentUser);
    }

    public ProfileResponse uploadAvatar(String username, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new RuntimeException("Avatar file is required");
        }
        if (avatarFile.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new RuntimeException("Avatar file is too large");
        }
        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Avatar must be an image file");
        }
        ensureCloudinaryConfigured();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    avatarFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "codraw/avatars",
                            "public_id", "user_" + currentUser.getId() + "_avatar",
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );
            currentUser.setAvatarUrl((String) uploadResult.get("secure_url"));
            userRepository.save(currentUser);
            return ProfileResponse.fromUser(currentUser);
        } catch (IOException e) {
            throw new RuntimeException("Could not upload avatar", e);
        }
    }

    public void updatePassword(String username, com.codraw.CoDraw.dto.UpdatePasswordRequest request) {
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            throw new RuntimeException("Current password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new RuntimeException("New password is required");
        }
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }
        
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
            throw new RuntimeException("Incorrect current password");
        }
        
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }



    private void ensureCloudinaryConfigured() {
        Object cloudName = cloudinary.config.cloudName;
        Object apiKey = cloudinary.config.apiKey;
        Object apiSecret = cloudinary.config.apiSecret;
        if (cloudName == null || apiKey == null || apiSecret == null
                || cloudName.toString().isBlank() || apiKey.toString().isBlank() || apiSecret.toString().isBlank()) {
            throw new RuntimeException("Cloudinary is not configured on the server");
        }
    }
}

