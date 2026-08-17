package com.groupsync.backend.knowledge.service;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.shared.exception.NotFoundException;

/** Small query layer for workspace data that is intentionally stored in simple join tables. */
@Service
public class KnowledgeWorkspaceService {
    private final NamedParameterJdbcTemplate jdbc;
    public KnowledgeWorkspaceService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> collections(Long ownerId) {
        return jdbc.queryForList("select id, name, description, created_at, updated_at from collections where owner_id=:owner order by updated_at desc", Map.of("owner", ownerId));
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> tags(Long ownerId) {
        return jdbc.queryForList("select id, name, created_at from tags where owner_id=:owner order by name", Map.of("owner", ownerId));
    }
    @Transactional
    public Map<String, Object> createTag(Long ownerId, String name) {
        String normalized = normalizeTag(name);
        jdbc.update("insert into tags(owner_id,name,created_at) values(:owner,:name,now()) on conflict(owner_id,name) do nothing", Map.of("owner", ownerId, "name", normalized));
        return jdbc.queryForMap("select id,name,created_at from tags where owner_id=:owner and name=:name", Map.of("owner", ownerId, "name", normalized));
    }
    @Transactional
    public Map<String, Object> updateTag(Long ownerId, Long id, String name) {
        requireTag(ownerId, id);
        String normalized = normalizeTag(name);
        jdbc.update("update tags set name=:name where id=:id and owner_id=:owner", Map.of("owner", ownerId, "id", id, "name", normalized));
        return jdbc.queryForMap("select id,name,created_at from tags where id=:id and owner_id=:owner", Map.of("owner", ownerId, "id", id));
    }
    @Transactional
    public void deleteTag(Long ownerId, Long id) { requireTag(ownerId, id); jdbc.update("delete from tags where id=:id", Map.of("id", id)); }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resourceTags(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        return jdbc.queryForList("select t.id,t.name,t.created_at from tags t join resource_tags rt on rt.tag_id=t.id where t.owner_id=:owner and rt.resource_id=:resource order by t.name", Map.of("owner", ownerId, "resource", resourceId));
    }
    @Transactional
    public void assignTag(Long ownerId, Long resourceId, Long tagId) {
        requireResource(ownerId, resourceId); requireTag(ownerId, tagId);
        jdbc.update("insert into resource_tags(resource_id,tag_id) values(:resource,:tag) on conflict do nothing", Map.of("resource", resourceId, "tag", tagId));
    }
    @Transactional
    public void removeTag(Long ownerId, Long resourceId, Long tagId) {
        requireResource(ownerId, resourceId); requireTag(ownerId, tagId);
        jdbc.update("delete from resource_tags where resource_id=:resource and tag_id=:tag", Map.of("resource", resourceId, "tag", tagId));
    }
    @Transactional
    public Map<String, Object> createCollection(Long ownerId, String name, String description) {
        jdbc.update("insert into collections(owner_id,name,description,created_at,updated_at) values(:owner,:name,:description,now(),now())", Map.of("owner", ownerId, "name", required(name), "description", blankToNull(description)));
        return jdbc.queryForMap("select id,name,description,created_at,updated_at from collections where owner_id=:owner and name=:name", Map.of("owner", ownerId, "name", required(name)));
    }
    @Transactional
    public Map<String, Object> updateCollection(Long ownerId, Long id, String name, String description) {
        requireCollection(ownerId, id);
        jdbc.update("update collections set name=:name,description=:description,updated_at=now() where id=:id", Map.of("id", id, "name", required(name), "description", blankToNull(description)));
        return jdbc.queryForMap("select id,name,description,created_at,updated_at from collections where id=:id", Map.of("id", id));
    }
    @Transactional public void deleteCollection(Long ownerId, Long id) { requireCollection(ownerId, id); jdbc.update("delete from collections where id=:id", Map.of("id", id)); }
    @Transactional
    public void assignResource(Long ownerId, Long collectionId, Long resourceId) {
        requireCollection(ownerId, collectionId); requireResource(ownerId, resourceId);
        jdbc.update("insert into resource_collections(resource_id,collection_id) values(:resource,:collection) on conflict do nothing", Map.of("resource", resourceId, "collection", collectionId));
    }
    @Transactional
    public void removeResource(Long ownerId, Long collectionId, Long resourceId) {
        requireCollection(ownerId, collectionId);
        jdbc.update("delete from resource_collections where resource_id=:resource and collection_id=:collection", Map.of("resource", resourceId, "collection", collectionId));
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> collectionResources(Long ownerId, Long collectionId) {
        requireCollection(ownerId, collectionId);
        return jdbc.queryForList("select r.id,r.title,r.description,r.resource_type,r.processing_status,r.created_at,r.updated_at from resources r join resource_collections rc on rc.resource_id=r.id where rc.collection_id=:collection and r.owner_id=:owner order by r.updated_at desc", Map.of("collection", collectionId, "owner", ownerId));
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> notes(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        return jdbc.queryForList("select id,content,created_at,updated_at from resource_notes where owner_id=:owner and resource_id=:resource order by updated_at desc", Map.of("owner", ownerId, "resource", resourceId));
    }
    @Transactional
    public Map<String, Object> createNote(Long ownerId, Long resourceId, String content) {
        requireResource(ownerId, resourceId);
        jdbc.update("insert into resource_notes(resource_id,owner_id,content,created_at,updated_at) values(:resource,:owner,:content,now(),now())", Map.of("resource", resourceId,"owner",ownerId,"content",required(content)));
        return jdbc.queryForMap("select id,content,created_at,updated_at from resource_notes where owner_id=:owner and resource_id=:resource order by id desc limit 1", Map.of("owner",ownerId,"resource",resourceId));
    }
    @Transactional
    public Map<String, Object> updateNote(Long ownerId, Long resourceId, Long noteId, String content) {
        int updated = jdbc.update("update resource_notes set content=:content,updated_at=now() where id=:id and owner_id=:owner and resource_id=:resource", Map.of("id",noteId,"owner",ownerId,"resource",resourceId,"content",required(content)));
        if (updated == 0) throw new NotFoundException("Resource note not found.");
        return jdbc.queryForMap("select id,content,created_at,updated_at from resource_notes where id=:id", Map.of("id",noteId));
    }
    @Transactional public void deleteNote(Long ownerId, Long resourceId, Long noteId) { if (jdbc.update("delete from resource_notes where id=:id and owner_id=:owner and resource_id=:resource", Map.of("id",noteId,"owner",ownerId,"resource",resourceId)) == 0) throw new NotFoundException("Resource note not found."); }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> related(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        return jdbc.queryForList("select r.id,r.title,r.description,r.resource_type,r.processing_status,rr.relation_type,rr.created_at from resource_relations rr join resources r on r.id=rr.target_resource_id where rr.source_resource_id=:resource and r.owner_id=:owner union all select r.id,r.title,r.description,r.resource_type,r.processing_status,rr.relation_type,rr.created_at from resource_relations rr join resources r on r.id=rr.source_resource_id where rr.target_resource_id=:resource and r.owner_id=:owner order by created_at desc", Map.of("owner",ownerId,"resource",resourceId));
    }
    @Transactional(readOnly = true)
    public Map<String, Object> activity(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        return jdbc.queryForMap("select r.processing_status,r.created_at,r.updated_at,coalesce(p.progress_percent,0) as progress_percent,p.last_opened_at,(select count(*) from resource_notes n where n.owner_id=:owner and n.resource_id=r.id) as note_count from resources r left join learning_progress p on p.resource_id=r.id and p.owner_id=:owner where r.id=:resource and r.owner_id=:owner", Map.of("owner",ownerId,"resource",resourceId));
    }
    @Transactional
    public Map<String, Object> updateProgress(Long ownerId, Long resourceId, int progress) {
        requireResource(ownerId, resourceId); if (progress < 0 || progress > 100) throw new IllegalArgumentException("Progress must be between 0 and 100.");
        jdbc.update("insert into learning_progress(resource_id,owner_id,progress_percent,last_opened_at,updated_at) values(:resource,:owner,:progress,now(),now()) on conflict(resource_id,owner_id) do update set progress_percent=excluded.progress_percent,last_opened_at=excluded.last_opened_at,updated_at=excluded.updated_at", Map.of("resource",resourceId,"owner",ownerId,"progress",progress));
        return activity(ownerId, resourceId);
    }
    public void requireCollection(Long ownerId, Long id) { if (jdbc.queryForObject("select count(*) from collections where id=:id and owner_id=:owner", Map.of("id",id,"owner",ownerId), Integer.class) == 0) throw new NotFoundException("Collection not found."); }
    private void requireResource(Long ownerId, Long id) { if (jdbc.queryForObject("select count(*) from resources where id=:id and owner_id=:owner", Map.of("id",id,"owner",ownerId), Integer.class) == 0) throw new NotFoundException("Resource not found."); }
    public void requireTag(Long ownerId, Long id) { if (jdbc.queryForObject("select count(*) from tags where id=:id and owner_id=:owner", Map.of("id",id,"owner",ownerId), Integer.class) == 0) throw new NotFoundException("Tag not found."); }
    private String required(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("A value is required."); return value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeTag(String value) { return required(value).toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "-"); }
}
