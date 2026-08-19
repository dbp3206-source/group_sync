package com.groupsync.backend.knowledge.dto;

public record OrganizationCollectionSuggestionResponse(
        String name,
        Long existingCollectionId,
        String reason,
        double confidence
) {}
