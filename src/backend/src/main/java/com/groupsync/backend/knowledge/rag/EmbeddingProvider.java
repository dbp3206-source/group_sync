package com.groupsync.backend.knowledge.rag;

import java.util.List;

public interface EmbeddingProvider {

    float[] embedDocument(String content);

    float[] embedQuery(String content);

    record BatchResult(
            List<float[]> embeddings,
            int providerRequestCount,
            int totalDocuments
    ) {}

    default List<float[]> embedDocuments(List<String> texts) {
        return embedDocumentsWithBatchResult(texts).embeddings();
    }

    BatchResult embedDocumentsWithBatchResult(List<String> texts);
}
