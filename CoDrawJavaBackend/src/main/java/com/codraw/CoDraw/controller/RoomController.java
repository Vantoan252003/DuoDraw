package com.codraw.CoDraw.controller;

import com.codraw.CoDraw.dto.CreateRoomRequest;
import com.codraw.CoDraw.dto.RoomResponse;
import com.codraw.CoDraw.entity.User;
import com.codraw.CoDraw.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** POST /api/rooms/create  (yeu cau JWT) */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            Authentication auth,
            @RequestBody(required = false) CreateRoomRequest request) {
        try {
            String roomType = request != null ? request.getRoomType() : null;
            RoomResponse room = roomService.createRoom(auth.getName(), roomType);
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/rooms/public  (khong yeu cau JWT) */
    @GetMapping("/public")
    public ResponseEntity<?> getPublicRooms() {
        try {
            return ResponseEntity.ok(roomService.getPublicRooms());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/rooms/join?code=ABC123  (yeu cau JWT) */
    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(
            @RequestParam String code,
            Authentication auth) {
        try {
            RoomResponse room = roomService.joinRoom(code, auth.getName());
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/rooms/{code}  (yeu cau JWT) */
    @GetMapping("/{code}")
    public ResponseEntity<?> getRoom(@PathVariable String code) {
        try {
            return ResponseEntity.ok(roomService.getRoom(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
