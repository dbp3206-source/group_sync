package com.groupsync.backend.knowledge.rag;

public record RetrievedChunk(Long chunkId, Long resourceId, String resourceTitle, int chunkIndex,
        Integer pageNumber, String section, String content, double distance) { }
