package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for RRF fusion logic inside HybridRetrievalStrategy. No Spring context required. */
class HybridRetrievalStrategyTest {

    /** Null-safe convenience constructor for test RetrievedChunk instances. */
    private static RetrievedChunk chunk(long id, long resourceId, String title, double distance) {
        return new RetrievedChunk(id, resourceId, title, 0, null, null, "content-" + id, distance);
    }

    // HybridRetrievalStrategy is tested only for its fuse() method — no Spring wiring needed.
    private final HybridRetrievalStrategy strategy = new HybridRetrievalStrategy(null, null,
            new GeminiProperties("key", "chat", "quality", "embedding", 768, 6, 2, 12, 60));

    @Test
    void rrfBoostsBothBranchesOverSingleBranch() {
        // chunk 1 appears in semantic at rank 1, chunk 2 appears in keyword at rank 1.
        // chunk 3 appears in BOTH at rank 3 — RRF should boost chunk 3 above single-branch top.
        List<RetrievedChunk> semantic = List.of(chunk(1, 10, "A", 0.1), chunk(2, 20, "B", 0.2), chunk(3, 30, "C", 0.3));
        List<RetrievedChunk> keyword = List.of(chunk(4, 40, "D", 0.1), chunk(5, 50, "E", 0.2), chunk(3, 30, "C", 0.3));

        List<RetrievedChunk> fused = strategy.fuse(semantic, keyword, 6);

        // chunk 3 appears in both branches → higher combined RRF score than chunks that appear once
        assertFalse(fused.isEmpty());
        assertEquals(5, fused.size(), "Should return all 5 distinct chunks");
        // chunk 3's RRF = 1/(60+3) + 1/(60+3) = 2/63 ≈ 0.0317
        // chunk 1's RRF = 1/(60+1) ≈ 0.0164; chunk 4's RRF = 1/(60+1) ≈ 0.0164
        // chunk 3 should outrank all single-branch chunks
        assertEquals(3L, fused.get(0).chunkId(), "Chunk 3 (appears in both branches) should rank first after RRF");
    }

    @Test
    void deduplicatesByChunkId() {
        RetrievedChunk chunkA = chunk(42, 100, "Resource A", 0.1);
        RetrievedChunk chunkADuplicate = chunk(42, 100, "Resource A", 0.3); // same id, different score

        List<RetrievedChunk> fused = strategy.fuse(List.of(chunkA), List.of(chunkADuplicate), 10);

        assertEquals(1, fused.size(), "Duplicate chunk IDs must be deduplicated");
        assertEquals(42L, fused.get(0).chunkId());
    }

    @Test
    void respectsTopKLimit() {
        List<RetrievedChunk> semantic = List.of(chunk(1, 1, "A", 0.1), chunk(2, 2, "B", 0.2),
                chunk(3, 3, "C", 0.3), chunk(4, 4, "D", 0.4));
        List<RetrievedChunk> keyword = List.of(chunk(5, 5, "E", 0.1), chunk(6, 6, "F", 0.2));

        List<RetrievedChunk> fused = strategy.fuse(semantic, keyword, 3);

        assertEquals(3, fused.size(), "fuse() must respect the topK limit");
    }

    @Test
    void handlesEmptySemanticBranch() {
        List<RetrievedChunk> keyword = List.of(chunk(10, 100, "Exact term", 0.05));

        List<RetrievedChunk> fused = strategy.fuse(List.of(), keyword, 6);

        assertFalse(fused.isEmpty(), "Should still return keyword results when semantic branch is empty");
        assertEquals(10L, fused.get(0).chunkId());
    }

    @Test
    void handlesEmptyKeywordBranch() {
        List<RetrievedChunk> semantic = List.of(chunk(20, 200, "Semantic concept", 0.15));

        List<RetrievedChunk> fused = strategy.fuse(semantic, List.of(), 6);

        assertFalse(fused.isEmpty(), "Should still return semantic results when keyword branch is empty");
        assertEquals(20L, fused.get(0).chunkId());
    }

    @Test
    void handlesBothBranchesEmpty() {
        List<RetrievedChunk> fused = strategy.fuse(List.of(), List.of(), 6);
        assertTrue(fused.isEmpty(), "Fusing two empty lists should return empty list");
    }

    @Test
    void distanceIsConsistentWithRrfScore() {
        // Chunk appearing in both branches at rank 1 has highest RRF score,
        // so its distance after normalization should be the smallest (best).
        List<RetrievedChunk> semantic = List.of(chunk(1, 1, "A", 0.1));
        List<RetrievedChunk> keyword = List.of(chunk(1, 1, "A", 0.1), chunk(2, 2, "B", 0.2));

        List<RetrievedChunk> fused = strategy.fuse(semantic, keyword, 6);

        // chunk 1 is in both at rank 1; chunk 2 is in keyword only at rank 2
        assertEquals(1L, fused.get(0).chunkId(), "Chunk in both branches should rank highest");
        assertTrue(fused.get(0).distance() < fused.get(1).distance(),
                "Better-ranked chunk must have smaller distance");
    }
}
