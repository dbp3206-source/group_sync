package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.dto.LocalUsageStatus;
import com.groupsync.backend.knowledge.model.AiUsageEvent;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

class LocalUsageClassifierTest {
    private final LocalUsageClassifier classifier = new LocalUsageClassifier();
    private final Instant now = Instant.parse("2026-08-21T00:00:00Z");

    @Test void noTelemetryIsUnknown() {
        assertEquals(LocalUsageStatus.UNKNOWN, classifier.classify(List.of(), now));
    }

    @Test void recentRateLimitWinsOverOtherSignals() {
        AiUsageEvent event = event(now.minus(20, ChronoUnit.MINUTES), "FAILED", AskFailureCategory.RATE_LIMIT, null);
        assertEquals(LocalUsageStatus.RATE_LIMITED, classifier.classify(List.of(event), now));
    }

    @Test void lowUsageIsComfortableAndOldEventsAreOutsideWindow() {
        AiUsageEvent comfortable = event(now.minus(1, ChronoUnit.HOURS), "COMPLETE", null, 1000);
        AiUsageEvent old = event(now.minus(25, ChronoUnit.HOURS), "COMPLETE", null, 1000);
        assertEquals(LocalUsageStatus.COMFORTABLE, classifier.classify(List.of(comfortable, old), now));
    }

    @Test void repeatedPressureIsLow() {
        List<AiUsageEvent> events = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> event(now.minus(index, ChronoUnit.MINUTES), "COMPLETE", null, 1000))
                .toList();
        assertEquals(LocalUsageStatus.LOW, classifier.classify(events, now));
    }

    private AiUsageEvent event(Instant createdAt, String status, AskFailureCategory failure, Integer totalTokens) {
        AiUsageEvent event = new AiUsageEvent(null, 1L, "Gemini", "model", status, null, null, null, totalTokens,
                null, null, 1L, failure);
        org.springframework.test.util.ReflectionTestUtils.setField(event, "createdAt", createdAt);
        return event;
    }
}
