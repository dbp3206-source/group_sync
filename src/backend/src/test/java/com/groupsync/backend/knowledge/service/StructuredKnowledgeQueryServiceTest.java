package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.rag.KnowledgeQueryFilters;
import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.knowledge.rag.QueryOperation;
import com.groupsync.backend.knowledge.rag.QueryPlan;
import com.groupsync.backend.knowledge.rag.RetrievalScope;

@ExtendWith(MockitoExtension.class)
class StructuredKnowledgeQueryServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    private StructuredKnowledgeQueryService service;

    @BeforeEach
    void setUp() {
        service = new StructuredKnowledgeQueryService(jdbcTemplate);
    }

    @Test
    void execute_countQueryReturnsTruthfulAnswer() {
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(7L);

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(
                null, null, null, ResourceType.PDF, true, null, null
        );
        QueryPlan plan = new QueryPlan(QueryMode.STRUCTURED, QueryOperation.COUNT, null, filters, "Count favorite PDFs");

        StructuredKnowledgeQueryService.StructuredResult result = service.execute(
                1L, plan, RetrievalScope.LIBRARY, null, List.of(), null
        );

        assertEquals(7L, result.count());
        assertTrue(result.textResponse().contains("7"));
        assertTrue(result.textResponse().contains("PDF"));
    }

    @Test
    void execute_listQueryReturnsTitles() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of("Distributed Algorithms Guide", "Vector Indexing Notes"));

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(
                null, null, null, ResourceType.MARKDOWN, null, null, null
        );
        QueryPlan plan = new QueryPlan(QueryMode.STRUCTURED, QueryOperation.LIST, null, filters, "List markdown files");

        StructuredKnowledgeQueryService.StructuredResult result = service.execute(
                1L, plan, RetrievalScope.LIBRARY, null, List.of(), null
        );

        assertEquals(2, result.count());
        assertTrue(result.textResponse().contains("Distributed Algorithms Guide"));
        assertTrue(result.textResponse().contains("Vector Indexing Notes"));
    }
}
