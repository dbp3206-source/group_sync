package com.groupsync.backend.auth.dto;

import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.auth.security.AuthenticatedUser;

public record UserResponse(Long id, String email, String displayName, String systemRole) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getSystemRole().name());
    }

    public static UserResponse from(AuthenticatedUser user) {
        String role = user.getAuthorities().stream().findFirst().map(authority -> authority.getAuthority().replace("ROLE_", "")).orElse("USER");
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), role);
    }
}
