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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;

@ExtendWith(MockitoExtension.class)
class ParentChildRetrievalTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    private ParentChildContextExpander expander;

    @BeforeEach
    void setUp() {
        expander = new ParentChildContextExpander(jdbcTemplate, 5, 2000);
    }

    @Test
    void expand_deduplicatesMultipleChildrenBelongingToSameParent() {
        RetrievedChunk child1 = new RetrievedChunk(101L, 10L, "Doc A", 1, 1, "Section 1", "Child 1 excerpt", 0.1d);
        RetrievedChunk child2 = new RetrievedChunk(102L, 10L, "Doc A", 2, 1, "Section 1", "Child 2 excerpt", 0.2d);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(2);
            java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
            when(rs1.getLong("child_id")).thenReturn(101L);
            when(rs1.getLong("parent_id")).thenReturn(100L);
            when(rs1.getLong("resource_id")).thenReturn(10L);
            when(rs1.getString("title")).thenReturn("Doc A");
            when(rs1.getInt("chunk_index")).thenReturn(0);
            when(rs1.getObject("page_number", Integer.class)).thenReturn(1);
            when(rs1.getString("section")).thenReturn("Section 1");
            when(rs1.getString("content")).thenReturn("Enclosing parent content combining section 1.");
            rch.processRow(rs1);

            java.sql.ResultSet rs2 = mock(java.sql.ResultSet.class);
            when(rs2.getLong("child_id")).thenReturn(102L);
            when(rs2.getLong("parent_id")).thenReturn(100L);
            when(rs2.getLong("resource_id")).thenReturn(10L);
            when(rs2.getString("title")).thenReturn("Doc A");
            when(rs2.getInt("chunk_index")).thenReturn(0);
            when(rs2.getObject("page_number", Integer.class)).thenReturn(1);
            when(rs2.getString("section")).thenReturn("Section 1");
            when(rs2.getString("content")).thenReturn("Enclosing parent content combining section 1.");
            rch.processRow(rs2);

            return null;
        }).when(jdbcTemplate).query(anyString(), any(MapSqlParameterSource.class), any(RowCallbackHandler.class));

        ExpandedContext result = expander.expand(List.of(child1, child2));

        // Must produce 1 deduplicated parent chunk for prompt context, with best child distance (0.1d)
        assertEquals(1, result.promptContextChunks().size(), "Multiple children under same parent must yield 1 parent");
        assertEquals(100L, result.promptContextChunks().get(0).chunkId());
        assertEquals(0.1d, result.promptContextChunks().get(0).distance());

        // Must preserve both child chunks for citation evidence
        assertEquals(2, result.citationEvidenceChunks().size(), "Both child chunks must be kept for citation evidence");
    }

    @Test
    void expand_respectsContextCharacterBudget() {
        ParentChildContextExpander smallBudgetExpander = new ParentChildContextExpander(jdbcTemplate, 5, 100);

        RetrievedChunk child1 = new RetrievedChunk(1L, 10L, "Doc A", 1, 1, "Sec 1", "child 1", 0.1d);
        RetrievedChunk child2 = new RetrievedChunk(2L, 10L, "Doc A", 2, 1, "Sec 2", "child 2", 0.2d);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(2);
            java.sql.ResultSet rs1 = mock(java.sql.ResultSet.class);
            when(rs1.getLong("child_id")).thenReturn(1L);
            when(rs1.getLong("parent_id")).thenReturn(10L);
            when(rs1.getLong("resource_id")).thenReturn(10L);
            when(rs1.getString("title")).thenReturn("Doc A");
            when(rs1.getInt("chunk_index")).thenReturn(0);
            when(rs1.getObject("page_number", Integer.class)).thenReturn(1);
            when(rs1.getString("section")).thenReturn("Sec 1");
            when(rs1.getString("content")).thenReturn("This is an extensive parent context paragraph that is intentionally longer than eighty chars.".repeat(2));
            rch.processRow(rs1);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(MapSqlParameterSource.class), any(RowCallbackHandler.class));

        ExpandedContext result = smallBudgetExpander.expand(List.of(child1, child2));

        assertEquals(1, result.promptContextChunks().size(), "Budget must limit to 1 parent chunk when character cap is exceeded");
    }
}
