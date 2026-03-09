package com.codraw.CoDraw.service;

import com.codraw.CoDraw.dto.RoomResponse;
import com.codraw.CoDraw.entity.Room;
import com.codraw.CoDraw.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class RoomService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final RoomRepository roomRepository;
    private final SecureRandom random = new SecureRandom();

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /** Tao phong moi, tra ve roomCode 6 ky tu */
    public RoomResponse createRoom(String hostUsername, String roomType) {
        String code = generateUniqueCode();
        Room room = new Room(code, hostUsername, parseRoomType(roomType));
        roomRepository.save(room);
        return toResponse(room);
    }

    /** Tham gia phong theo roomCode */
    public RoomResponse joinRoom(String roomCode, String guestUsername) {
        Room room = roomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Phong khong ton tai: " + roomCode));

        if (room.getHostUsername().equals(guestUsername)) {
            // Host re-join, tra ve thong tin phong
            return toResponse(room);
        }
        if (guestUsername.equals(room.getGuestUsername())) {
            return toResponse(room);
        }
        if (room.getStatus() == Room.RoomStatus.FINISHED) {
            throw new RuntimeException("Phong da hoan thanh!");
        }
        if (room.getGuestUsername() != null || room.getStatus() == Room.RoomStatus.PLAYING) {
            throw new RuntimeException("Phong da day, dang co nguoi choi!");
        }

        room.setGuestUsername(guestUsername);
        room.setStatus(Room.RoomStatus.PLAYING);
        roomRepository.save(room);
        return toResponse(room);
    }

    /** Lay thong tin phong */
    public RoomResponse getRoom(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Phong khong ton tai"));
        return toResponse(room);
    }

    /** Lay danh sach phong choi cong khai dang cho */
    public List<RoomResponse> getPublicRooms() {
        return roomRepository
                .findByRoomTypeAndStatusOrderByCreatedAtDesc(Room.RoomType.PUBLIC, Room.RoomStatus.WAITING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Ket thuc phong */
    public void finishRoom(String roomCode) {
        roomRepository.findByRoomCode(roomCode.toUpperCase()).ifPresent(room -> {
            room.setStatus(Room.RoomStatus.FINISHED);
            roomRepository.save(room);
        });
    }

    private Room.RoomType parseRoomType(String roomType) {
        if (roomType == null || roomType.isBlank()) {
            return Room.RoomType.PUBLIC;
        }
        try {
            return Room.RoomType.valueOf(roomType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Loai phong khong hop le. Chi ho tro PUBLIC hoac PRIVATE");
        }
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = randomCode(6);
            attempts++;
            if (attempts > 100) throw new RuntimeException("Khong the tao room code");
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private RoomResponse toResponse(Room room) {
        int count = room.getGuestUsername() != null ? 2 : 1;
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getHostUsername(),
                room.getGuestUsername(),
                room.getStatus().name(),
                room.getRoomType().name(),
                count
        );
    }
}
