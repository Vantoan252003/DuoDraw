package com.codraw.CoDraw.controller;

import com.codraw.CoDraw.dto.CompleteDrawingRequest;
import com.codraw.CoDraw.entity.User;
import com.codraw.CoDraw.service.CompletedDrawingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drawings")
@CrossOrigin(origins = "*")
public class CompletedDrawingController {

    private final CompletedDrawingService completedDrawingService;

    public CompletedDrawingController(CompletedDrawingService completedDrawingService) {
        this.completedDrawingService = completedDrawingService;
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeDrawing(
            @AuthenticationPrincipal User user,
            @RequestBody CompleteDrawingRequest request) {
        try {
            return ResponseEntity.ok(
                    completedDrawingService.completeDrawing(
                            request.getRoomCode(),
                            user.getUsername(),
                            request.getStrokes()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<?> getCompletedDrawing(@PathVariable String roomCode) {
        try {
            return ResponseEntity.ok(completedDrawingService.getCompletedDrawing(roomCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

