package com.groupsync.backend.knowledge.dto;

public record OrganizationTagSuggestionResponse(
        String name,
        Long existingTagId,
        String reason,
        double confidence
) {}
