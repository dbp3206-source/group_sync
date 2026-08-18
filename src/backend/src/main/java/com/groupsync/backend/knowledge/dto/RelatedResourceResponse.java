package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record RelatedResourceResponse(
        Long id,
        String title,
        String description,
        String resourceType,
        String processingStatus,
        String relationType,
        Instant createdAt
) {}
