package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record ChatSessionSummaryResponse(
        Long id,
        String title,
        String scope,
        Long collectionId,
        Instant updatedAt
) {}
