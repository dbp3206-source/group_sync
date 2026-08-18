package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record ResourceActivityResponse(
        String processingStatus,
        int progressPercent,
        long noteCount,
        Instant createdAt,
        Instant updatedAt,
        Instant lastOpenedAt
) {}
