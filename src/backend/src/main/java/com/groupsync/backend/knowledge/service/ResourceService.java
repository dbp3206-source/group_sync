package com.groupsync.backend.knowledge.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.knowledge.ingestion.ResourceProcessingRequestedEvent;
import com.groupsync.backend.shared.exception.*;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class ResourceService {
    private static final long MAX_UPLOAD_BYTES = 25L * 1024 * 1024;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher events;
    private final CitationRepository citationRepository;
    private final com.groupsync.backend.knowledge.repository.DocumentChunkRepository chunkRepository;
    private final com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry parserRegistry;

    public ResourceService(ResourceRepository resourceRepository, UserAccountRepository userRepository,
            StorageService storageService, ApplicationEventPublisher events, CitationRepository citationRepository,
            com.groupsync.backend.knowledge.repository.DocumentChunkRepository chunkRepository,
            com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry parserRegistry) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.events = events;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.parserRegistry = parserRegistry;
    }
    @Transactional
    public ResourceResponse upload(Long ownerId, MultipartFile file, String requestedTitle, String description) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Choose a resource to import.");
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new BadRequestException("Resources must be 25 MB or smaller.");
        ResourceType type = resolveType(file.getOriginalFilename(), file.getContentType());
        try {
            StorageService.StoredFile stored = storageService.store(ownerId, file.getOriginalFilename(), file.getInputStream());
            if (resourceRepository.findByOwnerIdAndChecksumSha256(ownerId, stored.checksumSha256()).isPresent()) { storageService.delete(stored.key()); throw new ConflictException("This resource is already in your library."); }
            Resource resource = new Resource(owner(ownerId), title(requestedTitle, file.getOriginalFilename()), normalize(description), type, file.getOriginalFilename(), file.getContentType(), stored.sizeBytes(), stored.key(), stored.checksumSha256());
            Resource saved = resourceRepository.save(resource);
            events.publishEvent(new ResourceProcessingRequestedEvent(saved.getId()));
            return ResourceResponse.from(saved);
        } catch (IOException exception) { throw new BadRequestException("The resource could not be stored."); }
    }
    @Transactional
    public ResourceResponse createNote(Long ownerId, CreateNoteResourceRequest request) {
        byte[] content = (request.content() == null ? "" : request.content()).getBytes(StandardCharsets.UTF_8);
        try {
            StorageService.StoredFile stored = storageService.store(ownerId, request.title() + ".md", new ByteArrayInputStream(content));
            Resource resource = new Resource(owner(ownerId), request.title().trim(), normalize(request.description()), ResourceType.NOTE, null, "text/markdown", stored.sizeBytes(), stored.key(), stored.checksumSha256());
            Resource saved = resourceRepository.save(resource);
            events.publishEvent(new ResourceProcessingRequestedEvent(saved.getId()));
            return ResourceResponse.from(saved);
        } catch (IOException exception) { throw new BadRequestException("The note could not be stored."); }
    }
    @Transactional(readOnly = true) public List<ResourceResponse> list(Long ownerId, String query, Long tagId, Long collectionId) { List<Resource> resources = resourceRepository.search(ownerId, query == null || query.isBlank() ? null : query.trim(), tagId, collectionId); return resources.stream().map(ResourceResponse::from).toList(); }
    @Transactional(readOnly = true) public ResourceResponse get(Long ownerId, Long resourceId) { return ResourceResponse.from(find(ownerId, resourceId)); }
    @Transactional
    public ResourceResponse update(Long ownerId, Long resourceId, UpdateResourceRequest request) {
        Resource resource = find(ownerId, resourceId);
        String title = request.title() != null && !request.title().isBlank() ? request.title().trim() : resource.getTitle();
        String description = request.description() != null ? normalize(request.description()) : resource.getDescription();
        boolean favorite = request.favorite() != null ? request.favorite() : resource.isFavorite();
        int priority = request.priority() != null ? request.priority() : resource.getPriority();
        resource.updateMetadata(title, description, favorite, priority);
        return ResourceResponse.from(resource);
    }
    @Transactional
    public void delete(Long ownerId, Long resourceId) {
        Resource resource = find(ownerId, resourceId);
        // Citations reference document_chunks with ON DELETE RESTRICT.
        // Delete citations first so that chunk (and then resource) deletion can proceed.
        // Historical chat session and message records are preserved; only the chunk FK link is removed.
        citationRepository.deleteByChunkResourceId(resourceId);
        try {
            storageService.delete(resource.getStorageKey());
        } catch (IOException exception) {
            throw new BadRequestException("The resource file could not be deleted.");
        }
        resourceRepository.delete(resource);
    }
    @Transactional public ResourceResponse retry(Long ownerId, Long resourceId) { Resource resource = find(ownerId, resourceId); resource.retry(); events.publishEvent(new ResourceProcessingRequestedEvent(resource.getId())); return ResourceResponse.from(resource); }
    @Transactional(readOnly = true) public InputStream content(Long ownerId, Long resourceId) { Resource resource = find(ownerId, resourceId); try { return storageService.open(resource.getStorageKey()); } catch (IOException exception) { throw new NotFoundException("Stored resource content was not found."); } }
    @Transactional(readOnly = true) public Resource resourceForOwner(Long ownerId, Long resourceId) { return find(ownerId, resourceId); }
    @Transactional(readOnly = true)
    public String extractedText(Long ownerId, Long resourceId) {
        Resource resource = find(ownerId, resourceId);
        // Phase 1.2: Canonical extracted text directly from stored document via parser registry (no chunk overlap duplication)
        if (resource.getStorageKey() != null) {
            try (InputStream is = storageService.open(resource.getStorageKey())) {
                com.groupsync.backend.knowledge.ingestion.ParsedResourceContent parsed =
                        parserRegistry.forType(resource.getResourceType()).parse(is);
                if (parsed != null && parsed.content() != null && !parsed.content().isBlank()) {
                    return parsed.content();
                }
            } catch (Exception ignored) {
            }
        }
        if (resource.getResourceType() == ResourceType.NOTE || resource.getResourceType() == ResourceType.TEXT || resource.getResourceType() == ResourceType.MARKDOWN) {
            try (InputStream is = storageService.open(resource.getStorageKey())) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return resource.getDescription() != null ? resource.getDescription() : "Nội dung văn bản đang được xử lý hoặc chưa sẵn sàng.";
    }
    private Resource find(Long ownerId, Long resourceId) { return resourceRepository.findByIdAndOwnerId(resourceId, ownerId).orElseThrow(() -> new NotFoundException("Resource not found.")); }
    private UserAccount owner(Long ownerId) { return userRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("User not found.")); }
    private ResourceType resolveType(String filename, String mimeType) { String value = filename == null ? "" : filename.toLowerCase(); if (value.endsWith(".pdf")) return ResourceType.PDF; if (value.endsWith(".docx")) return ResourceType.DOCX; if (value.endsWith(".md") || value.endsWith(".markdown")) return ResourceType.MARKDOWN; if (value.endsWith(".txt")) return ResourceType.TEXT; throw new BadRequestException("Supported files are PDF, DOCX, TXT, and Markdown."); }
    private String title(String requested, String filename) { if (requested != null && !requested.isBlank()) return requested.trim(); if (filename == null || filename.isBlank()) return "Untitled resource"; int dot = filename.lastIndexOf('.'); return (dot > 0 ? filename.substring(0, dot) : filename).trim(); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
