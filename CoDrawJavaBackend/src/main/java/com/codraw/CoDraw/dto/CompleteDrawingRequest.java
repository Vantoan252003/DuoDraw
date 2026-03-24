package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.model.StrokeMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteDrawingRequest {

    @NotBlank(message = "Room code is required")
    private String roomCode;

    @NotEmpty(message = "Strokes list cannot be empty")
    private List<StrokeMessage> strokes;
}
