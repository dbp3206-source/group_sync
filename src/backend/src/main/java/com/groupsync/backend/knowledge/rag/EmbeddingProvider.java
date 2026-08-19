package com.groupsync.backend.knowledge.rag;

import java.util.List;

public interface EmbeddingProvider {
    float[] embedDocument(String content);
    float[] embedQuery(String content);
    List<float[]> embedDocuments(List<String> texts);
}
