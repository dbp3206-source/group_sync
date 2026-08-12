package com.groupsync.backend.group.dto;

import com.groupsync.backend.group.model.GroupRole;

import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(@NotNull GroupRole role) {
}
