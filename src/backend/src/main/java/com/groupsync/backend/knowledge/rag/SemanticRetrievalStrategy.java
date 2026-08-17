package com.groupsync.backend.knowledge.rag;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Semantic retrieval branch: embeds the question with Gemini and finds the nearest document chunks
 * in pgvector using cosine distance. Scope isolation is enforced inside
 * {@link SemanticRetrievalRepository}.
 */
@Component("semanticRetrieval")
public class SemanticRetrievalStrategy implements RetrievalStrategy {

    private final EmbeddingProvider embeddingProvider;
    private final SemanticRetrievalRepository retrievalRepository;
    private final GeminiProperties properties;

    public SemanticRetrievalStrategy(EmbeddingProvider embeddingProvider,
                                     SemanticRetrievalRepository retrievalRepository,
                                     GeminiProperties properties) {
        this.embeddingProvider = embeddingProvider;
        this.retrievalRepository = retrievalRepository;
        this.properties = properties;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId) {
        return retrieve(ownerId, question, scope, resourceId, selectedResourceIds, collectionId, properties.ragTopK());
    }

    /**
     * Retrieves up to {@code limit} chunks. Used by {@link HybridRetrievalStrategy} to request a
     * larger candidate pool before fusion.
     */
    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId, int limit) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A question is required.");
        }
        float[] queryEmbedding = embeddingProvider.embedQuery(question.trim());
        return retrievalRepository.findNearest(ownerId, queryEmbedding, scope, resourceId,
                selectedResourceIds, collectionId, limit);
    }
}
