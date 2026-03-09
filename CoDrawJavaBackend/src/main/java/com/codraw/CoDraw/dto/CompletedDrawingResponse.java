package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.model.StrokeMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedDrawingResponse {
    private Long id;
    private String roomCode;
    private String hostUsername;
    private String guestUsername;
    private String roomType;
    private String savedByUsername;
    private Integer strokeCount;
    private LocalDateTime completedAt;
    private List<StrokeMessage> strokes;
}

