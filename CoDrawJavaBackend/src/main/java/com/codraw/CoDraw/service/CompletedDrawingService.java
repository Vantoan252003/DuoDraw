package com.codraw.CoDraw.service;

import com.codraw.CoDraw.dto.CompletedDrawingResponse;
import com.codraw.CoDraw.entity.CompletedDrawing;
import com.codraw.CoDraw.entity.Room;
import com.codraw.CoDraw.model.StrokeMessage;
import com.codraw.CoDraw.repository.CompletedDrawingRepository;
import com.codraw.CoDraw.repository.RoomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CompletedDrawingService {

    private final CompletedDrawingRepository completedDrawingRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final CanvasStateService canvasStateService;
    private final ObjectMapper objectMapper;

    public CompletedDrawingService(
            CompletedDrawingRepository completedDrawingRepository,
            RoomRepository roomRepository,
            RoomService roomService,
            CanvasStateService canvasStateService,
            ObjectMapper objectMapper
    ) {
        this.completedDrawingRepository = completedDrawingRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.canvasStateService = canvasStateService;
        this.objectMapper = objectMapper;
    }

    public CompletedDrawingResponse completeDrawing(String roomCode, String username, List<StrokeMessage> strokes) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RuntimeException("Missing roomCode");
        }
        if (strokes == null || strokes.isEmpty()) {
            throw new RuntimeException("Drawing is empty and cannot be completed");
        }

        String normalizedRoomCode = roomCode.trim().toUpperCase();
        Room room = roomRepository.findByRoomCode(normalizedRoomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        boolean isParticipant = username.equals(room.getHostUsername()) || username.equals(room.getGuestUsername());
        if (!isParticipant) {
            throw new RuntimeException("You are not part of this room");
        }

        CompletedDrawing drawing = completedDrawingRepository.findByRoomCode(normalizedRoomCode)
                .orElseGet(CompletedDrawing::new);

        drawing.setRoomCode(normalizedRoomCode);
        drawing.setHostUsername(room.getHostUsername());
        drawing.setGuestUsername(room.getGuestUsername());
        drawing.setRoomType(room.getRoomType());
        drawing.setSavedByUsername(username);
        drawing.setStrokeCount(strokes.size());
        drawing.setStrokesJson(writeJson(strokes));
        drawing.setCompletedAt(LocalDateTime.now());

        CompletedDrawing saved = completedDrawingRepository.save(drawing);
        roomService.finishRoom(normalizedRoomCode);
        canvasStateService.clearStrokes(normalizedRoomCode);
        return toResponse(saved, strokes);
    }

    public CompletedDrawingResponse getCompletedDrawing(String roomCode) {
        CompletedDrawing drawing = completedDrawingRepository.findByRoomCode(roomCode.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("No completed drawing found for this room"));
        return toResponse(drawing, readJson(drawing.getStrokesJson()));
    }

    public List<CompletedDrawingResponse> getMyCompletedDrawings(String username) {
        return completedDrawingRepository
                .findByHostUsernameOrGuestUsernameOrSavedByUsernameOrderByCompletedAtDesc(username, username, username)
                .stream()
                .map(drawing -> toResponse(drawing, readJson(drawing.getStrokesJson())))
                .toList();
    }

    private String writeJson(List<StrokeMessage> strokes) {
        try {
            return objectMapper.writeValueAsString(strokes);
        } catch (Exception e) {
            throw new RuntimeException("Could not save drawing", e);
        }
    }

    private List<StrokeMessage> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Could not read drawing data", e);
        }
    }

    private CompletedDrawingResponse toResponse(CompletedDrawing drawing, List<StrokeMessage> strokes) {
        return new CompletedDrawingResponse(
                drawing.getId(),
                drawing.getRoomCode(),
                drawing.getHostUsername(),
                drawing.getGuestUsername(),
                drawing.getRoomType().name(),
                drawing.getSavedByUsername(),
                drawing.getStrokeCount(),
                drawing.getCompletedAt(),
                strokes
        );
    }
}
