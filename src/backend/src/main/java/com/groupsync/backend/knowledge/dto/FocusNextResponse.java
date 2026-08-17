package com.groupsync.backend.knowledge.dto;

public record FocusNextResponse(Long resourceId, String title, String resourceType, int priority,
        boolean favorite, int progressPercent, String reason) { }
