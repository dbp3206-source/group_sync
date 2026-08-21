package com.groupsync.backend.knowledge.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.service.RepresentativeEvidenceSelector.EvidenceChunk;
import com.groupsync.backend.shared.exception.NotFoundException;

@Service
public class DocumentUnderstandingTransactionService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DocumentUnderstandingTransactionService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public record UnderstandingSource(Long resourceId, Long ownerId, String title, String originalFilename,
                                      String sourceChecksum, int chunkingVersion, List<EvidenceChunk> chunks) { }

    public record StoredUnderstanding(Long id, String normalizedTitle, String summary, String keyIdeasJson,
                                      String candidateTagsJson, String broadThemesJson, String difficultyLevel,
                                      List<Long> evidenceChunkIds, Instant updatedAt) { }

    public record WorkspaceUnderstanding(String status, String normalizedTitle, String summary,
                                         String keyIdeasJson, String broadThemesJson, int evidenceCount,
                                         Instant updatedAt) { }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<WorkspaceUnderstanding> readForWorkspace(Long ownerId, Long resourceId) {
        List<WorkspaceUnderstanding> rows = jdbc.query("""
                select du.status,du.normalized_title,du.summary,
                       du.key_ideas_json::text,du.broad_themes_json::text,
                       (select count(*) from document_understanding_evidence due
                        where due.understanding_id=du.id) as evidence_count,
                       du.updated_at
                from document_understandings du join resources r on r.id=du.resource_id
                where du.resource_id=:resource and r.owner_id=:owner
                order by du.updated_at desc, du.id desc
                limit 1
                """, Map.of("resource", resourceId, "owner", ownerId), (rs, rowNum) ->
                new WorkspaceUnderstanding(rs.getString("status"), rs.getString("normalized_title"),
                        rs.getString("summary"), rs.getString("key_ideas_json"),
                        rs.getString("broad_themes_json"), rs.getInt("evidence_count"),
                        toInstant(rs.getTimestamp("updated_at"))));
        return rows.stream().findFirst();
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public UnderstandingSource readSource(Long ownerId, Long resourceId) {
        Map<String, Object> resource = jdbc.query("""
                select r.id, r.owner_id, r.title, r.original_filename, r.checksum_sha256,
                       coalesce(max(dc.chunking_version), 1) as chunking_version
                from resources r
                left join document_chunks dc on dc.resource_id = r.id
                where r.id=:resource and r.owner_id=:owner and r.processing_status='READY'
                group by r.id, r.owner_id, r.title, r.original_filename, r.checksum_sha256
                """, Map.of("resource", resourceId, "owner", ownerId), rs -> {
            if (!rs.next()) return null;
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("owner", rs.getLong("owner_id"));
            row.put("title", rs.getString("title"));
            row.put("filename", rs.getString("original_filename"));
            row.put("checksum", rs.getString("checksum_sha256"));
            row.put("chunking", rs.getInt("chunking_version"));
            return row;
        });
        if (resource == null) throw new NotFoundException("Ready resource not found.");
        List<EvidenceChunk> chunks = jdbc.query("""
                select dc.id, dc.chunk_index, dc.section, dc.content
                from document_chunks dc join resources r on r.id=dc.resource_id
                where dc.resource_id=:resource and r.owner_id=:owner and dc.chunk_level='CHILD'
                order by dc.chunk_index
                """, Map.of("resource", resourceId, "owner", ownerId), (rs, rowNum) ->
                new EvidenceChunk(rs.getLong("id"), rs.getInt("chunk_index"), rs.getString("section"), rs.getString("content")));
        return new UnderstandingSource(resourceId, ownerId, (String) resource.get("title"),
                (String) resource.get("filename"), (String) resource.get("checksum"),
                (Integer) resource.get("chunking"), chunks);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<StoredUnderstanding> findCurrent(Long ownerId, Long resourceId, String checksum,
                                                      int chunkingVersion, String model, String version) {
        List<StoredUnderstanding> rows = jdbc.query("""
                select du.id,du.normalized_title,du.summary,du.key_ideas_json::text,
                       du.candidate_tags_json::text,du.broad_themes_json::text,du.difficulty_level,du.updated_at
                from document_understandings du join resources r on r.id=du.resource_id
                where du.resource_id=:resource and r.owner_id=:owner and du.status='CURRENT'
                  and du.source_checksum=:checksum and du.chunking_version=:chunking
                  and du.model=:model and du.understanding_version=:version
                """, new MapSqlParameterSource()
                .addValue("resource", resourceId).addValue("owner", ownerId).addValue("checksum", checksum)
                .addValue("chunking", chunkingVersion).addValue("model", model).addValue("version", version),
                (rs, rowNum) -> new StoredUnderstanding(rs.getLong("id"), rs.getString("normalized_title"),
                        rs.getString("summary"), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString("difficulty_level"), List.of(), toInstant(rs.getTimestamp("updated_at"))));
        return rows.stream().findFirst().map(row -> new StoredUnderstanding(row.id(), row.normalizedTitle(),
                row.summary(), row.keyIdeasJson(), row.candidateTagsJson(), row.broadThemesJson(),
                row.difficultyLevel(), evidenceIds(row.id()), row.updatedAt()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveCurrent(UnderstandingSource source, String checksum, String model, String version,
                            DocumentUnderstandingResult result) {
        jdbc.update("update document_understandings set status='STALE',updated_at=now() where resource_id=:resource and status='CURRENT'",
                Map.of("resource", source.resourceId()));
        MapSqlParameterSource params = baseParams(source, checksum, model, version)
                .addValue("title", result.normalizedTitle()).addValue("summary", result.summary())
                .addValue("ideas", json(result.keyIdeas())).addValue("tags", json(result.candidateTags()))
                .addValue("themes", json(result.broadThemes())).addValue("level", result.difficultyOrLevel());
        Long id = jdbc.queryForObject("""
                insert into document_understandings(resource_id,source_checksum,chunking_version,model,
                    understanding_version,normalized_title,summary,key_ideas_json,candidate_tags_json,
                    broad_themes_json,difficulty_level,status,created_at,updated_at)
                values(:resource,:checksum,:chunking,:model,:version,:title,:summary,cast(:ideas as jsonb),
                    cast(:tags as jsonb),cast(:themes as jsonb),:level,'CURRENT',now(),now()) returning id
                """, params, Long.class);
        for (Long chunkId : result.evidenceChunkIds()) {
            jdbc.update("insert into document_understanding_evidence(understanding_id,document_chunk_id,item_key) values(:understanding,:chunk,'document') on conflict do nothing",
                    Map.of("understanding", Objects.requireNonNull(id), "chunk", chunkId));
        }
        return Objects.requireNonNull(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UnderstandingSource source, String checksum, String model, String version,
                              String status, String reason) {
        jdbc.update("update document_understandings set status='STALE',updated_at=now() where resource_id=:resource and status='CURRENT'",
                Map.of("resource", source.resourceId()));
        MapSqlParameterSource params = baseParams(source, checksum, model, version)
                .addValue("title", source.title()).addValue("status", status)
                .addValue("reason", abbreviate(reason));
        jdbc.update("""
                insert into document_understandings(resource_id,source_checksum,chunking_version,model,
                    understanding_version,normalized_title,key_ideas_json,candidate_tags_json,broad_themes_json,
                    status,failure_reason,created_at,updated_at)
                values(:resource,:checksum,:chunking,:model,:version,:title,'[]'::jsonb,'[]'::jsonb,'[]'::jsonb,
                    :status,:reason,now(),now())
                """, params);
    }

    private List<Long> evidenceIds(Long understandingId) {
        return jdbc.queryForList("select document_chunk_id from document_understanding_evidence where understanding_id=:id order by document_chunk_id",
                Map.of("id", understandingId), Long.class);
    }

    private MapSqlParameterSource baseParams(UnderstandingSource source, String checksum, String model, String version) {
        return new MapSqlParameterSource().addValue("resource", source.resourceId()).addValue("checksum", checksum)
                .addValue("chunking", source.chunkingVersion()).addValue("model", model).addValue("version", version);
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize document understanding.", e); }
    }

    private String abbreviate(String value) {
        String safe = value == null || value.isBlank() ? "Semantic understanding failed." : value;
        return safe.substring(0, Math.min(500, safe.length()));
    }

    private Instant toInstant(Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant(); }
}
