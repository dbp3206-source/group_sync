package com.groupsync.backend.knowledge.dto;

import com.groupsync.backend.knowledge.rag.RetrievalScope;

public record FilterTrace(
        RetrievalScope scope,
        String resourceType,
        Boolean favorite,
        Integer collectionCount,
        Integer tagCount,
        Integer eligibleResourceCount,
        String createdAfter,
        String createdBefore
) {}
