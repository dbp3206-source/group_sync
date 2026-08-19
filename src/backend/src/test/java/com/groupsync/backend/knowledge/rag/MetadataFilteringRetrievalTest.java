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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.groupsync.backend.knowledge.model.ResourceType;

@ExtendWith(MockitoExtension.class)
class MetadataFilteringRetrievalTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private EmbeddingProvider embeddingProvider;

    private SemanticRetrievalRepository semanticRepo;
    private SemanticRetrievalStrategy semanticStrategy;
    private KeywordRetrievalStrategy keywordStrategy;
    private GeminiProperties properties;

    @BeforeEach
    void setUp() {
        semanticRepo = new SemanticRetrievalRepository(jdbcTemplate);
        properties = new GeminiProperties("key", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-embedding-001", 768, 5, 2, 12, 60, 30000);
        semanticStrategy = new SemanticRetrievalStrategy(embeddingProvider, semanticRepo, properties);
        keywordStrategy = new KeywordRetrievalStrategy(jdbcTemplate);
    }

    @Test
    void semanticRetrieval_buildsCorrectSqlWithMetadataFilters() {
        when(embeddingProvider.embedQuery(anyString())).thenReturn(new float[768]);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    MapSqlParameterSource params = invocation.getArgument(1);

                    assertTrue(sql.contains("r.resource_type = :filterResourceType"), "SQL must filter by resourceType");
                    assertTrue(sql.contains("r.favorite = :filterFavorite"), "SQL must filter by favorite");
                    assertTrue(sql.contains("dc.chunk_level = 'CHILD'"), "SQL must restrict to child chunks");

                    assertEquals("PDF", params.getValue("filterResourceType"));
                    assertEquals(true, params.getValue("filterFavorite"));

                    return List.of(new RetrievedChunk(1L, 10L, "Sample PDF", 1, 1, "Intro", "Vector Content", 0.1d));
                });

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(
                null, null, null, ResourceType.PDF, true, null, null
        );

        List<RetrievedChunk> results = semanticStrategy.retrieve(
                1L, "What is HNSW?", RetrievalScope.LIBRARY, null, List.of(), null, filters, 5
        );

        assertEquals(1, results.size());
        assertEquals("Sample PDF", results.get(0).resourceTitle());
    }

    @Test
    void keywordRetrieval_buildsCorrectSqlWithMetadataFilters() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    MapSqlParameterSource params = invocation.getArgument(1);

                    assertTrue(sql.contains("r.resource_type = :filterResourceType"), "SQL must filter by resourceType");
                    assertTrue(sql.contains("EXISTS (SELECT 1 FROM resource_collections rc"), "SQL must filter by collections");
                    assertTrue(sql.contains("dc.chunk_level = 'CHILD'"), "SQL must restrict to child chunks");

                    assertEquals("MARKDOWN", params.getValue("filterResourceType"));
                    assertEquals(Set.of(5L), params.getValue("filterCollectionIds"));

                    return List.of(new RetrievedChunk(2L, 20L, "Sample Markdown", 1, null, "Notes", "Lexical Content", 0.05d));
                });

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(
                null, Set.of(5L), null, ResourceType.MARKDOWN, null, null, null
        );

        List<RetrievedChunk> results = keywordStrategy.retrieve(
                1L, "CVE-2026-8819", RetrievalScope.LIBRARY, null, List.of(), null, filters, 6
        );

        assertEquals(1, results.size());
        assertEquals("Sample Markdown", results.get(0).resourceTitle());
    }
}
