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
        LocalDateTime createdBefore,
        boolean impossible
) {
    public KnowledgeQueryFilters(
            Set<Long> resourceIds,
            Set<Long> collectionIds,
            Set<Long> tagIds,
            ResourceType resourceType,
            Boolean favorite,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        this(resourceIds, collectionIds, tagIds, resourceType, favorite, createdAfter, createdBefore, false);
    }

    public static KnowledgeQueryFilters empty() {
        return new KnowledgeQueryFilters(null, null, null, null, null, null, null, false);
    }

    public static KnowledgeQueryFilters impossibleFilter() {
        return new KnowledgeQueryFilters(Set.of(-1L), Set.of(-1L), Set.of(-1L), null, null, null, null, true);
    }

    public boolean isEmpty() {
        return !impossible
                && (resourceIds == null || resourceIds.isEmpty())
                && (collectionIds == null || collectionIds.isEmpty())
                && (tagIds == null || tagIds.isEmpty())
                && resourceType == null
                && favorite == null
                && createdAfter == null
                && createdBefore == null;
    }
}
