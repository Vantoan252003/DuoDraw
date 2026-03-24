package com.codraw.CoDraw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 80, message = "Display name must be 1-80 characters")
    private String displayName;
}
