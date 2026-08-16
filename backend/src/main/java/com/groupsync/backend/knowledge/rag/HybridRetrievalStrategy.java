package com.groupsync.backend.knowledge.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Hybrid retrieval using Reciprocal Rank Fusion (RRF).
 *
 * <p>Architecture:
 * <pre>
 *   Question
 *     ├── SemanticRetrievalStrategy  →  semantic candidates  (top K×2)
 *     │         (pgvector cosine)
 *     └── KeywordRetrievalStrategy   →  keyword candidates   (top K×2)
 *               (PostgreSQL FTS)
 *                        │
 *                   Deduplicate
 *                   by chunk ID
 *                        │
 *              Reciprocal Rank Fusion
 *            score = Σ 1/(k + rank_i)
 *              k = 60  (standard RRF constant)
 *                        │
 *              Top-K fused candidates
 *                        │
 *                 Existing Gemini LLM
 *                        │
 *              Answer + persisted citations
 * </pre>
 *
 * <p>RRF is deterministic, requires no external model or calibrated weights, and handles the
 * case where a chunk appears in only one branch (missing rank contributes 0 to that branch).
 *
 * <p>Scope isolation: both underlying branches enforce identical owner + scope filters.
 * This strategy adds no post-hoc filtering and therefore cannot introduce leakage.
 */
@Component("hybridRetrieval")
public class HybridRetrievalStrategy implements RetrievalStrategy {

    /** Standard RRF constant. Reduces the impact of high-rank outliers. */
    private static final int RRF_K = 60;

    private final SemanticRetrievalStrategy semantic;
    private final KeywordRetrievalStrategy keyword;
    private final GeminiProperties properties;

    public HybridRetrievalStrategy(
            @Qualifier("semanticRetrieval") SemanticRetrievalStrategy semantic,
            @Qualifier("keywordRetrieval") KeywordRetrievalStrategy keyword,
            GeminiProperties properties) {
        this.semantic = semantic;
        this.keyword = keyword;
        this.properties = properties;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId) {
        int topK = properties.ragTopK();
        // Expand candidate pool before fusion so both branches have enough candidates to fuse from.
        int candidateSize = Math.min(topK * 2, 12);

        List<RetrievedChunk> semanticCandidates;
        try {
            semanticCandidates = semantic.retrieve(ownerId, question, scope, resourceId,
                    selectedResourceIds, collectionId, candidateSize);
        } catch (RuntimeException ex) {
            semanticCandidates = List.of();
        }

        List<RetrievedChunk> keywordCandidates;
        try {
            keywordCandidates = keyword.retrieve(ownerId, question, scope, resourceId,
                    selectedResourceIds, collectionId, candidateSize);
        } catch (RuntimeException ex) {
            keywordCandidates = List.of();
        }

        return fuse(semanticCandidates, keywordCandidates, topK);
    }

    /**
     * Fuses two ranked lists using Reciprocal Rank Fusion and returns the top {@code topK} chunks.
     *
     * <p>For each chunk, its RRF score is the sum of {@code 1 / (RRF_K + rank)} across all lists
     * in which it appears. Chunks missing from a list contribute 0 for that list.
     * A higher RRF score means a better overall rank; the returned list is sorted by score descending.
     * The {@code distance} field of each returned chunk is set to {@code 1 - rrfScore} for
     * consistency with the single-branch distance convention used downstream by citation mapping.
     *
     * @param semantic zero-indexed ranked list from the semantic branch (index 0 = rank 1).
     * @param keyword  zero-indexed ranked list from the keyword branch (index 0 = rank 1).
     * @param topK     maximum number of fused results to return.
     * @return merged, deduplicated, RRF-scored candidates.
     */
    List<RetrievedChunk> fuse(List<RetrievedChunk> semantic, List<RetrievedChunk> keyword, int topK) {
        // chunkId → accumulated RRF score and representative chunk
        Map<Long, double[]> scores = new LinkedHashMap<>();
        Map<Long, RetrievedChunk> representatives = new LinkedHashMap<>();

        accumulate(semantic, scores, representatives);
        accumulate(keyword, scores, representatives);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, double[]>comparingByValue((a, b) -> Double.compare(b[0], a[0])))
                .limit(topK)
                .map(entry -> {
                    RetrievedChunk source = representatives.get(entry.getKey());
                    double rrfScore = entry.getValue()[0];
                    // Normalize score relative to theoretical maximum (rank 1 in both branches = 2.0 / (RRF_K + 1))
                    double maxPossibleRrf = 2.0 / (RRF_K + 1.0);
                    double normalizedScore = Math.min(1.0, rrfScore / maxPossibleRrf);
                    double normalizedDistance = Math.max(0.0, 1.0 - normalizedScore);
                    return new RetrievedChunk(
                            source.chunkId(), source.resourceId(), source.resourceTitle(),
                            source.chunkIndex(), source.pageNumber(), source.section(),
                            source.content(), normalizedDistance);
                })
                .toList();
    }

    private void accumulate(List<RetrievedChunk> candidates, Map<Long, double[]> scores,
                            Map<Long, RetrievedChunk> representatives) {
        for (int i = 0; i < candidates.size(); i++) {
            RetrievedChunk chunk = candidates.get(i);
            int rank = i + 1; // 1-based rank
            double rrfContribution = 1.0 / (RRF_K + rank);
            scores.computeIfAbsent(chunk.chunkId(), id -> new double[]{0.0})[0] += rrfContribution;
            representatives.putIfAbsent(chunk.chunkId(), chunk);
        }
    }
}
