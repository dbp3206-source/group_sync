package com.groupsync.backend.group.dto;

import java.util.List;

import com.groupsync.backend.group.model.Group;

public record GroupDetailResponse(Long id, String name, String description, String type, List<GroupMemberResponse> members) {
    public static GroupDetailResponse of(Group group, List<GroupMemberResponse> members) {
        return new GroupDetailResponse(group.getId(), group.getName(), group.getDescription(), group.getType().name(), members);
    }
}
