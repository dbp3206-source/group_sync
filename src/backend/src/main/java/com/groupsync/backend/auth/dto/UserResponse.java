package com.groupsync.backend.auth.dto;

import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.model.UserAvatar;

public record UserResponse(
    Long id,
    String email,
    String displayName,
    String systemRole,
    String timeZone,
    boolean profileCompleted,
    String avatarUrl
) {
    public static UserResponse from(UserAccount user) {
        return from(user, null);
    }

    public static UserResponse from(UserAccount user, UserAvatar avatar) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getSystemRole().name(),
            user.getTimeZone(),
            user.isProfileCompleted(),
            avatar == null ? null : "/api/users/" + user.getId() + "/avatar?v=" + avatar.getUpdatedAt().toEpochMilli()
        );
    }
}
