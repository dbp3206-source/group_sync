package com.groupsync.backend.knowledge.rag;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validates untrusted QueryPlans produced by LLM planning.
 * Enforces ownership of all referenced entity IDs, validates enum boundaries,
 * and guarantees that planner filters never escape or widen the user-selected RetrievalScope.
 */
@Component
public class QueryPlanValidator {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanValidator.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QueryPlanValidator(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryPlan validateAndSanitize(Long ownerId, QueryPlan rawPlan, RetrievalScope scope,
                                         Long thisResourceId, List<Long> selectedResourceIds, Long collectionId) {
        if (rawPlan == null) {
            return QueryPlan.defaultHybrid("");
        }

        QueryMode mode = rawPlan.mode() != null ? rawPlan.mode() : QueryMode.HYBRID;
        QueryOperation operation = rawPlan.operation() != null ? rawPlan.operation() : QueryOperation.SEARCH;
        String semanticQuery = rawPlan.semanticQuery() != null ? rawPlan.semanticQuery().trim() : "";

        KnowledgeQueryFilters rawFilters = rawPlan.filters() != null ? rawPlan.filters() : KnowledgeQueryFilters.empty();

        // Validate and scope-constrain resource IDs
        Set<Long> sanitizedResourceIds = sanitizeResourceIds(ownerId, rawFilters.resourceIds(), scope, thisResourceId, selectedResourceIds);

        // Validate and scope-constrain collection IDs
        Set<Long> sanitizedCollectionIds = sanitizeCollectionIds(ownerId, rawFilters.collectionIds(), scope, collectionId);

        // Validate tag IDs
        Set<Long> sanitizedTagIds = sanitizeTagIds(ownerId, rawFilters.tagIds());

        KnowledgeQueryFilters sanitizedFilters = new KnowledgeQueryFilters(
                sanitizedResourceIds,
                sanitizedCollectionIds,
                sanitizedTagIds,
                rawFilters.resourceType(),
                rawFilters.favorite(),
                rawFilters.createdAfter(),
                rawFilters.createdBefore()
        );

        // If mode was FILTERED_HYBRID but no filters remain, convert to HYBRID
        if (mode == QueryMode.FILTERED_HYBRID && sanitizedFilters.isEmpty()) {
            mode = QueryMode.HYBRID;
        }

        return new QueryPlan(mode, operation, semanticQuery, sanitizedFilters, rawPlan.explanation());
    }

    private Set<Long> sanitizeResourceIds(Long ownerId, Set<Long> requested, RetrievalScope scope,
                                          Long thisResourceId, List<Long> selectedResourceIds) {
        if (scope == RetrievalScope.THIS_RESOURCE && thisResourceId != null) {
            return Set.of(thisResourceId);
        }

        if (scope == RetrievalScope.SELECTED_RESOURCES && selectedResourceIds != null && !selectedResourceIds.isEmpty()) {
            if (requested == null || requested.isEmpty()) {
                return new HashSet<>(selectedResourceIds);
            }
            Set<Long> intersection = new HashSet<>(requested);
            intersection.retainAll(selectedResourceIds);
            return intersection.isEmpty() ? new HashSet<>(selectedResourceIds) : intersection;
        }

        if (requested == null || requested.isEmpty()) {
            return null;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("ids", requested);
        List<Long> valid = jdbcTemplate.query(
                "SELECT id FROM resources WHERE owner_id = :ownerId AND id IN (:ids)",
                params,
                (rs, rowNum) -> rs.getLong("id")
        );
        return valid.isEmpty() ? null : new HashSet<>(valid);
    }

    private Set<Long> sanitizeCollectionIds(Long ownerId, Set<Long> requested, RetrievalScope scope, Long scopedCollectionId) {
        if (scope == RetrievalScope.COLLECTION && scopedCollectionId != null) {
            return Set.of(scopedCollectionId);
        }

        if (requested == null || requested.isEmpty()) {
            return null;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("ids", requested);
        List<Long> valid = jdbcTemplate.query(
                "SELECT id FROM collections WHERE owner_id = :ownerId AND id IN (:ids)",
                params,
                (rs, rowNum) -> rs.getLong("id")
        );
        return valid.isEmpty() ? null : new HashSet<>(valid);
    }

    private Set<Long> sanitizeTagIds(Long ownerId, Set<Long> requested) {
        if (requested == null || requested.isEmpty()) {
            return null;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerId", ownerId)
                .addValue("ids", requested);
        List<Long> valid = jdbcTemplate.query(
                "SELECT id FROM tags WHERE owner_id = :ownerId AND id IN (:ids)",
                params,
                (rs, rowNum) -> rs.getLong("id")
        );
        return valid.isEmpty() ? null : new HashSet<>(valid);
    }
}
