package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record ResourceNoteResponse(
        Long id,
        String content,
        Instant createdAt,
        Instant updatedAt
) {}
