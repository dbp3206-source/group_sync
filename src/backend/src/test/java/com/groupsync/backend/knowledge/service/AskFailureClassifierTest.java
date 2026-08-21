package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

class AskFailureClassifierTest {
    @Test void rateLimitIsClassifiedBeforeGenericProviderFailure() {
        assertEquals(AskFailureCategory.RATE_LIMIT, AskFailureClassifier.classify(new RuntimeException("HTTP 429 RESOURCE_EXHAUSTED")));
    }
    @Test void timeoutIsClassified() {
        assertEquals(AskFailureCategory.TIMEOUT, AskFailureClassifier.classify(new RuntimeException("provider timed out")));
    }
    @Test void retrievalFailureIsNotRateLimit() {
        assertEquals(AskFailureCategory.RETRIEVAL, AskFailureClassifier.classify(new RuntimeException("vector database retrieval failed")));
    }
}
