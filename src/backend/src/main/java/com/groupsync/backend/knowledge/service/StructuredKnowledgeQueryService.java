package com.groupsync.backend.knowledge.service;

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.knowledge.rag.KnowledgeQueryFilters;
import com.groupsync.backend.knowledge.rag.QueryOperation;
import com.groupsync.backend.knowledge.rag.QueryPlan;
import com.groupsync.backend.knowledge.rag.RetrievalScope;

/**
 * Executes safe, parameterized relational queries for structured metadata questions (e.g. COUNT, LIST).
 * Directly resolves answers from PostgreSQL without invoking semantic vector search or executing raw LLM SQL.
 */
@Service
public class StructuredKnowledgeQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StructuredKnowledgeQueryService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record StructuredResult(
            String textResponse,
            long count,
            List<String> items
    ) {}

    @Transactional(readOnly = true)
    public StructuredResult execute(Long ownerId, QueryPlan plan, RetrievalScope scope,
                                    Long thisResourceId, List<Long> selectedResourceIds, Long collectionId) {
        KnowledgeQueryFilters filters = plan.filters() != null ? plan.filters() : KnowledgeQueryFilters.empty();
        QueryOperation operation = plan.operation() != null ? plan.operation() : QueryOperation.COUNT;

        MapSqlParameterSource params = new MapSqlParameterSource("ownerId", ownerId);
        StringBuilder where = new StringBuilder(" WHERE r.owner_id = :ownerId ");

        // Scope filter
        switch (scope) {
            case THIS_RESOURCE -> {
                if (thisResourceId != null) {
                    where.append(" AND r.id = :thisResourceId ");
                    params.addValue("thisResourceId", thisResourceId);
                }
            }
            case SELECTED_RESOURCES -> {
                if (selectedResourceIds != null && !selectedResourceIds.isEmpty()) {
                    where.append(" AND r.id IN (:selectedResourceIds) ");
                    params.addValue("selectedResourceIds", selectedResourceIds);
                }
            }
            case COLLECTION -> {
                if (collectionId != null) {
                    where.append(" AND EXISTS (SELECT 1 FROM resource_collections rc WHERE rc.resource_id = r.id AND rc.collection_id = :scopedCollectionId) ");
                    params.addValue("scopedCollectionId", collectionId);
                }
            }
            case LIBRARY -> { }
        }

        // Metadata filters
        if (filters.resourceType() != null) {
            where.append(" AND r.resource_type = :resType ");
            params.addValue("resType", filters.resourceType().name());
        }
        if (filters.favorite() != null) {
            where.append(" AND r.favorite = :fav ");
            params.addValue("fav", filters.favorite());
        }
        if (filters.createdAfter() != null) {
            where.append(" AND r.created_at >= :cAfter ");
            params.addValue("cAfter", filters.createdAfter());
        }
        if (filters.createdBefore() != null) {
            where.append(" AND r.created_at <= :cBefore ");
            params.addValue("cBefore", filters.createdBefore());
        }
        if (filters.collectionIds() != null && !filters.collectionIds().isEmpty()) {
            where.append(" AND EXISTS (SELECT 1 FROM resource_collections rc WHERE rc.resource_id = r.id AND rc.collection_id IN (:filterColls)) ");
            params.addValue("filterColls", filters.collectionIds());
        }
        if (filters.tagIds() != null && !filters.tagIds().isEmpty()) {
            where.append(" AND EXISTS (SELECT 1 FROM resource_tags rt WHERE rt.resource_id = r.id AND rt.tag_id IN (:filterTags)) ");
            params.addValue("filterTags", filters.tagIds());
        }

        if (operation == QueryOperation.COUNT) {
            String sql = "SELECT COUNT(*) FROM resources r " + where;
            Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
            long total = count != null ? count : 0L;

            String desc = describeFilters(filters);
            String response = "You have " + total + (desc.isBlank() ? " resource" : " " + desc + " resource") + (total == 1 ? "" : "s") + " in KnowledgeOS.";
            return new StructuredResult(response, total, List.of());
        } else {
            String sql = "SELECT r.title FROM resources r " + where + " ORDER BY r.created_at DESC LIMIT 50";
            List<String> titles = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("title"));
            String response = "Found " + titles.size() + " matching resource(s):\n" +
                    String.join("\n", titles.stream().map(t -> "- " + t).toList());
            return new StructuredResult(response, titles.size(), titles);
        }
    }

    private String describeFilters(KnowledgeQueryFilters filters) {
        StringBuilder sb = new StringBuilder();
        if (filters.favorite() != null && filters.favorite()) {
            sb.append("favorite ");
        }
        if (filters.resourceType() != null) {
            sb.append(filters.resourceType().name()).append(" ");
        }
        return sb.toString().trim();
    }
}
