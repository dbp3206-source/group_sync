package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTagRequest(
        @NotBlank(message = "Tag name is required") String name
) {}
