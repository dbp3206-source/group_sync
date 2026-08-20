package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record CollectionResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        long resourceCount
) {
    public CollectionResponse(Long id, String name, String description, Instant createdAt, Instant updatedAt) {
        this(id, name, description, createdAt, updatedAt, 0L);
    }
}
