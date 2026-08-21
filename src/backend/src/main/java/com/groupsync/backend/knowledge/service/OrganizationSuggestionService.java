package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.rag.RetrievedChunk;
import com.groupsync.backend.knowledge.rag.SemanticRetrievalService;
import com.groupsync.backend.shared.exception.NotFoundException;

/** Review/apply facade backed by the same semantic policy as automatic organization. */
@Service
public class OrganizationSuggestionService {
    private final NamedParameterJdbcTemplate jdbc;
    private final KnowledgeWorkspaceService workspace;
    private final SemanticRetrievalService retrieval;
    private final AutoOrganizationService semanticOrganization;

    public OrganizationSuggestionService(NamedParameterJdbcTemplate jdbc, KnowledgeWorkspaceService workspace,
                                         SemanticRetrievalService retrieval, AutoOrganizationService semanticOrganization) {
        this.jdbc = jdbc;
        this.workspace = workspace;
        this.retrieval = retrieval;
        this.semanticOrganization = semanticOrganization;
    }

    public OrganizationSuggestionsResponse suggestions(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        OrganizationSuggestionsResponse semantic = semanticOrganization.suggestions(ownerId, resourceId);
        String title = jdbc.queryForObject("select title from resources where id=:resource and owner_id=:owner",
                Map.of("resource", resourceId, "owner", ownerId), String.class);
        return new OrganizationSuggestionsResponse(resourceId, semantic.suggestedTags(),
                semantic.suggestedCollections(), relatedSuggestions(ownerId, resourceId, title));
    }

    @Transactional
    public void apply(Long ownerId, Long resourceId, ApplyOrganizationRequest request) {
        requireResource(ownerId, resourceId);
        ApplyOrganizationRequest req = request != null ? request : new ApplyOrganizationRequest(List.of(), List.of(), List.of(), List.of());
        for (String name : req.tagNames()) {
            TagResponse tag = workspace.findOrCreateTag(ownerId, name);
            workspace.assignTag(ownerId, resourceId, tag.id());
        }
        for (String name : req.newCollectionNames()) {
            CollectionResponse collection = workspace.createCollection(ownerId, name, "Created from a reviewed semantic suggestion.");
            workspace.assignResource(ownerId, collection.id(), resourceId);
        }
        for (Long collectionId : req.collectionIds()) workspace.assignResource(ownerId, collectionId, resourceId);
        for (Long target : req.relatedResourceIds()) {
            requireResource(ownerId, target);
            if (!target.equals(resourceId)) {
                jdbc.update("insert into resource_relations(source_resource_id,target_resource_id,relation_type,created_at) values(:source,:target,'SUGGESTED_RELATED',now()) on conflict do nothing",
                        Map.of("source", resourceId, "target", target));
            }
        }
    }

    private List<OrganizationRelatedSuggestionResponse> relatedSuggestions(Long ownerId, Long resourceId, String title) {
        try {
            return retrieval.retrieve(ownerId, title, RetrievalScope.LIBRARY, null, List.of(), null).stream()
                    .filter(chunk -> !Objects.equals(chunk.resourceId(), resourceId))
                    .collect(Collectors.toMap(RetrievedChunk::resourceId,
                            chunk -> new OrganizationRelatedSuggestionResponse(chunk.resourceId(), chunk.resourceTitle(),
                                    "Semantic evidence is close to this resource", 1 - chunk.distance()),
                            (first, second) -> first, LinkedHashMap::new)).values().stream().limit(4).toList();
        } catch (RuntimeException ignored) { return List.of(); }
    }

    private void requireResource(Long ownerId, Long resourceId) {
        Integer count = jdbc.queryForObject("select count(*) from resources where id=:resource and owner_id=:owner",
                Map.of("resource", resourceId, "owner", ownerId), Integer.class);
        if (count == null || count == 0) throw new NotFoundException("Resource not found.");
    }
}
