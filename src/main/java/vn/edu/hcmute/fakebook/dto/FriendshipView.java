package vn.edu.hcmute.fakebook.dto;

import lombok.Builder;
import lombok.Getter;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;

@Getter
@Builder
public class FriendshipView {

    private Long friendshipId;
    private Long userId;
    private String username;
    private String fullName;
    private FriendshipStatus status;
    private boolean requestedByCurrentUser;
}