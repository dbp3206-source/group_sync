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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.model.ResourceType;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeQueryPlannerTest {

    @Mock private LanguageModelClient languageModelClient;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    private QueryPlanValidator validator;
    private KnowledgeQueryPlanner planner;

    @BeforeEach
    void setUp() {
        validator = new QueryPlanValidator(jdbcTemplate);
        planner = new KnowledgeQueryPlanner(languageModelClient, validator, jdbcTemplate);
    }

    @Test
    void plan_classifiesSemanticQueryCorrectly() {
        String json = """
                ```json
                {
                  "mode": "HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "how does cosine similarity work",
                  "resourceType": null,
                  "collectionName": null,
                  "tagName": null,
                  "favorite": null,
                  "explanation": "General technical question"
                }
                ```
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "How does cosine similarity work?", RetrievalScope.LIBRARY, null, List.of(), null);

        assertEquals(QueryMode.HYBRID, plan.mode());
        assertEquals(QueryOperation.SEARCH, plan.operation());
        assertEquals("how does cosine similarity work", plan.semanticQuery());
        assertTrue(plan.filters().isEmpty());
        assertFalse(plan.filters().impossible());
    }

    @Test
    void plan_classifiesStructuredCountQueryCorrectly() {
        String json = """
                {
                  "mode": "STRUCTURED",
                  "operation": "COUNT",
                  "semanticQuery": null,
                  "resourceType": "PDF",
                  "collectionName": null,
                  "tagName": null,
                  "favorite": true,
                  "explanation": "Counting favorite PDF files"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "How many favorite PDFs do I have?", RetrievalScope.LIBRARY, null, List.of(), null);

        assertEquals(QueryMode.STRUCTURED, plan.mode());
        assertEquals(QueryOperation.COUNT, plan.operation());
        assertEquals(ResourceType.PDF, plan.filters().resourceType());
        assertEquals(true, plan.filters().favorite());
        assertFalse(plan.filters().impossible());
    }

    @Test
    void plan_fallsBackSafelyOnInvalidJson() {
        when(languageModelClient.answer(anyString())).thenReturn("Not a valid JSON response from model.");

        QueryPlan plan = planner.plan(1L, "What is HNSW?", RetrievalScope.LIBRARY, null, List.of(), null);

        assertEquals(QueryMode.HYBRID, plan.mode());
        assertEquals(QueryOperation.SEARCH, plan.operation());
        assertEquals("What is HNSW?", plan.semanticQuery());
        assertTrue(plan.filters().isEmpty());
    }

    @Test
    void plan_unknownCollectionName_producesImpossibleFilter() {
        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "quantum computing notes",
                  "collectionName": "NON_EXISTENT_COURSE",
                  "explanation": "Search within specific collection"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find quantum computing notes in non existent course", RetrievalScope.LIBRARY, null, List.of(), null);

        assertTrue(plan.filters().impossible(), "Explicit unresolvable collection name must produce impossible filter, not search whole library");
        assertEquals(Set.of(-1L), plan.filters().collectionIds());
    }

    @Test
    void plan_unknownTagName_producesImpossibleFilter() {
        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "security vulnerabilities",
                  "tagName": "UNKNOWN_TAG",
                  "explanation": "Search with tag"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find vulnerabilities with tag UNKNOWN_TAG", RetrievalScope.LIBRARY, null, List.of(), null);

        assertTrue(plan.filters().impossible(), "Explicit unresolvable tag name must produce impossible filter");
        assertEquals(Set.of(-1L), plan.filters().tagIds());
    }

    @Test
    void plan_validCollectionName_resolvesCorrectly() {
        when(jdbcTemplate.query(eq("SELECT id, name FROM collections WHERE owner_id = :ownerId"), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(java.util.Map.entry("ai agent", 42L)));

        when(jdbcTemplate.query(eq("SELECT id FROM collections WHERE owner_id = :ownerId AND id IN (:ids)"), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(42L));

        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "agentic patterns",
                  "collectionName": "AI Agent",
                  "explanation": "Search inside AI Agent collection"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find agentic patterns in AI Agent", RetrievalScope.LIBRARY, null, List.of(), null);

        assertFalse(plan.filters().impossible());
        assertEquals(Set.of(42L), plan.filters().collectionIds());
    }

    @Test
    void plan_validTagName_resolvesCorrectly() {
        when(jdbcTemplate.query(eq("SELECT id, name FROM tags WHERE owner_id = :ownerId"), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(java.util.Map.entry("rag", 88L)));

        when(jdbcTemplate.query(eq("SELECT id FROM tags WHERE owner_id = :ownerId AND id IN (:ids)"), any(MapSqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of(88L));

        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "hybrid search",
                  "tagName": "RAG",
                  "explanation": "Search with RAG tag"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find hybrid search notes with tag RAG", RetrievalScope.LIBRARY, null, List.of(), null);

        assertFalse(plan.filters().impossible());
        assertEquals(Set.of(88L), plan.filters().tagIds());
    }

    @Test
    void plan_fieldAbsent_doesNotMarkImpossible() {
        String json = """
                {
                  "mode": "HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "all documents",
                  "explanation": "Unrestricted search"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Show all documents", RetrievalScope.LIBRARY, null, List.of(), null);

        assertFalse(plan.filters().impossible(), "Absent collectionName / tagName fields must not produce impossible filter");
        assertNull(plan.filters().collectionIds());
        assertNull(plan.filters().tagIds());
    }

    @Test
    void plan_invalidResourceType_producesImpossibleFilter() {
        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "slides",
                  "resourceType": "POWERPOINT_ULTRA",
                  "explanation": "Search for unsupported resource type"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find slides in powerpoint format", RetrievalScope.LIBRARY, null, List.of(), null);

        assertTrue(plan.filters().impossible(), "Explicit unsupported resourceType must produce impossible filter, not unrestricted query");
    }

    @Test
    void plan_invalidDate_producesImpossibleFilter() {
        String json = """
                {
                  "mode": "FILTERED_HYBRID",
                  "operation": "SEARCH",
                  "semanticQuery": "recent notes",
                  "createdAfter": "not-a-valid-date",
                  "explanation": "Search with malformed date"
                }
                """;
        when(languageModelClient.answer(anyString())).thenReturn(json);

        QueryPlan plan = planner.plan(1L, "Find recent notes", RetrievalScope.LIBRARY, null, List.of(), null);

        assertTrue(plan.filters().impossible(), "Explicit malformed createdAfter date must produce impossible filter");
    }
}
