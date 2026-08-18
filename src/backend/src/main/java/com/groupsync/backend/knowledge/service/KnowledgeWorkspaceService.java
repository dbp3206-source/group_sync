package com.groupsync.backend.knowledge.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.shared.exception.NotFoundException;

/** Small query layer for workspace data that is intentionally stored in simple join tables. */
@Service
public class KnowledgeWorkspaceService {

    private final NamedParameterJdbcTemplate jdbc;

    public KnowledgeWorkspaceService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> collections(Long ownerId) {
        String sql = "select id, name, description, created_at, updated_at from collections where owner_id=:owner order by updated_at desc";
        return jdbc.query(sql, Map.of("owner", ownerId), (rs, rowNum) ->
                new CollectionResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                )
        );
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tags(Long ownerId) {
        String sql = "select id, name, created_at from tags where owner_id=:owner order by name";
        return jdbc.query(sql, Map.of("owner", ownerId), (rs, rowNum) ->
                new TagResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        toInstant(rs.getTimestamp("created_at"))
                )
        );
    }

    @Transactional
    public TagResponse createTag(Long ownerId, String name) {
        String normalized = normalizeTag(name);
        jdbc.update("insert into tags(owner_id,name,created_at) values(:owner,:name,now()) on conflict(owner_id,name) do nothing",
                Map.of("owner", ownerId, "name", normalized));
        return jdbc.queryForObject("select id,name,created_at from tags where owner_id=:owner and name=:name",
                Map.of("owner", ownerId, "name", normalized),
                (rs, rowNum) -> new TagResponse(rs.getLong("id"), rs.getString("name"), toInstant(rs.getTimestamp("created_at")))
        );
    }

    @Transactional
    public TagResponse updateTag(Long ownerId, Long id, String name) {
        requireTag(ownerId, id);
        String normalized = normalizeTag(name);
        jdbc.update("update tags set name=:name where id=:id and owner_id=:owner",
                Map.of("owner", ownerId, "id", id, "name", normalized));
        return jdbc.queryForObject("select id,name,created_at from tags where id=:id and owner_id=:owner",
                Map.of("owner", ownerId, "id", id),
                (rs, rowNum) -> new TagResponse(rs.getLong("id"), rs.getString("name"), toInstant(rs.getTimestamp("created_at")))
        );
    }

    @Transactional
    public void deleteTag(Long ownerId, Long id) {
        requireTag(ownerId, id);
        jdbc.update("delete from tags where id=:id", Map.of("id", id));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> resourceTags(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        String sql = "select t.id,t.name,t.created_at from tags t join resource_tags rt on rt.tag_id=t.id where t.owner_id=:owner and rt.resource_id=:resource order by t.name";
        return jdbc.query(sql, Map.of("owner", ownerId, "resource", resourceId), (rs, rowNum) ->
                new TagResponse(rs.getLong("id"), rs.getString("name"), toInstant(rs.getTimestamp("created_at")))
        );
    }

    @Transactional
    public void assignTag(Long ownerId, Long resourceId, Long tagId) {
        requireResource(ownerId, resourceId);
        requireTag(ownerId, tagId);
        jdbc.update("insert into resource_tags(resource_id,tag_id) values(:resource,:tag) on conflict do nothing",
                Map.of("resource", resourceId, "tag", tagId));
    }

    @Transactional
    public void removeTag(Long ownerId, Long resourceId, Long tagId) {
        requireResource(ownerId, resourceId);
        requireTag(ownerId, tagId);
        jdbc.update("delete from resource_tags where resource_id=:resource and tag_id=:tag",
                Map.of("resource", resourceId, "tag", tagId));
    }

    @Transactional
    public TagResponse findOrCreateTag(Long ownerId, String name) {
        return createTag(ownerId, name);
    }

    @Transactional
    public CollectionResponse createCollection(Long ownerId, String name, String description) {
        String reqName = required(name);
        jdbc.update("insert into collections(owner_id,name,description,created_at,updated_at) values(:owner,:name,:description,now(),now())",
                Map.of("owner", ownerId, "name", reqName, "description", blankToNull(description)));
        return jdbc.queryForObject("select id,name,description,created_at,updated_at from collections where owner_id=:owner and name=:name",
                Map.of("owner", ownerId, "name", reqName),
                (rs, rowNum) -> new CollectionResponse(rs.getLong("id"), rs.getString("name"), rs.getString("description"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")))
        );
    }

    @Transactional
    public CollectionResponse findOrCreateCollection(Long ownerId, String name, String description) {
        String reqName = required(name);
        jdbc.update("insert into collections(owner_id,name,description,created_at,updated_at) values(:owner,:name,:description,now(),now()) on conflict(owner_id,name) do update set updated_at=now()",
                Map.of("owner", ownerId, "name", reqName, "description", blankToNull(description)));
        return jdbc.queryForObject("select id,name,description,created_at,updated_at from collections where owner_id=:owner and name=:name",
                Map.of("owner", ownerId, "name", reqName),
                (rs, rowNum) -> new CollectionResponse(rs.getLong("id"), rs.getString("name"), rs.getString("description"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")))
        );
    }

    @Transactional
    public CollectionResponse updateCollection(Long ownerId, Long id, String name, String description) {
        requireCollection(ownerId, id);
        jdbc.update("update collections set name=:name,description=:description,updated_at=now() where id=:id",
                Map.of("id", id, "name", required(name), "description", blankToNull(description)));
        return jdbc.queryForObject("select id,name,description,created_at,updated_at from collections where id=:id",
                Map.of("id", id),
                (rs, rowNum) -> new CollectionResponse(rs.getLong("id"), rs.getString("name"), rs.getString("description"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")))
        );
    }

    @Transactional
    public void deleteCollection(Long ownerId, Long id) {
        requireCollection(ownerId, id);
        jdbc.update("delete from collections where id=:id", Map.of("id", id));
    }

    @Transactional
    public void assignResource(Long ownerId, Long collectionId, Long resourceId) {
        requireCollection(ownerId, collectionId);
        requireResource(ownerId, resourceId);
        jdbc.update("insert into resource_collections(resource_id,collection_id) values(:resource,:collection) on conflict do nothing",
                Map.of("resource", resourceId, "collection", collectionId));
    }

    @Transactional
    public void removeResource(Long ownerId, Long collectionId, Long resourceId) {
        requireCollection(ownerId, collectionId);
        jdbc.update("delete from resource_collections where resource_id=:resource and collection_id=:collection",
                Map.of("resource", resourceId, "collection", collectionId));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> collectionResources(Long ownerId, Long collectionId) {
        requireCollection(ownerId, collectionId);
        String sql = "select r.id,r.title,r.description,r.resource_type,r.processing_status,r.original_filename,r.mime_type,r.size_bytes,r.favorite,r.priority,r.processing_error,r.created_at,r.updated_at " +
                "from resources r join resource_collections rc on rc.resource_id=r.id " +
                "where rc.collection_id=:collection and r.owner_id=:owner order by r.updated_at desc";
        return jdbc.query(sql, Map.of("collection", collectionId, "owner", ownerId), (rs, rowNum) ->
                new ResourceResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        ResourceType.valueOf(rs.getString("resource_type")),
                        ResourceProcessingStatus.valueOf(rs.getString("processing_status")),
                        rs.getString("original_filename"),
                        rs.getString("mime_type"),
                        rs.getObject("size_bytes") != null ? rs.getLong("size_bytes") : null,
                        rs.getBoolean("favorite"),
                        rs.getInt("priority"),
                        rs.getString("processing_error"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                )
        );
    }

    @Transactional(readOnly = true)
    public List<ResourceNoteResponse> notes(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        String sql = "select id,content,created_at,updated_at from resource_notes where owner_id=:owner and resource_id=:resource order by updated_at desc";
        return jdbc.query(sql, Map.of("owner", ownerId, "resource", resourceId), (rs, rowNum) ->
                new ResourceNoteResponse(
                        rs.getLong("id"),
                        rs.getString("content"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                )
        );
    }

    @Transactional
    public ResourceNoteResponse createNote(Long ownerId, Long resourceId, String content) {
        requireResource(ownerId, resourceId);
        jdbc.update("insert into resource_notes(resource_id,owner_id,content,created_at,updated_at) values(:resource,:owner,:content,now(),now())",
                Map.of("resource", resourceId, "owner", ownerId, "content", required(content)));
        return jdbc.queryForObject("select id,content,created_at,updated_at from resource_notes where owner_id=:owner and resource_id=:resource order by id desc limit 1",
                Map.of("owner", ownerId, "resource", resourceId),
                (rs, rowNum) -> new ResourceNoteResponse(rs.getLong("id"), rs.getString("content"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")))
        );
    }

    @Transactional
    public ResourceNoteResponse updateNote(Long ownerId, Long resourceId, Long noteId, String content) {
        int updated = jdbc.update("update resource_notes set content=:content,updated_at=now() where id=:id and owner_id=:owner and resource_id=:resource",
                Map.of("id", noteId, "owner", ownerId, "resource", resourceId, "content", required(content)));
        if (updated == 0) throw new NotFoundException("Resource note not found.");
        return jdbc.queryForObject("select id,content,created_at,updated_at from resource_notes where id=:id",
                Map.of("id", noteId),
                (rs, rowNum) -> new ResourceNoteResponse(rs.getLong("id"), rs.getString("content"), toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")))
        );
    }

    @Transactional
    public void deleteNote(Long ownerId, Long resourceId, Long noteId) {
        if (jdbc.update("delete from resource_notes where id=:id and owner_id=:owner and resource_id=:resource",
                Map.of("id", noteId, "owner", ownerId, "resource", resourceId)) == 0) {
            throw new NotFoundException("Resource note not found.");
        }
    }

    @Transactional(readOnly = true)
    public List<RelatedResourceResponse> related(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        String sql = "select r.id,r.title,r.description,r.resource_type,r.processing_status,rr.relation_type,rr.created_at " +
                "from resource_relations rr join resources r on r.id=rr.target_resource_id " +
                "where rr.source_resource_id=:resource and r.owner_id=:owner " +
                "union all " +
                "select r.id,r.title,r.description,r.resource_type,r.processing_status,rr.relation_type,rr.created_at " +
                "from resource_relations rr join resources r on r.id=rr.source_resource_id " +
                "where rr.target_resource_id=:resource and r.owner_id=:owner " +
                "order by created_at desc";
        return jdbc.query(sql, Map.of("owner", ownerId, "resource", resourceId), (rs, rowNum) ->
                new RelatedResourceResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("resource_type"),
                        rs.getString("processing_status"),
                        rs.getString("relation_type"),
                        toInstant(rs.getTimestamp("created_at"))
                )
        );
    }

    @Transactional(readOnly = true)
    public ResourceActivityResponse activity(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        String sql = "select r.processing_status,r.created_at,r.updated_at,coalesce(p.progress_percent,0) as progress_percent,p.last_opened_at," +
                "(select count(*) from resource_notes n where n.owner_id=:owner and n.resource_id=r.id) as note_count " +
                "from resources r left join learning_progress p on p.resource_id=r.id and p.owner_id=:owner " +
                "where r.id=:resource and r.owner_id=:owner";
        return jdbc.queryForObject(sql, Map.of("owner", ownerId, "resource", resourceId), (rs, rowNum) ->
                new ResourceActivityResponse(
                        rs.getString("processing_status"),
                        rs.getInt("progress_percent"),
                        rs.getLong("note_count"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at")),
                        toInstant(rs.getTimestamp("last_opened_at"))
                )
        );
    }

    @Transactional
    public ResourceActivityResponse updateProgress(Long ownerId, Long resourceId, int progress) {
        requireResource(ownerId, resourceId);
        if (progress < 0 || progress > 100) throw new IllegalArgumentException("Progress must be between 0 and 100.");
        jdbc.update("insert into learning_progress(resource_id,owner_id,progress_percent,last_opened_at,updated_at) " +
                        "values(:resource,:owner,:progress,now(),now()) " +
                        "on conflict(resource_id,owner_id) do update set progress_percent=excluded.progress_percent,last_opened_at=excluded.last_opened_at,updated_at=excluded.updated_at",
                Map.of("resource", resourceId, "owner", ownerId, "progress", progress));
        return activity(ownerId, resourceId);
    }

    public void requireCollection(Long ownerId, Long id) {
        if (jdbc.queryForObject("select count(*) from collections where id=:id and owner_id=:owner", Map.of("id", id, "owner", ownerId), Integer.class) == 0) {
            throw new NotFoundException("Collection not found.");
        }
    }

    private void requireResource(Long ownerId, Long id) {
        if (jdbc.queryForObject("select count(*) from resources where id=:id and owner_id=:owner", Map.of("id", id, "owner", ownerId), Integer.class) == 0) {
            throw new NotFoundException("Resource not found.");
        }
    }

    public void requireTag(Long ownerId, Long id) {
        if (jdbc.queryForObject("select count(*) from tags where id=:id and owner_id=:owner", Map.of("id", id, "owner", ownerId), Integer.class) == 0) {
            throw new NotFoundException("Tag not found.");
        }
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A value is required.");
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeTag(String value) {
        return required(value).toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "-");
    }
}
