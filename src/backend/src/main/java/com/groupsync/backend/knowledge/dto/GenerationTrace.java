package com.groupsync.backend.knowledge.dto;

public record GenerationTrace(
        String model,
        int promptChunksCount,
        int verifiedCitationsCount
) {}
