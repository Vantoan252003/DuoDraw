package com.codraw.CoDraw.repository;

import com.codraw.CoDraw.entity.Friendship;
import com.codraw.CoDraw.entity.FriendshipStatus;
import com.codraw.CoDraw.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :u1 AND f.receiver = :u2) OR (f.requester = :u2 AND f.receiver = :u1)")
    Optional<Friendship> findFriendshipBetween(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.receiver = :user) AND f.status = :status")
    List<Friendship> findAllByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);
    
    List<Friendship> findByReceiverAndStatus(User receiver, FriendshipStatus status);
    
    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);
}
