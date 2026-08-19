package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.model.ResourceType;

@ExtendWith(MockitoExtension.class)
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
}
