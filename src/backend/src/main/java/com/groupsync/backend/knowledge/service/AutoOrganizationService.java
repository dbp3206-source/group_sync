package com.groupsync.backend.knowledge.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.service.DocumentUnderstandingService.Outcome;
import com.groupsync.backend.knowledge.service.SemanticCollectionOrganizationService.CollectionPlan;
import com.groupsync.backend.knowledge.service.SemanticTaggingService.TagDecision;

/** Coordinates the one shared semantic policy used by ingestion, Library and suggestions. */
@Service
public class AutoOrganizationService {
    private static final Logger log = LoggerFactory.getLogger(AutoOrganizationService.class);
    private static final int MAX_BATCH_SIZE = 25;
    private final NamedParameterJdbcTemplate jdbc;
    private final DocumentUnderstandingService understandingService;
    private final SemanticTaggingService taggingService;
    private final SemanticCollectionOrganizationService collectionService;
    private final KnowledgeWorkspaceService workspace;

    public AutoOrganizationService(NamedParameterJdbcTemplate jdbc,
                                   DocumentUnderstandingService understandingService,
                                   SemanticTaggingService taggingService,
                                   SemanticCollectionOrganizationService collectionService,
                                   KnowledgeWorkspaceService workspace) {
        this.jdbc = jdbc;
        this.understandingService = understandingService;
        this.taggingService = taggingService;
        this.collectionService = collectionService;
        this.workspace = workspace;
    }

    public SemanticOrganizationResult autoOrganize(Long ownerId, Long resourceId) {
        long started = System.nanoTime();
        SemanticPlan plan = preview(ownerId, resourceId);
        if (plan.understanding().result() == null) {
            return new SemanticOrganizationResult(resourceId, plan.understanding().status(), List.of(), List.of(),
                    List.of(), List.of(), plan.understanding().warnings());
        }

        List<String> assignedTags = new ArrayList<>();
        for (TagDecision decision : plan.tags()) {
            TagResponse tag = decision.existingTagId() == null
                    ? workspace.findOrCreateTag(ownerId, decision.canonicalLabel())
                    : workspace.tag(ownerId, decision.existingTagId());
            if (workspace.assignTagIfMissing(ownerId, resourceId, tag.id())) assignedTags.add(tag.name());
        }

        List<Long> assignedCollections = new ArrayList<>();
        for (OrganizationCollectionSuggestionResponse match : plan.collections().strongMatches()) {
            if (workspace.assignResourceIfMissing(ownerId, match.existingCollectionId(), resourceId)) {
                assignedCollections.add(match.existingCollectionId());
            }
        }

        log.info("Semantic organization completed resourceId={} stage=ORGANIZE durationMs={} tagsAssigned={} collectionsAssigned={} suggestions={}",
                resourceId, (System.nanoTime() - started) / 1_000_000L, assignedTags.size(),
                assignedCollections.size(), plan.collections().possibleMatches().size() + plan.collections().newSuggestions().size());
        return new SemanticOrganizationResult(resourceId, plan.understanding().status(), assignedTags,
                assignedCollections, plan.collections().possibleMatches(), plan.collections().newSuggestions(),
                plan.understanding().warnings());
    }

    public SemanticOrganizationResult autoOrganizeByResourceId(Long resourceId) {
        Long ownerId = jdbc.query("select owner_id from resources where id=:resource",
                Map.of("resource", resourceId), rs -> rs.next() ? rs.getLong(1) : null);
        if (ownerId == null) throw new IllegalArgumentException("Resource not found.");
        return autoOrganize(ownerId, resourceId);
    }

    public OrganizationBatchResult autoOrganizeAll(Long ownerId) {
        List<Long> resourceIds = jdbc.queryForList("""
                select id from resources where owner_id=:owner and processing_status='READY'
                order by updated_at desc limit :limit
                """, Map.of("owner", ownerId, "limit", MAX_BATCH_SIZE), Long.class);
        List<SemanticOrganizationResult> results = new ArrayList<>();
        int assigned = 0, suggested = 0, skipped = 0, failed = 0;
        for (Long resourceId : resourceIds) {
            try {
                SemanticOrganizationResult result = autoOrganize(ownerId, resourceId);
                results.add(result);
                if ("FAILED".equals(result.understandingStatus())) {
                    failed++;
                } else {
                    if (result.assignedAnything()) assigned++;
                    if (result.hasSuggestions()) suggested++;
                    if (!result.assignedAnything() && !result.hasSuggestions()) skipped++;
                }
            } catch (RuntimeException exception) {
                failed++;
                results.add(new SemanticOrganizationResult(resourceId, "FAILED", List.of(), List.of(),
                        List.of(), List.of(), List.of("Semantic organization failed.")));
                log.warn("Semantic organization failed resourceId={} category={}", resourceId,
                        exception.getClass().getSimpleName());
            }
        }
        return new OrganizationBatchResult(resourceIds.size(), assigned, suggested, skipped, failed, results);
    }

    public OrganizationSuggestionsResponse suggestions(Long ownerId, Long resourceId) {
        SemanticPlan plan = preview(ownerId, resourceId);
        List<OrganizationTagSuggestionResponse> tags = plan.tags().stream()
                .map(tag -> new OrganizationTagSuggestionResponse(tag.canonicalLabel(),
                        tag.existingTagId() == null ? 0L : tag.existingTagId(),
                        tag.existingTagId() == null ? "Useful semantic tag" : "Equivalent existing tag",
                        tag.confidence())).toList();
        List<OrganizationCollectionSuggestionResponse> collections = new ArrayList<>();
        collections.addAll(plan.collections().strongMatches());
        collections.addAll(plan.collections().possibleMatches());
        collections.addAll(plan.collections().newSuggestions());
        return new OrganizationSuggestionsResponse(resourceId, tags, collections, List.of());
    }

    SemanticPlan preview(Long ownerId, Long resourceId) {
        Outcome understanding = understandingService.understand(ownerId, resourceId);
        if (understanding.result() == null) {
            return new SemanticPlan(understanding, List.of(), new CollectionPlan(List.of(), List.of(), List.of()));
        }
        return new SemanticPlan(understanding, taggingService.plan(ownerId, understanding.result()),
                collectionService.plan(ownerId, understanding.result()));
    }

    record SemanticPlan(Outcome understanding, List<TagDecision> tags, CollectionPlan collections) { }
}
