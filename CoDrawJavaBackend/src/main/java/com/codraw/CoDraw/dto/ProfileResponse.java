package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;

    public static ProfileResponse fromUser(User user) {
        String displayName = (user.getDisplayName() == null || user.getDisplayName().isBlank())
                ? user.getUsername()
                : user.getDisplayName();
        return new ProfileResponse(
                user.getUsername(),
                user.getEmail(),
                displayName,
                user.getAvatarUrl()
        );
    }
}

