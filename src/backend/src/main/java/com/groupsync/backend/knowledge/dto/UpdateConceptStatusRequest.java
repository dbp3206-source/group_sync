package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateConceptStatusRequest(
        @NotBlank(message = "Status cannot be blank")
        String status
) {}
