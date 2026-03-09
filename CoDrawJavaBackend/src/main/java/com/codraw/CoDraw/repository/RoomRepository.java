package com.codraw.CoDraw.repository;

import com.codraw.CoDraw.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);
    boolean existsByRoomCode(String roomCode);
    List<Room> findByRoomTypeAndStatusOrderByCreatedAtDesc(Room.RoomType roomType, Room.RoomStatus status);
}
