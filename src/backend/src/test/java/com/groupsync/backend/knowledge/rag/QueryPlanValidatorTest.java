package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.model.ResourceType;

@ExtendWith(MockitoExtension.class)
class QueryPlanValidatorTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    private QueryPlanValidator validator;

    @BeforeEach
    void setUp() {
        validator = new QueryPlanValidator(jdbcTemplate);
    }

    @Test
    void validateAndSanitize_enforcesScopeContainment() {
        KnowledgeQueryFilters requested = new KnowledgeQueryFilters(
                Set.of(100L, 200L), Set.of(5L), null, ResourceType.PDF, null, null, null
        );
        QueryPlan plan = new QueryPlan(QueryMode.FILTERED_HYBRID, QueryOperation.SEARCH, "query", requested, "test");

        // When user selected THIS_RESOURCE (id = 100L), scope cannot widen to 200L
        QueryPlan sanitized = validator.validateAndSanitize(1L, plan, RetrievalScope.THIS_RESOURCE, 100L, List.of(), null);

        assertEquals(Set.of(100L), sanitized.filters().resourceIds());
        assertNull(sanitized.filters().collectionIds(), "Collection filter should not escape THIS_RESOURCE scope");
    }

    @Test
    void validateAndSanitize_verifiesCollectionOwnership() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(10L)); // only collection 10L belongs to user

        KnowledgeQueryFilters requested = new KnowledgeQueryFilters(
                null, Set.of(10L, 999L), null, null, null, null, null
        );
        QueryPlan plan = new QueryPlan(QueryMode.FILTERED_HYBRID, QueryOperation.SEARCH, "query", requested, "test");

        QueryPlan sanitized = validator.validateAndSanitize(1L, plan, RetrievalScope.LIBRARY, null, List.of(), null);

        assertEquals(Set.of(10L), sanitized.filters().collectionIds(), "Only owned collection 10L should be kept");
    }
}
