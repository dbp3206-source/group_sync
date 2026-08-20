package com.groupsync.backend.knowledge.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.knowledge.service.OrganizationSuggestionService;

@RestController
public class KnowledgeWorkspaceController {
    private final KnowledgeWorkspaceService workspace;
    private final OrganizationSuggestionService organization;

    public KnowledgeWorkspaceController(KnowledgeWorkspaceService workspace, OrganizationSuggestionService organization) {
        this.workspace = workspace;
        this.organization = organization;
    }

    @GetMapping("/api/collections")
    public List<CollectionResponse> collections(@AuthenticationPrincipal AuthenticatedUser user) {
        return workspace.collections(user.getId());
    }

    @GetMapping("/api/tags")
    public List<TagResponse> tags(@AuthenticationPrincipal AuthenticatedUser user) {
        return workspace.tags(user.getId());
    }

    @PostMapping("/api/tags")
    public TagResponse createTag(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateTagRequest request) {
        return workspace.createTag(user.getId(), request.name());
    }

    @PatchMapping("/api/tags/{id}")
    public TagResponse updateTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
        return workspace.updateTag(user.getId(), id, request.name());
    }

    @DeleteMapping("/api/tags/{id}")
    public ResponseEntity<Void> deleteTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        workspace.deleteTag(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/collections")
    public CollectionResponse createCollection(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateCollectionRequest request) {
        return workspace.createCollection(user.getId(), request.name(), request.description());
    }

    @PatchMapping("/api/collections/{id}")
    public CollectionResponse updateCollection(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody UpdateCollectionRequest request) {
        return workspace.updateCollection(user.getId(), id, request.name(), request.description());
    }

    @DeleteMapping("/api/collections/{id}")
    public ResponseEntity<Void> deleteCollection(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        workspace.deleteCollection(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/collections/{id}/resources")
    public List<ResourceResponse> collectionResources(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return workspace.collectionResources(user.getId(), id);
    }

    @PutMapping("/api/collections/{id}/resources/{resourceId}")
    public ResponseEntity<Void> assign(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @PathVariable Long resourceId) {
        workspace.assignResource(user.getId(), id, resourceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/collections/{id}/resources/{resourceId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @PathVariable Long resourceId) {
        workspace.removeResource(user.getId(), id, resourceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/resources/{resourceId}/notes")
    public List<ResourceNoteResponse> notes(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return workspace.notes(user.getId(), resourceId);
    }

    @GetMapping("/api/resources/{resourceId}/tags")
    public List<TagResponse> resourceTags(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return workspace.resourceTags(user.getId(), resourceId);
    }

    @PutMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ResponseEntity<Void> assignTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long tagId) {
        workspace.assignTag(user.getId(), resourceId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long tagId) {
        workspace.removeTag(user.getId(), resourceId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/resources/{resourceId}/notes")
    public ResourceNoteResponse createNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @Valid @RequestBody CreateNoteRequest request) {
        return workspace.createNote(user.getId(), resourceId, request.content());
    }

    @PatchMapping("/api/resources/{resourceId}/notes/{noteId}")
    public ResourceNoteResponse updateNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long noteId, @Valid @RequestBody CreateNoteRequest request) {
        return workspace.updateNote(user.getId(), resourceId, noteId, request.content());
    }

    @DeleteMapping("/api/resources/{resourceId}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long noteId) {
        workspace.deleteNote(user.getId(), resourceId, noteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/resources/{resourceId}/related")
    public List<RelatedResourceResponse> related(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return workspace.related(user.getId(), resourceId);
    }

    @GetMapping("/api/resources/{resourceId}/activity")
    public ResourceActivityResponse activity(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return workspace.activity(user.getId(), resourceId);
    }

    @PostMapping("/api/resources/{resourceId}/open")
    public ResourceActivityResponse open(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return workspace.recordResourceOpened(user.getId(), resourceId);
    }

    @PutMapping("/api/resources/{resourceId}/progress")
    public ResourceActivityResponse progress(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @Valid @RequestBody UpdateProgressRequest request) {
        return workspace.updateProgress(user.getId(), resourceId, request.progressPercent());
    }

    @GetMapping("/api/resources/{resourceId}/organization/suggestions")
    public OrganizationSuggestionsResponse organizationSuggestions(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) {
        return organization.suggestions(user.getId(), resourceId);
    }

    @PostMapping("/api/resources/{resourceId}/organization/apply")
    public ResponseEntity<Void> applyOrganization(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @Valid @RequestBody ApplyOrganizationRequest request) {
        organization.apply(user.getId(), resourceId, request);
        return ResponseEntity.noContent().build();
    }
}
