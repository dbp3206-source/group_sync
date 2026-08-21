package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record AiUsageResponse(
        long completedRequests,
        long rateLimitCount,
        long failedRequests,
        long promptTokens,
        long outputTokens,
        long totalTokens,
        boolean exactProviderQuotaVisible,
        String providerQuotaState,
        String resetAt,
        Instant lastRecordedAt,
        LocalUsageStatus localUsageStatus,
        String usageWindow,
        String tokenScope
) { }
