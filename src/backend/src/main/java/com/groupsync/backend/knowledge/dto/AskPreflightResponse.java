package com.groupsync.backend.knowledge.dto;

public record AskPreflightResponse(
        boolean heavy,
        int estimatedInputTokens,
        int estimatedContextCharacters,
        String estimateBasis,
        boolean providerQuotaVisible,
        String providerQuotaState,
        String resetAt,
        LocalUsageStatus localUsageStatus,
        String usageWindow,
        String warningLevel
) { }
