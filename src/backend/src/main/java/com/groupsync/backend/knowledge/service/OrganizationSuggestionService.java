package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.rag.RetrievedChunk;
import com.groupsync.backend.knowledge.rag.SemanticRetrievalService;
import com.groupsync.backend.shared.exception.NotFoundException;

/** Produces small, reviewable organization suggestions from the resource itself and its library. */
@Service
public class OrganizationSuggestionService {
    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final List<String> CONTROLLED_TAGS = List.of("rag", "retrieval", "embedding", "vector-search", "gemini", "oop", "design-patterns", "architecture", "vietnamese");
    private final NamedParameterJdbcTemplate jdbc;
    private final KnowledgeWorkspaceService workspace;
    private final SemanticRetrievalService retrieval;

    public OrganizationSuggestionService(NamedParameterJdbcTemplate jdbc, KnowledgeWorkspaceService workspace, SemanticRetrievalService retrieval) {
        this.jdbc = jdbc;
        this.workspace = workspace;
        this.retrieval = retrieval;
    }

    @Transactional(readOnly = true)
    public OrganizationSuggestionsResponse suggestions(Long ownerId, Long resourceId) {
        requireResource(ownerId, resourceId);
        Map<String, Object> resource = jdbc.queryForMap("select id,title,coalesce(description,'') as description from resources where id=:resource and owner_id=:owner", Map.of("resource", resourceId, "owner", ownerId));
        String corpus = ((String) resource.get("title")) + " " + resource.get("description") + " " + jdbc.queryForObject("select coalesce(string_agg(content,' '),'') from document_chunks where resource_id=:resource", Map.of("resource", resourceId), String.class);
        String normalized = corpus.toLowerCase(Locale.ROOT);
        List<TagResponse> existingTags = workspace.tags(ownerId);
        Set<String> existingNames = existingTags.stream().map(tag -> tag.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<OrganizationTagSuggestionResponse> suggestedTags = new ArrayList<>();
        for (String candidate : CONTROLLED_TAGS) {
            if (normalized.contains(candidate.replace('-', ' ')) || normalized.contains(candidate)) {
                TagResponse tag = existingTags.stream().filter(item -> candidate.equalsIgnoreCase(item.name())).findFirst().orElse(null);
                suggestedTags.add(new OrganizationTagSuggestionResponse(candidate, tag == null ? 0L : tag.id(), "Found in the title or extracted text", tag == null ? 0.72 : 0.9));
            }
        }
        if (suggestedTags.isEmpty()) {
            String first = Arrays.stream(WORDS.split(String.valueOf(resource.get("title")))).filter(word -> word.length() >= 4).findFirst().orElse("review").toLowerCase(Locale.ROOT);
            if (!existingNames.contains(first)) suggestedTags.add(new OrganizationTagSuggestionResponse(first, 0L, "A concise title keyword", 0.45));
        }
        List<OrganizationCollectionSuggestionResponse> suggestedCollections = collectionSuggestions(ownerId, normalized);
        List<OrganizationRelatedSuggestionResponse> related = relatedSuggestions(ownerId, resourceId, String.valueOf(resource.get("title")));
        return new OrganizationSuggestionsResponse(resourceId, suggestedTags.stream().limit(4).toList(), suggestedCollections.stream().limit(2).toList(), related.stream().limit(4).toList());
    }

    @Transactional
    public void apply(Long ownerId, Long resourceId, ApplyOrganizationRequest request) {
        requireResource(ownerId, resourceId);
        ApplyOrganizationRequest req = request != null ? request : new ApplyOrganizationRequest(List.of(), List.of(), List.of(), List.of());
        for (String name : req.tagNames()) {
            TagResponse tag = workspace.createTag(ownerId, name);
            workspace.assignTag(ownerId, resourceId, tag.id());
        }
        for (String name : req.newCollectionNames()) {
            CollectionResponse collection = workspace.createCollection(ownerId, name, "Created from a reviewed organization suggestion.");
            workspace.assignResource(ownerId, collection.id(), resourceId);
        }
        for (Long collectionId : req.collectionIds()) {
            workspace.assignResource(ownerId, collectionId, resourceId);
        }
        for (Long target : req.relatedResourceIds()) {
            requireResource(ownerId, target);
            if (!target.equals(resourceId)) {
                jdbc.update("insert into resource_relations(source_resource_id,target_resource_id,relation_type,created_at) values(:source,:target,'SUGGESTED_RELATED',now()) on conflict do nothing", Map.of("source", resourceId, "target", target));
            }
        }
    }

    private List<OrganizationCollectionSuggestionResponse> collectionSuggestions(Long ownerId, String text) {
        List<OrganizationCollectionSuggestionResponse> result = new ArrayList<>();
        for (CollectionResponse collection : workspace.collections(ownerId)) {
            String name = collection.name();
            String[] terms = WORDS.split(name.toLowerCase(Locale.ROOT));
            long matches = Arrays.stream(terms).filter(term -> term.length() > 2 && text.contains(term)).count();
            if (matches > 0) result.add(new OrganizationCollectionSuggestionResponse(name, collection.id(), "Collection name matches extracted topics", Math.min(0.95, 0.55 + matches * 0.15)));
        }
        if (result.isEmpty()) {
            String proposed = text.contains("rag") || text.contains("embedding") || text.contains("gemini") ? "AI Engineering" : text.contains("oop") || text.contains("pattern") ? "OOP Semester" : "Review Queue";
            result.add(new OrganizationCollectionSuggestionResponse(proposed, 0L, "A reviewable topic grouping based on extracted text", 0.48));
        }
        return result;
    }

    private List<OrganizationRelatedSuggestionResponse> relatedSuggestions(Long ownerId, Long resourceId, String title) {
        try {
            return retrieval.retrieve(ownerId, title, RetrievalScope.LIBRARY, null, List.of(), null).stream()
                    .filter(chunk -> chunk.resourceId() != resourceId)
                    .collect(Collectors.toMap(
                            RetrievedChunk::resourceId,
                            chunk -> new OrganizationRelatedSuggestionResponse(chunk.resourceId(), chunk.resourceTitle(), "Semantic evidence is close to this resource", 1 - chunk.distance()),
                            (first, second) -> first,
                            LinkedHashMap::new
                    )).values().stream().toList();
        } catch (RuntimeException ignored) { return List.of(); }
    }

    private void requireResource(Long ownerId, Long resourceId) {
        if (jdbc.queryForObject("select count(*) from resources where id=:resource and owner_id=:owner", Map.of("resource", resourceId, "owner", ownerId), Integer.class) == 0) {
            throw new NotFoundException("Resource not found.");
        }
    }
}
