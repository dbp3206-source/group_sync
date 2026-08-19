package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.groupsync.backend.knowledge.dto.ApplyOrganizationRequest;
import com.groupsync.backend.knowledge.dto.CollectionResponse;
import com.groupsync.backend.knowledge.dto.OrganizationSuggestionsResponse;
import com.groupsync.backend.knowledge.dto.TagResponse;
import com.groupsync.backend.knowledge.rag.SemanticRetrievalService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.knowledge.service.OrganizationSuggestionService;

@ExtendWith(MockitoExtension.class)
class OrganizationSuggestionServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private KnowledgeWorkspaceService workspace;
    @Mock private SemanticRetrievalService retrieval;
    @InjectMocks private OrganizationSuggestionService organizationService;

    @Test
    void suggestionsReturnsTypedResponse() {
        Long ownerId = 1L;
        Long resourceId = 10L;

        when(jdbc.queryForObject(contains("count(*)"), anyMap(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForMap(contains("from resources"), anyMap())).thenReturn(
                Map.of("id", resourceId, "title", "Advanced RAG Architecture", "description", "Overview of vector search and embeddings")
        );
        when(jdbc.queryForObject(contains("string_agg"), anyMap(), eq(String.class))).thenReturn("gemini and pgvector retrieval");
        when(workspace.tags(ownerId)).thenReturn(List.of(new TagResponse(100L, "rag", null)));
        when(workspace.collections(ownerId)).thenReturn(List.of(new CollectionResponse(200L, "AI Engineering", null, null, null)));

        OrganizationSuggestionsResponse response = organizationService.suggestions(ownerId, resourceId);

        assertNotNull(response);
        assertEquals(resourceId, response.resourceId());
        assertFalse(response.suggestedTags().isEmpty());
        assertTrue(response.suggestedTags().stream().anyMatch(t -> t.name().equals("rag") && t.existingTagId().equals(100L)));
        assertFalse(response.suggestedCollections().isEmpty());
    }

    @Test
    void applyCreatesAndAssignsTagsCollectionsAndRelations() {
        Long ownerId = 1L;
        Long resourceId = 10L;

        when(jdbc.queryForObject(contains("count(*)"), anyMap(), eq(Integer.class))).thenReturn(1);
        when(workspace.createTag(ownerId, "retrieval")).thenReturn(new TagResponse(101L, "retrieval", null));
        when(workspace.createCollection(eq(ownerId), eq("New Topic"), anyString()))
                .thenReturn(new CollectionResponse(201L, "New Topic", "desc", null, null));

        ApplyOrganizationRequest request = new ApplyOrganizationRequest(
                List.of("retrieval"),
                List.of(300L),
                List.of("New Topic"),
                List.of(400L)
        );

        assertDoesNotThrow(() -> organizationService.apply(ownerId, resourceId, request));

        verify(workspace).createTag(ownerId, "retrieval");
        verify(workspace).assignTag(ownerId, resourceId, 101L);
        verify(workspace).createCollection(eq(ownerId), eq("New Topic"), anyString());
        verify(workspace).assignResource(ownerId, 201L, resourceId);
        verify(workspace).assignResource(ownerId, 300L, resourceId);
        verify(jdbc).update(contains("insert into resource_relations"), anyMap());
    }
}
