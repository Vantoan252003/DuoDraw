package com.codraw.CoDraw.repository;

import com.codraw.CoDraw.entity.ChatMessage;
import com.codraw.CoDraw.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistoryOrderAsc(@Param("user1") User user1, @Param("user2") User user2);
}
