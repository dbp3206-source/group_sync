package com.groupsync.backend.knowledge.dto;

public record CitationResponse(Long chunkId, Long resourceId, String resourceTitle, Integer pageNumber,
        String section, int citationOrder, double relevanceScore, String evidenceExcerpt) { }
