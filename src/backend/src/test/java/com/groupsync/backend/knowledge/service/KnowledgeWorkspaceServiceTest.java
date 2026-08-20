package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.groupsync.backend.knowledge.dto.ResourceActivityResponse;
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
}
