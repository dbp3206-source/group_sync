package com.groupsync.backend.knowledge.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Hybrid retrieval using Reciprocal Rank Fusion (RRF) with metadata filtering and execution trace support.
 */
@Component("hybridRetrieval")
public class HybridRetrievalStrategy implements RetrievalStrategy {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalStrategy.class);

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

    public record HybridExecutionDetails(
            List<RetrievedChunk> fusedChunks,
            int semanticCandidateCount,
            int keywordCandidateCount,
            int totalInputCandidates,
            int rrfK
    ) {}

    @Override
    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId) {
        return retrieve(ownerId, question, scope, resourceId, selectedResourceIds, collectionId,
                KnowledgeQueryFilters.empty());
    }

    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId, KnowledgeQueryFilters filters) {
        return retrieveWithTrace(ownerId, question, scope, resourceId, selectedResourceIds, collectionId, filters).fusedChunks();
    }

    public HybridExecutionDetails retrieveWithTrace(Long ownerId, String question, RetrievalScope scope,
                                                    Long resourceId, List<Long> selectedResourceIds,
                                                    Long collectionId, KnowledgeQueryFilters filters) {
        int topK = properties.ragTopK();
        int candidateSize = Math.min(topK * properties.ragCandidateMultiplier(), properties.ragMaxCandidateSize());

        boolean semanticFailed = false;
        List<RetrievedChunk> semanticCandidates;
        try {
            semanticCandidates = semantic.retrieve(ownerId, question, scope, resourceId,
                    selectedResourceIds, collectionId, filters, candidateSize);
        } catch (RuntimeException ex) {
            semanticFailed = true;
            log.warn("[semantic_retrieval_failed] strategy=semantic scope={} resourceId={} exception={}: {}",
                    scope, resourceId, ex.getClass().getSimpleName(), ex.getMessage());
            semanticCandidates = List.of();
        }

        boolean keywordFailed = false;
        List<RetrievedChunk> keywordCandidates;
        try {
            keywordCandidates = keyword.retrieve(ownerId, question, scope, resourceId,
                    selectedResourceIds, collectionId, filters, candidateSize);
        } catch (RuntimeException ex) {
            keywordFailed = true;
            log.warn("[keyword_retrieval_failed] strategy=keyword scope={} resourceId={} exception={}: {}",
                    scope, resourceId, ex.getClass().getSimpleName(), ex.getMessage());
            keywordCandidates = List.of();
        }

        if (semanticFailed && keywordFailed) {
            log.warn("[hybrid_retrieval_failed] Both retrieval branches threw exceptions for query.");
            return new HybridExecutionDetails(List.of(), 0, 0, 0, properties.ragRrfK());
        } else if (semanticFailed) {
            log.warn("[hybrid_retrieval_degraded] Semantic branch failed; degraded to keyword-only retrieval (keywordCount={}).", keywordCandidates.size());
        } else if (keywordFailed) {
            log.warn("[hybrid_retrieval_degraded] Keyword branch failed; degraded to semantic-only retrieval (semanticCount={}).", semanticCandidates.size());
        } else if (semanticCandidates.isEmpty() && keywordCandidates.isEmpty()) {
            log.info("[hybrid_no_candidates] Neither semantic nor keyword retrieval returned candidates for query.");
            return new HybridExecutionDetails(List.of(), 0, 0, 0, properties.ragRrfK());
        }

        List<RetrievedChunk> fused = fuse(semanticCandidates, keywordCandidates, topK);
        return new HybridExecutionDetails(
                fused,
                semanticCandidates.size(),
                keywordCandidates.size(),
                semanticCandidates.size() + keywordCandidates.size(),
                properties.ragRrfK()
        );
    }

    /**
     * Fuses two ranked lists using Reciprocal Rank Fusion and returns the top {@code topK} chunks.
     */
    List<RetrievedChunk> fuse(List<RetrievedChunk> semantic, List<RetrievedChunk> keyword, int topK) {
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
                    int rrfK = properties.ragRrfK();
                    double maxPossibleRrf = 2.0 / (rrfK + 1.0);
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
        int rrfK = properties.ragRrfK();
        for (int i = 0; i < candidates.size(); i++) {
            RetrievedChunk chunk = candidates.get(i);
            int rank = i + 1; // 1-based rank
            double rrfContribution = 1.0 / (rrfK + rank);
            scores.computeIfAbsent(chunk.chunkId(), id -> new double[]{0.0})[0] += rrfContribution;
            representatives.putIfAbsent(chunk.chunkId(), chunk);
        }
    }
}
