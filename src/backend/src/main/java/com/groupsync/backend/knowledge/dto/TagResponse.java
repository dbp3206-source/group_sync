package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record TagResponse(
        Long id,
        String name,
        Instant createdAt
) {}
