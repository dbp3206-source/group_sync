package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.groupsync.backend.knowledge.dto.RecentActivityResponse;

@ExtendWith(MockitoExtension.class)
class KnowledgeDashboardServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    private KnowledgeDashboardService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeDashboardService(jdbcTemplate);
    }

    @Test
    void recentActivityMergesRealSourcesNewestFirst() {
        RecentActivityResponse resource = activity("RESOURCE_OPENED", "Database Systems", "2026-08-20T10:00:00Z", "/library/7", "Mở không gian tài liệu");
        RecentActivityResponse ask = activity("ASK_ACTIVITY", "Database questions", "2026-08-20T11:00:00Z", "/ask?session=9", "Đặt câu hỏi trong Ask");
        RecentActivityResponse focus = activity("FOCUS_ACTIVITY", "SQL revision", "2026-08-20T09:00:00Z", "/focus", "Làm việc trong Focus");
        RecentActivityResponse recall = activity("RECALL_ACTIVITY", "SQL revision", "2026-08-20T08:00:00Z", "/focus", "Thực hiện một lượt Recall");
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(resource, ask, focus, recall));

        List<RecentActivityResponse> result = service.recentActivity(42L);

        assertEquals(List.of(ask, resource, focus, recall), result);
        assertEquals("/ask?session=9", result.get(0).resumeUrl());
        assertEquals("/library/7", result.get(1).resumeUrl());
        verify(jdbcTemplate).query(contains("lp.last_opened_at IS NOT NULL"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void recentActivityReturnsEmptyForFirstTimeUser() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        assertTrue(service.recentActivity(42L).isEmpty());
    }

    @Test
    void projectionScopesEverySourceToTheOwnerAndDoesNotUseResourceCreationAsOpen() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        service.recentActivity(42L);

        var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertEquals(42L, paramsCaptor.getValue().getValue("ownerId"));
        assertTrue(sql.contains("cs.owner_id = :ownerId"));
        assertTrue(sql.contains("st.owner_id = :ownerId"));
        assertTrue(sql.contains("qa.owner_id = :ownerId"));
        assertFalse(sql.contains("r.created_at AS occurred_at"));
    }

    private RecentActivityResponse activity(String type, String title, String occurredAt, String resumeUrl, String context) {
        return new RecentActivityResponse(type, title, Instant.parse(occurredAt), resumeUrl, context);
    }
}
