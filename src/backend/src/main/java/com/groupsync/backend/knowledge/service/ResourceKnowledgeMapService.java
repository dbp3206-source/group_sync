package com.groupsync.backend.knowledge.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.CollectionResponse;
import com.groupsync.backend.knowledge.dto.RelatedResourceResponse;
import com.groupsync.backend.knowledge.dto.ResourceKnowledgeMapResponse;
import com.groupsync.backend.knowledge.dto.ResourceKnowledgeMapResponse.Edge;
import com.groupsync.backend.knowledge.dto.ResourceKnowledgeMapResponse.Node;
import com.groupsync.backend.knowledge.dto.TagResponse;

@Service
public class ResourceKnowledgeMapService {
    private static final int MAX_CONNECTED_RESOURCES = 8;

    private final NamedParameterJdbcTemplate jdbc;
    private final KnowledgeWorkspaceService workspace;

    public ResourceKnowledgeMapService(NamedParameterJdbcTemplate jdbc, KnowledgeWorkspaceService workspace) {
        this.jdbc = jdbc;
        this.workspace = workspace;
    }

    @Transactional(readOnly = true)
    public ResourceKnowledgeMapResponse get(Long ownerId, Long resourceId) {
        String title = jdbc.queryForObject(
                "select title from resources where id=:resource and owner_id=:owner",
                Map.of("resource", resourceId, "owner", ownerId), String.class);

        Node center = new Node(resourceKey(resourceId), "RESOURCE", title, resourceId, null, null);
        Map<String, Node> nodes = new LinkedHashMap<>();
        nodes.put(center.id(), center);
        List<Edge> edges = new ArrayList<>();
        Set<Long> connectedResourceIds = new LinkedHashSet<>();

        List<TagResponse> tags = workspace.resourceTags(ownerId, resourceId);
        for (TagResponse tag : tags) {
            String tagKey = tagKey(tag.id());
            nodes.put(tagKey, new Node(tagKey, "TAG", tag.name(), null, null, tag.id()));
            edges.add(new Edge(center.id(), tagKey, "RESOURCE_HAS_TAG", "Shared tag: " + tag.name(),
                    null, "resource_tags"));
        }

        List<CollectionResponse> collections = workspace.resourceCollections(ownerId, resourceId);
        for (CollectionResponse collection : collections) {
            String collectionKey = collectionKey(collection.id());
            nodes.put(collectionKey, new Node(collectionKey, "COLLECTION", collection.name(), null,
                    collection.id(), null));
            edges.add(new Edge(center.id(), collectionKey, "RESOURCE_IN_COLLECTION",
                    "Member of " + collection.name(), null, "resource_collections"));
        }

        for (RelatedResourceResponse related : workspace.related(ownerId, resourceId)) {
            if (connectedResourceIds.size() >= MAX_CONNECTED_RESOURCES
                    && !connectedResourceIds.contains(related.id())) continue;
            connectedResourceIds.add(related.id());
            addResourceNode(nodes, related.id(), related.title());
            edges.add(new Edge(center.id(), resourceKey(related.id()), "SEMANTICALLY_RELATED",
                    "Stored relation: " + safeRelation(related.relationType()), null, "resource_relations"));
        }

        List<Long> tagIds = tags.stream().map(TagResponse::id).toList();
        if (!tagIds.isEmpty()) {
            List<SharedLink> sharedTags = jdbc.query("""
                    select distinct r.id,r.title,t.name as shared_name,r.updated_at
                    from resources r
                    join resource_tags rt on rt.resource_id=r.id
                    join tags t on t.id=rt.tag_id
                    where r.owner_id=:owner and r.id<>:resource and rt.tag_id in (:tagIds)
                    order by r.updated_at desc limit :limit
                    """, new MapSqlParameterSource().addValue("owner", ownerId).addValue("resource", resourceId)
                    .addValue("tagIds", tagIds).addValue("limit", MAX_CONNECTED_RESOURCES),
                    (rs, rowNum) -> new SharedLink(rs.getLong("id"), rs.getString("title"),
                            rs.getString("shared_name"), "SHARES_TAG", "resource_tags"));
            addSharedLinks(sharedTags, connectedResourceIds, nodes, edges, center.id());
        }

        List<Long> collectionIds = collections.stream().map(CollectionResponse::id).toList();
        if (!collectionIds.isEmpty()) {
            List<SharedLink> sharedCollections = jdbc.query("""
                    select distinct r.id,r.title,c.name as shared_name,r.updated_at
                    from resources r
                    join resource_collections rc on rc.resource_id=r.id
                    join collections c on c.id=rc.collection_id
                    where r.owner_id=:owner and r.id<>:resource and rc.collection_id in (:collectionIds)
                    order by r.updated_at desc limit :limit
                    """, new MapSqlParameterSource().addValue("owner", ownerId).addValue("resource", resourceId)
                    .addValue("collectionIds", collectionIds).addValue("limit", MAX_CONNECTED_RESOURCES),
                    (rs, rowNum) -> new SharedLink(rs.getLong("id"), rs.getString("title"),
                            rs.getString("shared_name"), "SHARES_COLLECTION", "resource_collections"));
            addSharedLinks(sharedCollections, connectedResourceIds, nodes, edges, center.id());
        }
        return new ResourceKnowledgeMapResponse(new ArrayList<>(nodes.values()), edges);
    }

    private void addSharedLinks(List<SharedLink> links, Set<Long> connectedResourceIds,
                                Map<String, Node> nodes, List<Edge> edges, String centerId) {
        for (SharedLink link : links) {
            if (connectedResourceIds.size() >= MAX_CONNECTED_RESOURCES
                    && !connectedResourceIds.contains(link.resourceId())) continue;
            connectedResourceIds.add(link.resourceId());
            addResourceNode(nodes, link.resourceId(), link.title());
            String reasonPrefix = "SHARES_TAG".equals(link.relationType()) ? "Shared tag: " : "Same collection: ";
            edges.add(new Edge(centerId, resourceKey(link.resourceId()), link.relationType(),
                    reasonPrefix + link.sharedName(), null, link.provenance()));
        }
    }

    private void addResourceNode(Map<String, Node> nodes, Long id, String title) {
        nodes.putIfAbsent(resourceKey(id), new Node(resourceKey(id), "RESOURCE", title, id, null, null));
    }

    private String safeRelation(String relation) {
        return relation == null || relation.isBlank() ? "stored relation" : relation;
    }

    private String resourceKey(Long id) { return "resource:" + id; }
    private String collectionKey(Long id) { return "collection:" + id; }
    private String tagKey(Long id) { return "tag:" + id; }

    private record SharedLink(Long resourceId, String title, String sharedName,
                              String relationType, String provenance) { }
}
