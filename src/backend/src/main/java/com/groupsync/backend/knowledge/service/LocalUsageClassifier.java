package com.groupsync.backend.knowledge.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.LocalUsageStatus;
import com.groupsync.backend.knowledge.model.AiUsageEvent;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

/** Local telemetry only; this is intentionally not a provider quota estimate. */
@Service
public class LocalUsageClassifier {
    public static final String WINDOW = "ROLLING_24_HOURS";
    private static final Duration WINDOW_DURATION = Duration.ofHours(24);
    private static final Duration RATE_LIMIT_PRESSURE = Duration.ofHours(2);

    public LocalUsageStatus classify(List<AiUsageEvent> events, Instant now) {
        if (events == null || events.isEmpty()) return LocalUsageStatus.UNKNOWN;
        Instant windowStart = now.minus(WINDOW_DURATION);
        List<AiUsageEvent> recent = events.stream()
                .filter(event -> event.getCreatedAt() != null && !event.getCreatedAt().isBefore(windowStart))
                .toList();
        if (recent.isEmpty()) return LocalUsageStatus.UNKNOWN;
        Instant pressureStart = now.minus(RATE_LIMIT_PRESSURE);
        boolean recentRateLimit = recent.stream().anyMatch(event ->
                event.getFailureCategory() == AskFailureCategory.RATE_LIMIT
                        && event.getCreatedAt() != null && !event.getCreatedAt().isBefore(pressureStart));
        if (recentRateLimit) return LocalUsageStatus.RATE_LIMITED;

        long completed = recent.stream().filter(event -> "COMPLETE".equals(event.getRequestStatus())).count();
        long failed = recent.stream().filter(event -> "FAILED".equals(event.getRequestStatus())).count();
        long totalTokens = recent.stream().filter(event -> event.getTotalTokens() != null)
                .mapToLong(event -> event.getTotalTokens()).sum();
        if (completed >= 20 || failed >= 5 || totalTokens >= 100_000) return LocalUsageStatus.LOW;
        if (completed <= 3 && failed == 0 && totalTokens < 20_000) return LocalUsageStatus.COMFORTABLE;
        return LocalUsageStatus.MODERATE;
    }
}
