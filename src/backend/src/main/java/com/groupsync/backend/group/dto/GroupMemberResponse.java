package com.groupsync.backend.group.dto;

import com.groupsync.backend.group.model.Membership;

public record GroupMemberResponse(Long userId, String displayName, String role) {
    public static GroupMemberResponse from(Membership membership) {
        return new GroupMemberResponse(
            membership.getUser().getId(),
            membership.getUser().getDisplayName(),
            membership.getRole().name());
    }
}
