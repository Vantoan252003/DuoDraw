package com.codraw.CoDraw.repository;

import com.codraw.CoDraw.entity.CompletedDrawing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompletedDrawingRepository extends JpaRepository<CompletedDrawing, Long> {
    Optional<CompletedDrawing> findByRoomCode(String roomCode);
    List<CompletedDrawing> findByHostUsernameOrGuestUsernameOrSavedByUsernameOrderByCompletedAtDesc(
            String hostUsername,
            String guestUsername,
            String savedByUsername
    );
}
