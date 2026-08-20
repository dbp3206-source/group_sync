package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class KnowledgeWorkspaceServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbc;
    private KnowledgeWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeWorkspaceService(jdbc);
    }

    @Test
    void openingResourceUpdatesLastOpenedAtAndPreservesProgress() {
        ResourceActivityResponse activity = new ResourceActivityResponse(
                "READY", 63, 2, java.time.Instant.parse("2026-08-19T10:00:00Z"),
                java.time.Instant.parse("2026-08-20T10:00:00Z"), java.time.Instant.parse("2026-08-20T10:00:00Z"));
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class))).thenReturn(activity);

        ResourceActivityResponse result = service.recordResourceOpened(7L, 11L);

        assertEquals(63, result.progressPercent());
        assertNotNull(result.lastOpenedAt());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), anyMap());
        assertTrue(sqlCaptor.getValue().contains("last_opened_at=excluded.last_opened_at"));
        assertTrue(sqlCaptor.getValue().contains("progress_percent"));
    }

    @Test
    void onlyOwnerCanRecordResourceOpen() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(0);

        assertThrows(NotFoundException.class, () -> service.recordResourceOpened(99L, 11L));
        verify(jdbc, never()).update(anyString(), anyMap());
    }

    @Test
    void createCollectionAcceptsBlankOptionalDescriptionWithoutNullParameterFailure() {
        CollectionResponse response = new CollectionResponse(4L, "Reading", null, null, null, 0L);
        when(jdbc.queryForObject(contains("select count(*)"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(contains("resource_count"), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(response);

        assertEquals(response, service.createCollection(7L, " Reading ", "   "));

        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(contains("insert into collections"), params.capture());
        assertNull(params.getValue().getValue("description"));
    }

    @Test
    void updateCollectionAcceptsNullBlankAndTextDescriptions() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(contains("select count(*)"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(contains("resource_count"), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new CollectionResponse(4L, "Reading", "notes", null, null, 2L));

        service.updateCollection(7L, 4L, "Reading", null);
        service.updateCollection(7L, 4L, "Reading", "   ");
        service.updateCollection(7L, 4L, "Reading", "notes");

        verify(jdbc, times(3)).update(contains("update collections"), any(SqlParameterSource.class));
    }

    @Test
    void findOrCreateCollectionUsesNullSafeParametersForBlankDescription() {
        when(jdbc.queryForObject(contains("resource_count"), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new CollectionResponse(4L, "Reading", null, null, null, 0L));

        assertDoesNotThrow(() -> service.findOrCreateCollection(7L, "Reading", ""));
        verify(jdbc).update(contains("on conflict(owner_id,name)"), any(SqlParameterSource.class));
    }

    @Test
    void duplicateCollectionIsReportedAsConflict() {
        when(jdbc.queryForObject(contains("select count(*)"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(1);

        assertThrows(ConflictException.class, () -> service.createCollection(7L, "Reading", null));
        verify(jdbc, never()).update(contains("insert into collections"), any(SqlParameterSource.class));
    }

    @Test
    void bulkAssignIsIdempotentAndValidatesAllResourcesBeforeWriting() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(1);
        when(jdbc.update(contains("insert into resource_collections"), anyMap())).thenReturn(1);

        BulkOperationResponse result = service.assignResources(7L, 4L, List.of(11L, 12L));

        assertEquals(new BulkOperationResponse(2, 2), result);
        verify(jdbc, times(2)).update(contains("on conflict do nothing"), anyMap());
    }

    @Test
    void bulkAssignRejectsCrossOwnerResourceWithoutPartialWrites() {
        when(jdbc.queryForObject(contains("collections"), anyMap(), eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(contains("resources"), anyMap(), eq(Integer.class))).thenReturn(1, 0);

        assertThrows(NotFoundException.class, () -> service.assignResources(7L, 4L, List.of(11L, 12L)));
        verify(jdbc, never()).update(contains("resource_collections"), anyMap());
    }

    @Test
    void bulkAssignRejectsEmptySelection() {
        assertThrows(IllegalArgumentException.class, () -> service.assignResources(7L, 4L, List.of()));
        verifyNoInteractions(jdbc);
    }
}
