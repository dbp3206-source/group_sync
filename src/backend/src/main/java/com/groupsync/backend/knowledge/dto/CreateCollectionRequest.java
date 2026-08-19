package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCollectionRequest(
        @NotBlank(message = "Collection name is required") String name,
        String description
) {}
