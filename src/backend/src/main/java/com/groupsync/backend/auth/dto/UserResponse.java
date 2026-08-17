package com.groupsync.backend.auth.dto;

import com.groupsync.backend.user.model.UserAccount;

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
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getSystemRole().name(),
            user.getTimeZone(),
            user.isProfileCompleted(),
            user.isProfileCompleted() ? "/api/users/" + user.getId() + "/avatar" : null
        );
    }
}
