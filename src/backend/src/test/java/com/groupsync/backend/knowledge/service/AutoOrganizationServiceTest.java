package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.service.DocumentUnderstandingService.Outcome;
import com.groupsync.backend.knowledge.service.SemanticCollectionOrganizationService.CollectionPlan;
import com.groupsync.backend.knowledge.service.SemanticTaggingService.TagDecision;

class AutoOrganizationServiceTest {
    private NamedParameterJdbcTemplate jdbc;
    private DocumentUnderstandingService understanding;
    private SemanticTaggingService tagging;
    private SemanticCollectionOrganizationService collections;
    private KnowledgeWorkspaceService workspace;
    private AutoOrganizationService service;

    @BeforeEach void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class); understanding = mock(DocumentUnderstandingService.class);
        tagging = mock(SemanticTaggingService.class); collections = mock(SemanticCollectionOrganizationService.class);
        workspace = mock(KnowledgeWorkspaceService.class);
        service = new AutoOrganizationService(jdbc, understanding, tagging, collections, workspace);
    }

    @Test void highConfidenceAssignmentsAreAdditiveAndManualMembershipIsNeverRemoved() {
        DocumentUnderstandingResult du = du();
        when(understanding.understand(1L,10L)).thenReturn(new Outcome("CURRENT",du,false,List.of()));
        when(tagging.plan(1L,du)).thenReturn(List.of(new TagDecision("RAG",7L,"RAG",1)));
        when(collections.plan(1L,du)).thenReturn(new CollectionPlan(
                List.of(new OrganizationCollectionSuggestionResponse("AI Engineering",8L,"Strong semantic match",.9)), List.of(), List.of()));
        when(workspace.tag(1L,7L)).thenReturn(new TagResponse(7L,"RAG",null));
        when(workspace.assignTagIfMissing(1L,10L,7L)).thenReturn(true);
        when(workspace.assignResourceIfMissing(1L,8L,10L)).thenReturn(true);
        var result = service.autoOrganize(1L,10L);
        assertEquals(List.of("RAG"),result.tagsAssigned());
        assertEquals(List.of(8L),result.collectionsAssigned());
        verify(workspace, never()).removeTag(anyLong(),anyLong(),anyLong());
        verify(workspace, never()).removeResource(anyLong(),anyLong(),anyLong());
    }

    @Test void lowConfidenceCollectionIsSuggestedNotAssigned() {
        DocumentUnderstandingResult du=du();
        when(understanding.understand(1L,10L)).thenReturn(new Outcome("CURRENT",du,false,List.of()));
        when(tagging.plan(1L,du)).thenReturn(List.of());
        var possible=new OrganizationCollectionSuggestionResponse("AI Engineering",8L,"Possible semantic match",.7);
        when(collections.plan(1L,du)).thenReturn(new CollectionPlan(List.of(),List.of(possible),List.of()));
        var result=service.autoOrganize(1L,10L);
        assertTrue(result.collectionsAssigned().isEmpty());
        assertEquals(List.of(possible),result.collectionSuggestions());
    }

    @Test void unsupportedUnderstandingSkipsWithoutMutation() {
        when(understanding.understand(1L,10L)).thenReturn(new Outcome("UNSUPPORTED",null,false,List.of("Not enough evidence")));
        var result=service.autoOrganize(1L,10L);
        assertEquals("UNSUPPORTED",result.understandingStatus());
        verifyNoInteractions(tagging,collections,workspace);
    }

    @Test void batchCountsAssignedSuggestedSkippedAndFailedTruthfully() {
        AutoOrganizationService observed = spy(service);
        when(jdbc.queryForList(anyString(), anyMap(), eq(Long.class))).thenReturn(List.of(10L, 11L, 12L, 13L));
        doReturn(result(10L, "CURRENT", List.of("RAG"), List.of(), List.of()))
                .when(observed).autoOrganize(1L, 10L);
        doReturn(result(11L, "CURRENT", List.of(), List.of(
                new OrganizationCollectionSuggestionResponse("AI Engineering", 8L, "Possible semantic match", .7)), List.of()))
                .when(observed).autoOrganize(1L, 11L);
        doReturn(result(12L, "UNSUPPORTED", List.of(), List.of(), List.of()))
                .when(observed).autoOrganize(1L, 12L);
        doReturn(result(13L, "FAILED", List.of(), List.of(), List.of()))
                .when(observed).autoOrganize(1L, 13L);

        OrganizationBatchResult batch = observed.autoOrganizeAll(1L);
        assertAll(
                () -> assertEquals(4, batch.processed()),
                () -> assertEquals(1, batch.assigned()),
                () -> assertEquals(1, batch.suggested()),
                () -> assertEquals(1, batch.skipped()),
                () -> assertEquals(1, batch.failed()));
    }

    private SemanticOrganizationResult result(Long resourceId, String status, List<String> tags,
                                               List<OrganizationCollectionSuggestionResponse> suggestions,
                                               List<OrganizationCollectionSuggestionResponse> newSuggestions) {
        return new SemanticOrganizationResult(resourceId, status, tags, List.of(), suggestions, newSuggestions, List.of());
    }

    private DocumentUnderstandingResult du(){ return new DocumentUnderstandingResult("Title","A factual semantic summary with enough detail.",List.of("Idea"),List.of("RAG"),List.of("AI Engineering"),null,List.of(1L)); }
}
