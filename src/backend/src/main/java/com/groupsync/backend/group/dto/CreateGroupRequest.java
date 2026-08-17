package com.groupsync.backend.group.dto;

import com.groupsync.backend.group.model.GroupType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @NotNull GroupType type
) {
}
