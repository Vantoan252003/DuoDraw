package com.codraw.CoDraw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private Long id;
    private String roomCode;
    private String hostUsername;
    private String guestUsername;
    private String status;
    private String roomType;
    private int playerCount;
}

