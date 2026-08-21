package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.dto.TagResponse;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;

class SemanticTaggingServiceTest {
    private KnowledgeWorkspaceService workspace;
    private EmbeddingProvider embeddings;
    private SemanticTaggingService service;

    @BeforeEach void setUp() {
        workspace = mock(KnowledgeWorkspaceService.class);
        embeddings = mock(EmbeddingProvider.class);
        service = new SemanticTaggingService(workspace, embeddings);
    }

    @Test void ragReusesExistingLongForm() {
        when(workspace.tags(1L)).thenReturn(List.of(new TagResponse(7L, "Retrieval-Augmented Generation", null)));
        var result = service.plan(1L, understanding("RAG"));
        assertEquals(7L, result.getFirst().existingTagId());
        verifyNoInteractions(embeddings);
    }

    @Test void oopReusesExistingLongForm() {
        when(workspace.tags(1L)).thenReturn(List.of(new TagResponse(8L, "Object Oriented Programming", null)));
        assertEquals(8L, service.plan(1L, understanding("OOP")).getFirst().existingTagId());
    }

    @Test void postgresReusesExistingPostgresql() {
        when(workspace.tags(1L)).thenReturn(List.of(new TagResponse(9L, "PostgreSQL", null)));
        assertEquals(9L, service.plan(1L, understanding("Postgres")).getFirst().existingTagId());
    }

    @Test void relatedButNotEquivalentTagIsNotForced() {
        when(workspace.tags(1L)).thenReturn(List.of(new TagResponse(10L, "Semantic Search", null)));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(vector(1, 0), vector(.75f, .66f)));
        assertNull(service.plan(1L, understanding("RAG")).getFirst().existingTagId());
    }

    @Test void unrelatedTagIsNotReused() {
        when(workspace.tags(1L)).thenReturn(List.of(new TagResponse(11L, "Cardiology", null)));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(vector(1, 0), vector(0, 1)));
        assertNull(service.plan(1L, understanding("RAG")).getFirst().existingTagId());
    }

    @Test void canonicalizationNeverReadsAnotherOwnerTags() {
        when(workspace.tags(22L)).thenReturn(List.of());
        service.plan(22L, understanding("RAG"));
        verify(workspace).tags(22L);
        verify(workspace, never()).tags(argThat(owner -> owner != 22L));
    }

    private DocumentUnderstandingResult understanding(String... tags) {
        return new DocumentUnderstandingResult("Title", "A factual source-grounded summary with enough detail.",
                List.of("Idea"), List.of(tags), List.of("Theme"), null, List.of(1L));
    }
    private float[] vector(float a, float b) { return new float[]{a, b}; }
}
