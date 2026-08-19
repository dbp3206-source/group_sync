package com.groupsync.backend.knowledge.dto;

public record OrganizationRelatedSuggestionResponse(
        Long resourceId,
        String title,
        String reason,
        double similarity
) {}
