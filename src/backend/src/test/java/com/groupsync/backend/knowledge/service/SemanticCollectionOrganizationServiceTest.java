package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.dto.CollectionResponse;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;

class SemanticCollectionOrganizationServiceTest {
    private KnowledgeWorkspaceService workspace;
    private EmbeddingProvider embeddings;
    private SemanticCollectionOrganizationService service;

    @BeforeEach void setUp() {
        workspace = mock(KnowledgeWorkspaceService.class);
        embeddings = mock(EmbeddingProvider.class);
        service = new SemanticCollectionOrganizationService(workspace, embeddings);
    }

    @Test void bcnfSemanticallyMatchesExistingDatabaseCollectionWithoutKeywordRule() {
        when(workspace.collections(1L)).thenReturn(List.of(collection(10L, "Database Systems")));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(.9f,.1f), v(.9f,.1f)));
        var plan = service.plan(1L, understanding(List.of("Relational theory")));
        assertEquals(10L, plan.strongMatches().getFirst().existingCollectionId());
    }

    @Test void documentMayStronglyMatchTwoCollections() {
        when(workspace.collections(1L)).thenReturn(List.of(collection(10L,"AI Engineering"), collection(11L,"Database Systems")));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(.91f,.1f), v(.86f,.1f), v(.9f,.1f)));
        assertEquals(2, service.plan(1L, understanding(List.of("Vector Databases"))).strongMatches().size());
    }

    @Test void lowConfidenceProducesSuggestionNotAssignment() {
        when(workspace.collections(1L)).thenReturn(List.of(collection(10L,"Possible Topic")));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(.7f,.71f), v(.9f,.1f)));
        var plan = service.plan(1L, understanding(List.of("Broad Theme")));
        assertTrue(plan.strongMatches().isEmpty());
        assertEquals(1, plan.possibleMatches().size());
    }

    @Test void unrelatedDocumentHasNoForcedCollection() {
        when(workspace.collections(1L)).thenReturn(List.of(collection(10L,"Cardiology")));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(0,1)));
        var plan = service.plan(1L, understanding(List.of()));
        assertTrue(plan.strongMatches().isEmpty());
        assertTrue(plan.possibleMatches().isEmpty());
        assertTrue(plan.newSuggestions().isEmpty());
    }

    @Test void existingStrongCollectionSuppressesInventedCollection() {
        when(workspace.collections(1L)).thenReturn(List.of(collection(10L,"Database Systems")));
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(.95f,.1f), v(.99f,.01f)));
        var plan = service.plan(1L, understanding(List.of("Relational Database Theory")));
        assertFalse(plan.strongMatches().isEmpty());
        assertTrue(plan.newSuggestions().isEmpty());
    }

    @Test void newCollectionSuggestionMustBeBroadAndStrong() {
        when(workspace.collections(1L)).thenReturn(List.of());
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0), v(.95f,.05f)));
        var plan = service.plan(1L, understanding(List.of("Database Systems")));
        assertEquals("Database Systems", plan.newSuggestions().getFirst().name());
    }

    @Test void fileSpecificThemeIsRejected() {
        when(workspace.collections(1L)).thenReturn(List.of());
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0)));
        assertTrue(service.plan(1L, understanding(List.of("Chapter 6 SQL Lecture Notes"))).newSuggestions().isEmpty());
    }

    @Test void collectionLookupIsOwnerScoped() {
        when(workspace.collections(33L)).thenReturn(List.of());
        when(embeddings.embedSemanticTexts(anyList())).thenReturn(List.of(v(1,0)));
        service.plan(33L, understanding(List.of()));
        verify(workspace).collections(33L);
        verify(workspace, never()).collections(argThat(owner -> owner != 33L));
    }

    private CollectionResponse collection(Long id, String name) { return new CollectionResponse(id, name, "Description", null, null, 0); }
    private DocumentUnderstandingResult understanding(List<String> themes) {
        return new DocumentUnderstandingResult("BCNF normalization", "A semantic explanation of determinants, normalization and relational database design.",
                List.of("Functional dependencies", "BCNF"), List.of("Normalization", "SQL"), themes, null, List.of(1L));
    }
    private float[] v(float a, float b) { return new float[]{a,b}; }
}
