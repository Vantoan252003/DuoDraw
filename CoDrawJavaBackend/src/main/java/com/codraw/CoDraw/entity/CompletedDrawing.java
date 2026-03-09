package com.codraw.CoDraw.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "completed_drawings")
public class CompletedDrawing {

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
    private Room.RoomType roomType;

    @Column(nullable = false, length = 50)
    private String savedByUsername;

    @Column(nullable = false)
    private Integer strokeCount;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String strokesJson;

    @Column(nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();
}

