package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.rag.RetrievalScope;

class AskPreflightServiceTest {
    private final AskPreflightService service = new AskPreflightService();

    @Test
    void estimateIsDeterministicAndDoesNotClaimProviderQuota() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Explain parent child retrieval", RetrievalScope.LIBRARY, null, List.of(), null, null);
        var first = service.estimate(request);
        var second = service.estimate(request);
        assertEquals(first, second);
        assertFalse(first.providerQuotaVisible());
        assertEquals("UNKNOWN", first.providerQuotaState());
        assertNull(first.resetAt());
    }

    @Test
    void longQuestionIsWarnedByLocalHeuristic() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "x".repeat(700), RetrievalScope.THIS_RESOURCE, 1L, List.of(), null, null);
        assertTrue(service.estimate(request).heavy());
    }
}
