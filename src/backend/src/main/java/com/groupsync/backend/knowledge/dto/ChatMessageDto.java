package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import java.util.List;

public record ChatMessageDto(
        Long id,
        String role,
        String content,
        Instant createdAt,
        List<CitationResponse> citations
) {}
