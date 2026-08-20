package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record RecentActivityResponse(
        String type,
        String title,
        Instant occurredAt,
        String resumeUrl,
        String context
) {}
