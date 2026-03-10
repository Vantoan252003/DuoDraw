package com.codraw.CoDraw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String message;
}
