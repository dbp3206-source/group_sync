package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.groupsync.backend.knowledge.dto.RelatedResourceResponse;

class ResourceKnowledgeMapServiceTest {

    @Test
    void mapHasNoArbitraryLibraryEdgesAndIsBounded() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        KnowledgeWorkspaceService workspace = mock(KnowledgeWorkspaceService.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(String.class))).thenReturn("Current source");
        when(workspace.resourceTags(7L, 11L)).thenReturn(List.of());
        when(workspace.resourceCollections(7L, 11L)).thenReturn(List.of());
        when(workspace.related(7L, 11L)).thenReturn(LongStream.rangeClosed(101L, 112L)
                .mapToObj(id -> new RelatedResourceResponse(id, "Stored " + id, null, "PDF", "READY", "SUGGESTED_RELATED", null))
                .toList());

        var response = new ResourceKnowledgeMapService(jdbc, workspace).get(7L, 11L);

        assertEquals(9, response.nodes().size());
        assertEquals(8, response.edges().size());
        assertTrue(response.nodes().stream().noneMatch(node -> node.resourceId() != null && node.resourceId() == 99L));
        assertTrue(response.edges().stream().allMatch(edge -> "resource_relations".equals(edge.provenance())));
    }

    @Test
    void storedRelationReasonIsShownWithItsProvenance() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        KnowledgeWorkspaceService workspace = mock(KnowledgeWorkspaceService.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(String.class))).thenReturn("Current source");
        when(workspace.resourceTags(7L, 11L)).thenReturn(List.of());
        when(workspace.resourceCollections(7L, 11L)).thenReturn(List.of());
        when(workspace.related(7L, 11L)).thenReturn(List.of(
                new RelatedResourceResponse(12L, "Related source", null, "PDF", "READY", "SUGGESTED_RELATED", null)));

        var response = new ResourceKnowledgeMapService(jdbc, workspace).get(7L, 11L);

        assertEquals("Stored relation: SUGGESTED_RELATED", response.edges().get(0).reason());
        assertEquals("SEMANTICALLY_RELATED", response.edges().get(0).relationType());
    }
}
