package com.codraw.CoDraw.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_code", columnList = "roomCode", unique = true),
        @Index(name = "idx_room_type_status", columnList = "roomType, status"),
        @Index(name = "idx_room_host", columnList = "hostUsername")
})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String roomCode;

    @Column(nullable = false, length = 50)
    private String hostUsername;

    @Column(length = 50)
    private String guestUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType = RoomType.PUBLIC;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RoomStatus {
        WAITING, PLAYING, FINISHED
    }

    public enum RoomType {
        PUBLIC, PRIVATE
    }

    public Room(String roomCode, String hostUsername) {
        this.roomCode = roomCode;
        this.hostUsername = hostUsername;
    }

    public Room(String roomCode, String hostUsername, RoomType roomType) {
        this.roomCode = roomCode;
        this.hostUsername = hostUsername;
        this.roomType = roomType;
    }
}
