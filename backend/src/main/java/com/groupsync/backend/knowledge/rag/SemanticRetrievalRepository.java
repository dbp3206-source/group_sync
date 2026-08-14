package com.groupsync.backend.knowledge.rag;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SemanticRetrievalRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SemanticRetrievalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RetrievedChunk> findNearest(Long ownerId, float[] embedding, RetrievalScope scope,
            Long resourceId, List<Long> selectedResourceIds, Long collectionId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT dc.id, dc.resource_id, r.title, dc.chunk_index, dc.page_number, dc.section, dc.content,
                       dc.embedding <=> CAST(:embedding AS vector) AS distance
                FROM document_chunks dc
                JOIN resources r ON r.id = dc.resource_id
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("embedding", vectorLiteral(embedding))
                .addValue("limit", Math.min(Math.max(limit, 1), 12));
        sql.append(" WHERE r.owner_id = :ownerId AND dc.embedding IS NOT NULL ");

        switch (scope) {
            case THIS_RESOURCE -> {
                require(resourceId, "A resource is required for THIS_RESOURCE retrieval.");
                sql.append(" AND dc.resource_id = :resourceId ");
                parameters.addValue("resourceId", resourceId);
            }
            case SELECTED_RESOURCES -> {
                if (selectedResourceIds == null || selectedResourceIds.isEmpty()) {
                    throw new IllegalArgumentException("At least one resource is required for SELECTED_RESOURCES retrieval.");
                }
                sql.append(" AND dc.resource_id IN (:resourceIds) ");
                parameters.addValue("resourceIds", selectedResourceIds);
            }
            case COLLECTION -> {
                require(collectionId, "A collection is required for COLLECTION retrieval.");
                sql.append(" AND EXISTS (SELECT 1 FROM resource_collections rc WHERE rc.resource_id = r.id AND rc.collection_id = :collectionId) ");
                parameters.addValue("collectionId", collectionId);
            }
            case LIBRARY -> { }
        }
        sql.append(" ORDER BY dc.embedding <=> CAST(:embedding AS vector), dc.id LIMIT :limit");
        return jdbcTemplate.query(sql.toString(), parameters, (row, index) -> new RetrievedChunk(
                row.getLong("id"), row.getLong("resource_id"), row.getString("title"),
                row.getInt("chunk_index"), row.getObject("page_number", Integer.class), row.getString("section"),
                row.getString("content"), row.getDouble("distance")));
    }

    private String vectorLiteral(float[] values) {
        if (values == null || values.length != 768) {
            throw new IllegalArgumentException("A 768-dimensional query embedding is required.");
        }
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) literal.append(',');
            literal.append(values[index]);
        }
        return literal.append(']').toString();
    }

    private void require(Object value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
    }
}
