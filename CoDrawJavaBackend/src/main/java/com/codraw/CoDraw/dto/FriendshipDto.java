package com.codraw.CoDraw.dto;

import com.codraw.CoDraw.entity.Friendship;
import com.codraw.CoDraw.entity.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FriendshipDto {
    private Long id;
    private ProfileResponse requester;
    private ProfileResponse receiver;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    
    public static FriendshipDto fromEntity(Friendship friendship) {
        return new FriendshipDto(
                friendship.getId(),
                ProfileResponse.fromUser(friendship.getRequester()),
                ProfileResponse.fromUser(friendship.getReceiver()),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }
}
