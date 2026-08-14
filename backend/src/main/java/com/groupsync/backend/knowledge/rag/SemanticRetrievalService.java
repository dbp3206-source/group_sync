package com.groupsync.backend.knowledge.rag;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SemanticRetrievalService {
    private final EmbeddingProvider embeddingProvider;
    private final SemanticRetrievalRepository retrievalRepository;
    private final GeminiProperties properties;

    public SemanticRetrievalService(EmbeddingProvider embeddingProvider, SemanticRetrievalRepository retrievalRepository,
            GeminiProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.retrievalRepository = retrievalRepository;
        this.properties = properties;
    }

    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope, Long resourceId,
            List<Long> selectedResourceIds, Long collectionId) {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("A question is required.");
        float[] queryEmbedding = embeddingProvider.embedQuery(question.trim());
        return retrievalRepository.findNearest(ownerId, queryEmbedding, scope, resourceId, selectedResourceIds,
                collectionId, properties.ragTopK());
    }
}
