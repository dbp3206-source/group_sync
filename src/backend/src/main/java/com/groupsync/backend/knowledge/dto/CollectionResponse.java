package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record CollectionResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
