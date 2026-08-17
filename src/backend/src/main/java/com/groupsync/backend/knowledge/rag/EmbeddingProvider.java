package com.groupsync.backend.knowledge.rag;

public interface EmbeddingProvider {
    float[] embedDocument(String content);
    float[] embedQuery(String content);
}
