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

import com.groupsync.backend.knowledge.service.AutoOrganizationService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;

@ExtendWith(MockitoExtension.class)
class AutoOrganizationServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private KnowledgeWorkspaceService workspaceService;
    @InjectMocks private AutoOrganizationService autoOrganizationService;

    @Test
    void autoOrganizeAssignsResourceToExistingCollectionAndTagWithoutFailing() {
        Long ownerId = 1L;
        Long resourceId = 42L;

        when(jdbc.queryForList(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("title", "Database Normalization and BCNF", "description", "Study guide on SQL and functional dependency")));

        when(jdbc.queryForObject(anyString(), anyMap(), eq(String.class)))
                .thenReturn("Armstrong axioms, superkey, 3nf, bcnf decomposition");

        when(workspaceService.findOrCreateCollection(anyLong(), eq("Database Systems"), anyString()))
                .thenReturn(Map.of("id", 100L, "name", "Database Systems"));

        when(workspaceService.findOrCreateTag(anyLong(), anyString()))
                .thenReturn(Map.of("id", 200L, "name", "tag"));

        assertDoesNotThrow(() -> autoOrganizationService.autoOrganize(ownerId, resourceId));

        verify(workspaceService, atLeastOnce()).findOrCreateCollection(eq(ownerId), eq("Database Systems"), anyString());
        verify(workspaceService, atLeastOnce()).assignResource(eq(ownerId), eq(100L), eq(resourceId));
        verify(workspaceService, atLeastOnce()).assignTag(eq(ownerId), eq(resourceId), eq(200L));
    }
}
