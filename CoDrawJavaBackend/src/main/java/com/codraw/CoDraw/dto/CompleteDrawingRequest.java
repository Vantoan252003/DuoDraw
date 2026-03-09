package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.model.StrokeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteDrawingRequest {
    private String roomCode;
    private List<StrokeMessage> strokes;
}

