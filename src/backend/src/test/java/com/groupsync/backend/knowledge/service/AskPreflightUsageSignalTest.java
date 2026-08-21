package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.model.AiUsageEvent;
import com.groupsync.backend.knowledge.model.AskFailureCategory;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.repository.AiUsageEventRepository;

@ExtendWith(MockitoExtension.class)
class AskPreflightUsageSignalTest {
    @Mock private AiUsageEventRepository usageRepository;
    private AskPreflightService service;

    @BeforeEach
    void setUp() {
        service = new AskPreflightService(usageRepository, new LocalUsageClassifier());
    }

    @Test
    void heavyLowUsageShowsStrongLocalWarning() {
        when(usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(1L), any(Instant.class)))
                .thenReturn(repeatedCompletedEvents(20));
        var result = service.estimate(1L, new AskKnowledgeRequest(null, "x".repeat(700), RetrievalScope.LIBRARY, null, List.of(), null, null));
        assertEquals("LOW", result.localUsageStatus().name());
        assertEquals("STRONG", result.warningLevel());
    }

    @Test
    void heavyRateLimitedUsageShowsStrongLocalWarning() {
        AiUsageEvent event = new AiUsageEvent(null, 1L, "Gemini", "model", "FAILED", null, null, null, null, null, null, 1L, AskFailureCategory.RATE_LIMIT);
        ReflectionTestUtils.setField(event, "createdAt", Instant.now().minusSeconds(60));
        when(usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(1L), any(Instant.class))).thenReturn(List.of(event));
        var result = service.estimate(1L, new AskKnowledgeRequest(null, "x".repeat(700), RetrievalScope.LIBRARY, null, List.of(), null, null));
        assertEquals("RATE_LIMITED", result.localUsageStatus().name());
        assertEquals("STRONG", result.warningLevel());
    }

    @Test
    void lightComfortableUsageHasNoWarning() {
        AiUsageEvent event = new AiUsageEvent(null, 1L, "Gemini", "model", "COMPLETE", null, null, null, 1000, null, null, 1L, null);
        ReflectionTestUtils.setField(event, "createdAt", Instant.now().minusSeconds(60));
        when(usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(1L), any(Instant.class))).thenReturn(List.of(event));
        var result = service.estimate(1L, new AskKnowledgeRequest(null, "short question", RetrievalScope.THIS_RESOURCE, 1L, List.of(), null, null));
        assertEquals("COMFORTABLE", result.localUsageStatus().name());
        assertEquals("NONE", result.warningLevel());
    }

    private List<AiUsageEvent> repeatedCompletedEvents(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> {
            AiUsageEvent event = new AiUsageEvent(null, 1L, "Gemini", "model", "COMPLETE", null, null, null, 1000, null, null, 1L, null);
            ReflectionTestUtils.setField(event, "createdAt", Instant.now().minusSeconds(index * 60L));
            return event;
        }).toList();
    }
}
