package com.groupsync.backend.knowledge.rag;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Thin façade kept for backward compatibility with integration tests and any callers that were
 * written before the {@link RetrievalStrategy} interface was introduced.
 *
 * <p>All logic now lives in {@link SemanticRetrievalStrategy}.
 */
@Service
public class SemanticRetrievalService {

    private final SemanticRetrievalStrategy delegate;

    public SemanticRetrievalService(@Qualifier("semanticRetrieval") SemanticRetrievalStrategy delegate) {
        this.delegate = delegate;
    }

    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope, Long resourceId,
            List<Long> selectedResourceIds, Long collectionId) {
        return delegate.retrieve(ownerId, question, scope, resourceId, selectedResourceIds, collectionId);
    }
}
