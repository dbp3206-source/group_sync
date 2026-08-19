package com.groupsync.backend.knowledge.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lexical retrieval branch using PostgreSQL full-text search with metadata filtering support.
 */
@Component("keywordRetrieval")
public class KeywordRetrievalStrategy implements RetrievalStrategy {

    private static final int MAX_LIMIT = 24;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KeywordRetrievalStrategy(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId) {
        return retrieve(ownerId, question, scope, resourceId, selectedResourceIds, collectionId,
                KnowledgeQueryFilters.empty(), 6);
    }

    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId, int limit) {
        return retrieve(ownerId, question, scope, resourceId, selectedResourceIds, collectionId,
                KnowledgeQueryFilters.empty(), limit);
    }

    public List<RetrievedChunk> retrieve(Long ownerId, String question, RetrievalScope scope,
                                         Long resourceId, List<Long> selectedResourceIds,
                                         Long collectionId, KnowledgeQueryFilters filters, int limit) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A question is required.");
        }
        String queryText = question.trim();
        String orQuery = buildOrTsquery(queryText);
        boolean hasOrQuery = !orQuery.isBlank();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("query", queryText)
                .addValue("orQuery", orQuery)
                .addValue("hasOrQuery", hasOrQuery)
                .addValue("limit", Math.min(Math.max(limit, 1), MAX_LIMIT));

        StringBuilder sql = new StringBuilder("""
                SELECT dc.id, dc.resource_id, r.title, dc.chunk_index, dc.page_number, dc.section, dc.content,
                       CASE 
                           WHEN dc.fts_content @@ plainto_tsquery('simple', :query) THEN 
                               ts_rank_cd(dc.fts_content, plainto_tsquery('simple', :query), 1) + 1.0
                           WHEN :hasOrQuery = TRUE AND dc.fts_content @@ to_tsquery('simple', :orQuery) THEN 
                               ts_rank_cd(dc.fts_content, to_tsquery('simple', :orQuery), 1)
                           ELSE 0.0
                       END AS fts_rank
                FROM document_chunks dc
                JOIN resources r ON r.id = dc.resource_id
                WHERE r.owner_id = :ownerId
                  AND (dc.chunk_level = 'CHILD' OR dc.chunk_level IS NULL)
                  AND (
                      dc.fts_content @@ plainto_tsquery('simple', :query)
                      OR (:hasOrQuery = TRUE AND dc.fts_content @@ to_tsquery('simple', :orQuery))
                  )
                """);

        switch (scope) {
            case THIS_RESOURCE -> {
                if (resourceId == null) {
                    throw new IllegalArgumentException("A resource is required for THIS_RESOURCE retrieval.");
                }
                sql.append(" AND dc.resource_id = :resourceId ");
                params.addValue("resourceId", resourceId);
            }
            case SELECTED_RESOURCES -> {
                if (selectedResourceIds == null || selectedResourceIds.isEmpty()) {
                    throw new IllegalArgumentException("At least one resource is required for SELECTED_RESOURCES retrieval.");
                }
                sql.append(" AND dc.resource_id IN (:resourceIds) ");
                params.addValue("resourceIds", selectedResourceIds);
            }
            case COLLECTION -> {
                if (collectionId == null) {
                    throw new IllegalArgumentException("A collection is required for COLLECTION retrieval.");
                }
                sql.append("""
                         AND EXISTS (
                             SELECT 1 FROM resource_collections rc
                             WHERE rc.resource_id = r.id AND rc.collection_id = :collectionId
                         )
                        """);
                params.addValue("collectionId", collectionId);
            }
            case LIBRARY -> { /* no extra filter — owner_id already applied */ }
        }

        // Relational Metadata Filters
        if (filters != null && !filters.isEmpty()) {
            if (filters.resourceType() != null) {
                sql.append(" AND r.resource_type = :filterResourceType ");
                params.addValue("filterResourceType", filters.resourceType().name());
            }
            if (filters.favorite() != null) {
                sql.append(" AND r.favorite = :filterFavorite ");
                params.addValue("filterFavorite", filters.favorite());
            }
            if (filters.createdAfter() != null) {
                sql.append(" AND r.created_at >= :filterCreatedAfter ");
                params.addValue("filterCreatedAfter", filters.createdAfter());
            }
            if (filters.createdBefore() != null) {
                sql.append(" AND r.created_at <= :filterCreatedBefore ");
                params.addValue("filterCreatedBefore", filters.createdBefore());
            }
            if (filters.resourceIds() != null && !filters.resourceIds().isEmpty()) {
                sql.append(" AND r.id IN (:filterResourceIds) ");
                params.addValue("filterResourceIds", filters.resourceIds());
            }
            if (filters.collectionIds() != null && !filters.collectionIds().isEmpty()) {
                sql.append(" AND EXISTS (SELECT 1 FROM resource_collections rc WHERE rc.resource_id = r.id AND rc.collection_id IN (:filterCollectionIds)) ");
                params.addValue("filterCollectionIds", filters.collectionIds());
            }
            if (filters.tagIds() != null && !filters.tagIds().isEmpty()) {
                sql.append(" AND EXISTS (SELECT 1 FROM resource_tags rt WHERE rt.resource_id = r.id AND rt.tag_id IN (:filterTagIds)) ");
                params.addValue("filterTagIds", filters.tagIds());
            }
        }

        sql.append(" ORDER BY fts_rank DESC, dc.id LIMIT :limit");

        return jdbcTemplate.query(sql.toString(), params, (row, rowIndex) -> {
            double ftsRank = row.getDouble("fts_rank");
            double distance = 1.0 - Math.min(ftsRank, 1.0);
            return new RetrievedChunk(
                    row.getLong("id"),
                    row.getLong("resource_id"),
                    row.getString("title"),
                    row.getInt("chunk_index"),
                    row.getObject("page_number", Integer.class),
                    row.getString("section"),
                    row.getString("content"),
                    distance);
        });
    }

    private static String buildOrTsquery(String text) {
        if (text == null || text.isBlank()) return "";
        String[] rawTokens = text.replaceAll("[^a-zA-Z0-9\\u00C0-\\u1EF9\\-_]", " ").split("\\s+");
        List<String> valid = new ArrayList<>();
        for (String token : rawTokens) {
            String clean = token.trim().replace("'", "").replace("&", "").replace("|", "").replace("!", "");
            if (clean.length() >= 2 && !clean.equalsIgnoreCase("hoặc") && !clean.equalsIgnoreCase("and") && !clean.equalsIgnoreCase("or")) {
                valid.add(clean + ":*");
            }
        }
        if (valid.isEmpty()) return "";
        return String.join(" | ", valid);
    }

    public boolean hasAnyMatch(String question) {
        if (question == null || question.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM document_chunks dc WHERE dc.fts_content @@ plainto_tsquery('simple', :query)",
                new MapSqlParameterSource("query", question.trim()),
                Integer.class);
        return count != null && count > 0;
    }

    public List<RetrievedChunk> retrieveForTest(Long ownerId, String question, int limit) {
        return retrieve(ownerId, question, RetrievalScope.LIBRARY, null, new ArrayList<>(), null, limit);
    }
}
