package com.groupsync.backend.knowledge.rag;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Expands top-ranked Child chunks to their enclosing Parent chunks for LLM context grounding.
 * Enforces ranking-first expansion, parent deduplication, and strict character/count context budgeting.
 * Preserves child chunk evidence for precise, verified citations and exposes execution trace metrics.
 */
@Component
public class ParentChildContextExpander {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final int maxParents;
    private final int maxContextChars;

    public ParentChildContextExpander(
            NamedParameterJdbcTemplate jdbcTemplate,
            @Value("${rag.parent-child.max-parents:5}") int maxParents,
            @Value("${rag.parent-child.max-context-chars:6000}") int maxContextChars) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxParents = maxParents > 0 ? maxParents : 5;
        this.maxContextChars = maxContextChars > 0 ? maxContextChars : 6000;
    }

    public int getMaxParents() {
        return maxParents;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public record ExpandedContext(
            List<RetrievedChunk> promptContextChunks,
            List<RetrievedChunk> citationEvidenceChunks,
            int uniqueParentsFound,
            int duplicateParentsDeduplicated,
            int charactersUsed
    ) {
        public ExpandedContext(List<RetrievedChunk> promptContextChunks, List<RetrievedChunk> citationEvidenceChunks) {
            this(promptContextChunks, citationEvidenceChunks, promptContextChunks.size(), 0,
                    promptContextChunks.stream().mapToInt(c -> c.content() != null ? c.content().length() : 0).sum());
        }
    }

    public ExpandedContext expand(List<RetrievedChunk> rankedChildren) {
        if (rankedChildren == null || rankedChildren.isEmpty()) {
            return new ExpandedContext(List.of(), List.of(), 0, 0, 0);
        }

        // Collect child IDs to lookup parent relationships in one single query
        List<Long> childIds = rankedChildren.stream().map(RetrievedChunk::chunkId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("childIds", childIds);

        Map<Long, RetrievedChunk> childToParentMap = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT c.id AS child_id, p.id AS parent_id, p.resource_id, r.title,
                       p.chunk_index, p.page_number, p.section, p.content
                FROM document_chunks c
                JOIN document_chunks p ON c.parent_chunk_id = p.id
                JOIN resources r ON r.id = p.resource_id
                WHERE c.id IN (:childIds)
                """,
                params,
                rs -> {
                    long childId = rs.getLong("child_id");
                    RetrievedChunk parent = new RetrievedChunk(
                            rs.getLong("parent_id"),
                            rs.getLong("resource_id"),
                            rs.getString("title"),
                            rs.getInt("chunk_index"),
                            rs.getObject("page_number", Integer.class),
                            rs.getString("section"),
                            rs.getString("content"),
                            0.0d
                    );
                    childToParentMap.put(childId, parent);
                }
        );

        Set<Long> seenParentIds = new HashSet<>();
        List<RetrievedChunk> promptChunks = new ArrayList<>();
        List<RetrievedChunk> citationEvidence = new ArrayList<>();
        int currentChars = 0;
        int duplicatesDeduplicated = 0;

        for (RetrievedChunk child : rankedChildren) {
            citationEvidence.add(child);

            RetrievedChunk parent = childToParentMap.get(child.chunkId());
            RetrievedChunk targetForPrompt = parent != null ? parent : child;

            if (seenParentIds.contains(targetForPrompt.chunkId())) {
                duplicatesDeduplicated++;
                continue; // Deduplicate: already added this parent block to prompt
            }

            int chunkLength = targetForPrompt.content() != null ? targetForPrompt.content().length() : 0;
            if (promptChunks.size() >= maxParents || (currentChars + chunkLength > maxContextChars && !promptChunks.isEmpty())) {
                continue; // Budget exceeded
            }

            seenParentIds.add(targetForPrompt.chunkId());
            promptChunks.add(new RetrievedChunk(
                    targetForPrompt.chunkId(),
                    targetForPrompt.resourceId(),
                    targetForPrompt.resourceTitle(),
                    targetForPrompt.chunkIndex(),
                    targetForPrompt.pageNumber(),
                    targetForPrompt.section(),
                    targetForPrompt.content(),
                    child.distance() // Retain highest child score
            ));
            currentChars += chunkLength;
        }

        return new ExpandedContext(promptChunks, citationEvidence, seenParentIds.size(), duplicatesDeduplicated, currentChars);
    }
}
