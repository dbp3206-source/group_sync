package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgressRequest(
        @NotNull(message = "Progress percent is required")
        @Min(value = 0, message = "Progress must be at least 0")
        @Max(value = 100, message = "Progress cannot exceed 100")
        Integer progressPercent
) {}
