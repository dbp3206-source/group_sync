package com.groupsync.backend.knowledge.rag;

import java.time.LocalDateTime;
import java.util.Set;
import com.groupsync.backend.knowledge.model.ResourceType;

/**
 * Strongly-typed metadata filter constraints for RAG v2.
 * Directly maps to PostgreSQL relational predicates in vector and FTS retrieval queries.
 */
public record KnowledgeQueryFilters(
        Set<Long> resourceIds,
        Set<Long> collectionIds,
        Set<Long> tagIds,
        ResourceType resourceType,
        Boolean favorite,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {
    public static KnowledgeQueryFilters empty() {
        return new KnowledgeQueryFilters(null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return (resourceIds == null || resourceIds.isEmpty())
                && (collectionIds == null || collectionIds.isEmpty())
                && (tagIds == null || tagIds.isEmpty())
                && resourceType == null
                && favorite == null
                && createdAfter == null
                && createdBefore == null;
    }
}
