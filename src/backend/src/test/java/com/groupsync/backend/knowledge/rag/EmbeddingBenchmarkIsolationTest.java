package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.rag.EmbeddingModelComparisonBenchmarkTest.IndexedVector;

class EmbeddingBenchmarkIsolationTest {
    @Test void rankingRejectsMixedEmbeddingSpaces() {
        List<IndexedVector> mixed = List.of(new IndexedVector("gemini-embedding-2", "doc.md", new float[]{1,0}));
        assertThrows(IllegalArgumentException.class, () ->
                EmbeddingModelComparisonBenchmarkTest.rank("gemini-embedding-001", new float[]{1,0}, mixed, Set.of()));
    }

    @Test void rankingHonorsScopeFilter() {
        List<IndexedVector> index = List.of(
                new IndexedVector("gemini-embedding-001", "inside.md", new float[]{.8f,.2f}),
                new IndexedVector("gemini-embedding-001", "outside.md", new float[]{1,0}));
        assertEquals(List.of("inside.md"), EmbeddingModelComparisonBenchmarkTest.rank(
                "gemini-embedding-001", new float[]{1,0}, index, Set.of("inside.md")));
    }
}
