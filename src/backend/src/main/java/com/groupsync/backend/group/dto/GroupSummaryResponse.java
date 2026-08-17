package com.groupsync.backend.group.dto;

import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.Membership;

public record GroupSummaryResponse(Long id, String name, String description, String type, String role) {
    public static GroupSummaryResponse from(Membership membership) {
        Group group = membership.getGroup();
        return new GroupSummaryResponse(group.getId(), group.getName(), group.getDescription(), group.getType().name(), membership.getRole().name());
    }
}
