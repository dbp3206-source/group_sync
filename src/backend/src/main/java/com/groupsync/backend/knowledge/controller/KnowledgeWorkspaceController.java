package com.groupsync.backend.knowledge.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.knowledge.service.OrganizationSuggestionService;

@RestController
public class KnowledgeWorkspaceController {
    private final KnowledgeWorkspaceService workspace; private final OrganizationSuggestionService organization;
    public KnowledgeWorkspaceController(KnowledgeWorkspaceService workspace, OrganizationSuggestionService organization) { this.workspace = workspace; this.organization = organization; }
    @GetMapping("/api/collections") public List<Map<String,Object>> collections(@AuthenticationPrincipal AuthenticatedUser user) { return workspace.collections(user.getId()); }
    @GetMapping("/api/tags") public List<Map<String,Object>> tags(@AuthenticationPrincipal AuthenticatedUser user) { return workspace.tags(user.getId()); }
    @PostMapping("/api/tags") public Map<String,Object> createTag(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody Map<String,String> body) { return workspace.createTag(user.getId(), body.get("name")); }
    @PatchMapping("/api/tags/{id}") public Map<String,Object> updateTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @RequestBody Map<String,String> body) { return workspace.updateTag(user.getId(), id, body.get("name")); }
    @DeleteMapping("/api/tags/{id}") public ResponseEntity<Void> deleteTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { workspace.deleteTag(user.getId(), id); return ResponseEntity.noContent().build(); }
    @PostMapping("/api/collections") public Map<String,Object> createCollection(@AuthenticationPrincipal AuthenticatedUser user, @RequestBody Map<String,String> body) { return workspace.createCollection(user.getId(), body.get("name"), body.get("description")); }
    @PatchMapping("/api/collections/{id}") public Map<String,Object> updateCollection(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @RequestBody Map<String,String> body) { return workspace.updateCollection(user.getId(), id, body.get("name"), body.get("description")); }
    @DeleteMapping("/api/collections/{id}") public ResponseEntity<Void> deleteCollection(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { workspace.deleteCollection(user.getId(), id); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/collections/{id}/resources") public List<Map<String,Object>> collectionResources(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { return workspace.collectionResources(user.getId(), id); }
    @PutMapping("/api/collections/{id}/resources/{resourceId}") public ResponseEntity<Void> assign(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @PathVariable Long resourceId) { workspace.assignResource(user.getId(), id, resourceId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/api/collections/{id}/resources/{resourceId}") public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @PathVariable Long resourceId) { workspace.removeResource(user.getId(), id, resourceId); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/resources/{resourceId}/notes") public List<Map<String,Object>> notes(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) { return workspace.notes(user.getId(), resourceId); }
    @GetMapping("/api/resources/{resourceId}/tags") public List<Map<String,Object>> resourceTags(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) { return workspace.resourceTags(user.getId(), resourceId); }
    @PutMapping("/api/resources/{resourceId}/tags/{tagId}") public ResponseEntity<Void> assignTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long tagId) { workspace.assignTag(user.getId(), resourceId, tagId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/api/resources/{resourceId}/tags/{tagId}") public ResponseEntity<Void> removeTag(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long tagId) { workspace.removeTag(user.getId(), resourceId, tagId); return ResponseEntity.noContent().build(); }
    @PostMapping("/api/resources/{resourceId}/notes") public Map<String,Object> createNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @RequestBody Map<String,String> body) { return workspace.createNote(user.getId(), resourceId, body.get("content")); }
    @PatchMapping("/api/resources/{resourceId}/notes/{noteId}") public Map<String,Object> updateNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long noteId, @RequestBody Map<String,String> body) { return workspace.updateNote(user.getId(), resourceId, noteId, body.get("content")); }
    @DeleteMapping("/api/resources/{resourceId}/notes/{noteId}") public ResponseEntity<Void> deleteNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @PathVariable Long noteId) { workspace.deleteNote(user.getId(), resourceId, noteId); return ResponseEntity.noContent().build(); }
    @GetMapping("/api/resources/{resourceId}/related") public List<Map<String,Object>> related(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) { return workspace.related(user.getId(), resourceId); }
    @GetMapping("/api/resources/{resourceId}/activity") public Map<String,Object> activity(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) { return workspace.activity(user.getId(), resourceId); }
    @PutMapping("/api/resources/{resourceId}/progress") public Map<String,Object> progress(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @RequestBody Map<String,Integer> body) { return workspace.updateProgress(user.getId(), resourceId, body.getOrDefault("progressPercent", 0)); }
    @GetMapping("/api/resources/{resourceId}/organization/suggestions") public Map<String,Object> organizationSuggestions(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId) { return organization.suggestions(user.getId(), resourceId); }
    @PostMapping("/api/resources/{resourceId}/organization/apply") public ResponseEntity<Void> applyOrganization(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long resourceId, @RequestBody Map<String,Object> body) { organization.apply(user.getId(), resourceId, body); return ResponseEntity.noContent().build(); }
}
