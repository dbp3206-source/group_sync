package com.groupsync.backend.knowledge.service;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.FocusNextResponse;
import com.groupsync.backend.knowledge.dto.InsightOverviewResponse;

@Service
public class KnowledgeDashboardService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public KnowledgeDashboardService(NamedParameterJdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Optional<FocusNextResponse> focusNext(Long ownerId) {
        String sql = """
                SELECT r.id, r.title, r.resource_type, r.priority, r.favorite,
                       COALESCE(lp.progress_percent, 0) AS progress_percent
                FROM resources r
                LEFT JOIN learning_progress lp ON lp.resource_id = r.id AND lp.owner_id = r.owner_id
                WHERE r.owner_id = :ownerId AND r.processing_status = 'READY'
                ORDER BY r.favorite DESC, r.priority DESC,
                         CASE WHEN COALESCE(lp.progress_percent, 0) BETWEEN 1 AND 99 THEN 0 ELSE 1 END,
                         r.updated_at DESC
                LIMIT 1
                """;
        List<FocusNextResponse> results = jdbcTemplate.query(sql, new MapSqlParameterSource("ownerId", ownerId),
                (row, index) -> {
                    int progress = row.getInt("progress_percent");
                    String reason = progress > 0 && progress < 100 ? "Continue where you left off"
                            : row.getBoolean("favorite") ? "A saved favorite" : "Highest priority ready resource";
                    return new FocusNextResponse(row.getLong("id"), row.getString("title"), row.getString("resource_type"),
                            row.getInt("priority"), row.getBoolean("favorite"), progress, reason);
                });
        return results.stream().findFirst();
    }

    public InsightOverviewResponse overview(Long ownerId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("ownerId", ownerId);
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM resources WHERE owner_id = :ownerId", parameters, Long.class);
        Long ready = jdbcTemplate.queryForObject("SELECT count(*) FROM resources WHERE owner_id = :ownerId AND processing_status = 'READY'", parameters, Long.class);
        Long inProgress = jdbcTemplate.queryForObject("SELECT count(*) FROM learning_progress WHERE owner_id = :ownerId AND progress_percent BETWEEN 1 AND 99", parameters, Long.class);
        Long completed = jdbcTemplate.queryForObject("SELECT count(*) FROM learning_progress WHERE owner_id = :ownerId AND progress_percent = 100", parameters, Long.class);
        List<InsightOverviewResponse.InsightTopicCount> composition = jdbcTemplate.query("""
                SELECT resource_type, count(*) AS count FROM resources WHERE owner_id = :ownerId
                GROUP BY resource_type ORDER BY count DESC, resource_type
                """, parameters, (row, index) -> new InsightOverviewResponse.InsightTopicCount(row.getString("resource_type"), row.getLong("count")));
        return new InsightOverviewResponse(total == null ? 0 : total, ready == null ? 0 : ready,
                inProgress == null ? 0 : inProgress, completed == null ? 0 : completed, composition);
    }
}
