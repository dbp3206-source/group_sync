package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for RRF fusion and branch resilience in HybridRetrievalStrategy. */
@ExtendWith(MockitoExtension.class)
class HybridRetrievalStrategyTest {

    /** Null-safe convenience constructor for test RetrievedChunk instances. */
    private static RetrievedChunk chunk(long id, long resourceId, String title, double distance) {
        return new RetrievedChunk(id, resourceId, title, 0, null, null, "content-" + id, distance);
    }

    private final GeminiProperties properties = new GeminiProperties("key", "chat", "quality", "embedding", 768, 6, 2, 12, 60, 30000);

    @Mock private SemanticRetrievalStrategy mockSemantic;
    @Mock private KeywordRetrievalStrategy mockKeyword;

    private final HybridRetrievalStrategy fuseOnlyStrategy = new HybridRetrievalStrategy(null, null,
            new GeminiProperties("key", "chat", "quality", "embedding", 768, 6, 2, 12, 60, 30000));

    @Test
    void rrfBoostsBothBranchesOverSingleBranch() {
        List<RetrievedChunk> semantic = List.of(chunk(1, 10, "A", 0.1), chunk(2, 20, "B", 0.2), chunk(3, 30, "C", 0.3));
        List<RetrievedChunk> keyword = List.of(chunk(4, 40, "D", 0.1), chunk(5, 50, "E", 0.2), chunk(3, 30, "C", 0.3));

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(semantic, keyword, 6);

        assertFalse(fused.isEmpty());
        assertEquals(5, fused.size(), "Should return all 5 distinct chunks");
        assertEquals(3L, fused.get(0).chunkId(), "Chunk 3 (appears in both branches) should rank first after RRF");
    }

    @Test
    void deduplicatesByChunkId() {
        RetrievedChunk chunkA = chunk(42, 100, "Resource A", 0.1);
        RetrievedChunk chunkADuplicate = chunk(42, 100, "Resource A", 0.3);

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(List.of(chunkA), List.of(chunkADuplicate), 10);

        assertEquals(1, fused.size(), "Duplicate chunk IDs must be deduplicated");
        assertEquals(42L, fused.get(0).chunkId());
    }

    @Test
    void respectsTopKLimit() {
        List<RetrievedChunk> semantic = List.of(chunk(1, 1, "A", 0.1), chunk(2, 2, "B", 0.2),
                chunk(3, 3, "C", 0.3), chunk(4, 4, "D", 0.4));
        List<RetrievedChunk> keyword = List.of(chunk(5, 5, "E", 0.1), chunk(6, 6, "F", 0.2));

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(semantic, keyword, 3);

        assertEquals(3, fused.size(), "fuse() must respect the topK limit");
    }

    @Test
    void handlesEmptySemanticBranch() {
        List<RetrievedChunk> keyword = List.of(chunk(10, 100, "Exact term", 0.05));

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(List.of(), keyword, 6);

        assertFalse(fused.isEmpty(), "Should still return keyword results when semantic branch is empty");
        assertEquals(10L, fused.get(0).chunkId());
    }

    @Test
    void handlesEmptyKeywordBranch() {
        List<RetrievedChunk> semantic = List.of(chunk(20, 200, "Semantic concept", 0.15));

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(semantic, List.of(), 6);

        assertFalse(fused.isEmpty(), "Should still return semantic results when keyword branch is empty");
        assertEquals(20L, fused.get(0).chunkId());
    }

    @Test
    void handlesBothBranchesEmpty() {
        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(List.of(), List.of(), 6);
        assertTrue(fused.isEmpty(), "Fusing two empty lists should return empty list");
    }

    @Test
    void distanceIsConsistentWithRrfScore() {
        List<RetrievedChunk> semantic = List.of(chunk(1, 1, "A", 0.1));
        List<RetrievedChunk> keyword = List.of(chunk(1, 1, "A", 0.1), chunk(2, 2, "B", 0.2));

        List<RetrievedChunk> fused = fuseOnlyStrategy.fuse(semantic, keyword, 6);

        assertEquals(1L, fused.get(0).chunkId(), "Chunk in both branches should rank highest");
        assertTrue(fused.get(0).distance() < fused.get(1).distance(),
                "Better-ranked chunk must have smaller distance");
    }

    @Test
    void retrieve_whenSemanticFailsAndKeywordSucceeds_returnsKeywordResultsGracefully() {
        HybridRetrievalStrategy hybrid = new HybridRetrievalStrategy(mockSemantic, mockKeyword, properties);

        when(mockSemantic.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("pgvector connection timeout"));
        when(mockKeyword.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(chunk(50, 1, "Keyword Doc", 0.1)));

        List<RetrievedChunk> result = hybrid.retrieve(1L, "test question", RetrievalScope.LIBRARY, null, null, null);

        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).chunkId());
    }

    @Test
    void retrieve_whenBothSucceedWithZeroCandidates_returnsEmptyList() {
        HybridRetrievalStrategy hybrid = new HybridRetrievalStrategy(mockSemantic, mockKeyword, properties);

        when(mockSemantic.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());
        when(mockKeyword.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        List<RetrievedChunk> result = hybrid.retrieve(1L, "no matches", RetrievalScope.LIBRARY, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void retrieve_whenBothBranchesFail_returnsEmptyListGracefully() {
        HybridRetrievalStrategy hybrid = new HybridRetrievalStrategy(mockSemantic, mockKeyword, properties);

        when(mockSemantic.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("pgvector error"));
        when(mockKeyword.retrieve(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("FTS error"));

        List<RetrievedChunk> result = hybrid.retrieve(1L, "both failed", RetrievalScope.LIBRARY, null, null, null);

        assertTrue(result.isEmpty());
    }
}
