package com.groupsync.backend.knowledge.controller;

import java.io.InputStream;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.service.ResourceService;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final com.groupsync.backend.knowledge.service.AutoOrganizationService autoOrganizationService;

    public ResourceController(ResourceService resourceService,
            com.groupsync.backend.knowledge.service.AutoOrganizationService autoOrganizationService) {
        this.resourceService = resourceService;
        this.autoOrganizationService = autoOrganizationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(resourceService.upload(user.getId(), file, title, description));
    }

    @PostMapping("/notes")
    public ResponseEntity<ResourceResponse> note(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateNoteResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createNote(user.getId(), request));
    }

    @GetMapping
    public List<ResourceResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long collectionId) {
        return resourceService.list(user.getId(), q, tagId, collectionId);
    }

    @GetMapping("/{resourceId}")
    public ResourceResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId) {
        return resourceService.get(user.getId(), resourceId);
    }

    @PatchMapping("/{resourceId}")
    public ResourceResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateResourceRequest request) {
        return resourceService.update(user.getId(), resourceId, request);
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId) {
        resourceService.delete(user.getId(), resourceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{resourceId}/retry")
    public ResourceResponse retry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId) {
        return resourceService.retry(user.getId(), resourceId);
    }

    @GetMapping("/{resourceId}/content")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> content(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId) {
        Resource resource = resourceService.resourceForOwner(user.getId(), resourceId);
        InputStream input = resourceService.content(user.getId(), resourceId);
        MediaType type = resource.getMimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(resource.getMimeType());
        String filename = resource.getOriginalFilename() == null
                ? resource.getTitle()
                : resource.getOriginalFilename();
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(new org.springframework.core.io.InputStreamResource(input));
    }

    @PostMapping("/auto-organize-all")
    public ResponseEntity<java.util.Map<String, Object>> autoOrganizeAll(@AuthenticationPrincipal AuthenticatedUser user) {
        autoOrganizationService.autoOrganizeAll(user.getId());
        return ResponseEntity.ok(java.util.Map.of("message", "All resources organized into relevant collections and tags."));
    }

    @PostMapping("/{resourceId}/auto-organize")
    public ResponseEntity<java.util.Map<String, Object>> autoOrganize(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long resourceId) {
        autoOrganizationService.autoOrganize(user.getId(), resourceId);
        return ResponseEntity.ok(java.util.Map.of("message", "Resource organized into relevant collections and tags."));
    }
}
