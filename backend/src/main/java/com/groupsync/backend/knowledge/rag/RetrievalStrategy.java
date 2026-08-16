package com.groupsync.backend.knowledge.rag;

import java.util.List;

/**
 * Contract for a retrieval branch inside KnowledgeOS.
 *
 * <p>Each concrete strategy retrieves a ranked list of {@link RetrievedChunk} candidates that are
 * strictly scoped to the authenticated owner and the supplied {@link RetrievalScope}. Scope leakage
 * — returning chunks owned by another user or from outside the requested scope — must never occur.
 *
 * <p>Known implementations:
 * <ul>
 *   <li>{@code SemanticRetrievalStrategy} – pgvector cosine-distance retrieval.</li>
 *   <li>{@code KeywordRetrievalStrategy} – PostgreSQL FTS lexical retrieval.</li>
 *   <li>{@code HybridRetrievalStrategy} – Reciprocal Rank Fusion of the two branches above.</li>
 * </ul>
 */
public interface RetrievalStrategy {

    /**
     * Retrieve a ranked list of chunks that best match {@code question} within the given scope.
     *
     * @param ownerId            authenticated user; all returned chunks must belong to this owner.
     * @param question           raw question text; must not be blank.
     * @param scope              retrieval scope; must be respected by every implementation.
     * @param resourceId         resource filter for {@link RetrievalScope#THIS_RESOURCE} (may be null otherwise).
     * @param selectedResourceIds resource filter for {@link RetrievalScope#SELECTED_RESOURCES} (may be empty otherwise).
     * @param collectionId       collection filter for {@link RetrievalScope#COLLECTION} (may be null otherwise).
     * @return ranked candidates, best match first, never null.
     */
    List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                  Long resourceId, List<Long> selectedResourceIds, Long collectionId);
}
