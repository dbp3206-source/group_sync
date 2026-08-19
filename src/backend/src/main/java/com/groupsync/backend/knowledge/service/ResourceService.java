package com.groupsync.backend.knowledge.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.ingestion.ResourceProcessingRequestedEvent;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.shared.exception.*;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);

    private final long maxUploadBytes;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher events;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceParserRegistry parserRegistry;
    private final com.groupsync.backend.knowledge.rag.GeminiProperties geminiProperties;

    public ResourceService(
            @Value("${knowledge.upload.max-size:25MB}") org.springframework.util.unit.DataSize maxUploadSize,
            ResourceRepository resourceRepository,
            UserAccountRepository userRepository,
            StorageService storageService,
            ApplicationEventPublisher events,
            CitationRepository citationRepository,
            DocumentChunkRepository chunkRepository,
            ResourceParserRegistry parserRegistry) {
        this(maxUploadSize, resourceRepository, userRepository, storageService, events, citationRepository, chunkRepository, parserRegistry, null);
    }

    public ResourceService(
            @Value("${knowledge.upload.max-size:25MB}") org.springframework.util.unit.DataSize maxUploadSize,
            ResourceRepository resourceRepository,
            UserAccountRepository userRepository,
            StorageService storageService,
            ApplicationEventPublisher events,
            CitationRepository citationRepository,
            DocumentChunkRepository chunkRepository,
            ResourceParserRegistry parserRegistry,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.groupsync.backend.knowledge.rag.GeminiProperties geminiProperties) {
        this.maxUploadBytes = maxUploadSize != null ? maxUploadSize.toBytes() : 25L * 1024 * 1024;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.events = events;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.parserRegistry = parserRegistry;
        this.geminiProperties = geminiProperties;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    @Transactional
    public ResourceResponse upload(Long ownerId, MultipartFile file, String requestedTitle, String description) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose a resource to import.");
        }
        if (file.getSize() > maxUploadBytes) {
            long maxMb = maxUploadBytes / (1024 * 1024);
            throw new BadRequestException("Resources must be " + maxMb + " MB or smaller.");
        }
        ResourceType type = resolveType(file.getOriginalFilename(), file.getContentType());
        try {
            StorageService.StoredFile stored = storageService.store(ownerId, file.getOriginalFilename(), file.getInputStream());
            if (resourceRepository.findByOwnerIdAndChecksumSha256(ownerId, stored.checksumSha256()).isPresent()) {
                storageService.delete(stored.key());
                throw new ConflictException("This resource is already in your library.");
            }
            Resource resource = new Resource(
                    owner(ownerId),
                    title(requestedTitle, file.getOriginalFilename()),
                    normalize(description),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    stored.sizeBytes(),
                    stored.key(),
                    stored.checksumSha256()
            );
            Resource saved = resourceRepository.save(resource);
            events.publishEvent(new ResourceProcessingRequestedEvent(saved.getId()));
            return ResourceResponse.from(saved);
        } catch (IOException exception) {
            throw new BadRequestException("The resource could not be stored.");
        }
    }

    @Transactional
    public ResourceResponse createNote(Long ownerId, CreateNoteResourceRequest request) {
        byte[] content = (request.content() == null ? "" : request.content()).getBytes(StandardCharsets.UTF_8);
        try {
            StorageService.StoredFile stored = storageService.store(ownerId, request.title() + ".md", new ByteArrayInputStream(content));
            Resource resource = new Resource(
                    owner(ownerId),
                    request.title().trim(),
                    normalize(request.description()),
                    ResourceType.NOTE,
                    null,
                    "text/markdown",
                    stored.sizeBytes(),
                    stored.key(),
                    stored.checksumSha256()
            );
            Resource saved = resourceRepository.save(resource);
            events.publishEvent(new ResourceProcessingRequestedEvent(saved.getId()));
            return ResourceResponse.from(saved);
        } catch (IOException exception) {
            throw new BadRequestException("The note could not be stored.");
        }
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> list(Long ownerId, String query, Long tagId, Long collectionId) {
        List<Resource> resources = resourceRepository.search(ownerId, query == null || query.isBlank() ? null : query.trim(), tagId, collectionId);
        return resources.stream().map(ResourceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(Long ownerId, Long resourceId) {
        return ResourceResponse.from(find(ownerId, resourceId));
    }

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
        citationRepository.deleteByChunkResourceId(resourceId);
        try {
            storageService.delete(resource.getStorageKey());
        } catch (IOException exception) {
            throw new BadRequestException("The resource file could not be deleted.");
        }
        resourceRepository.delete(resource);
    }

    @Transactional
    public ResourceResponse retry(Long ownerId, Long resourceId) {
        Resource resource = find(ownerId, resourceId);
        resource.retry();
        events.publishEvent(new ResourceProcessingRequestedEvent(resource.getId()));
        return ResourceResponse.from(resource);
    }

    @Transactional(readOnly = true)
    public InputStream content(Long ownerId, Long resourceId) {
        Resource resource = find(ownerId, resourceId);
        try {
            return storageService.open(resource.getStorageKey());
        } catch (IOException exception) {
            throw new NotFoundException("Stored resource content was not found.");
        }
    }

    @Transactional(readOnly = true)
    public Resource resourceForOwner(Long ownerId, Long resourceId) {
        return find(ownerId, resourceId);
    }

    public String extractedText(Long ownerId, Long resourceId) {
        // Step 1: Read resource metadata in short read transaction
        Resource resource = resourceForOwner(ownerId, resourceId);
        String storageKey = resource.getStorageKey();
        ResourceType type = resource.getResourceType();

        if (storageKey == null || storageKey.isBlank()) {
            throw new NotFoundException("Stored resource content was not found.");
        }

        // Step 2: Storage I/O and parsing execute outside DB transaction
        try (InputStream is = storageService.open(storageKey)) {
            ParsedResourceContent parsed = parserRegistry.forType(type).parse(is);
            if (parsed != null && parsed.content() != null && !parsed.content().isBlank()) {
                return parsed.content();
            }
            throw new BadRequestException("Tài liệu không chứa nội dung văn bản khả dụng.");
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse resource {} ({}) from storage: {}", resourceId, type, e.getMessage());
            throw new BadRequestException("Không thể đọc nội dung tài liệu: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResourceIngestionTraceDto getIngestionTrace(Long ownerId, Long resourceId) {
        Resource resource = find(ownerId, resourceId);
        List<DocumentChunk> chunks = chunkRepository.findByResourceIdOrderByChunkIndex(resourceId);

        int parentCount = 0;
        int childCount = 0;
        int maxVersion = 1;
        for (DocumentChunk chunk : chunks) {
            if (chunk.getChunkLevel() == ChunkLevel.PARENT) {
                parentCount++;
            } else {
                childCount++;
            }
            if (chunk.getChunkingVersion() > maxVersion) {
                maxVersion = chunk.getChunkingVersion();
            }
        }

        boolean isV2 = maxVersion >= 2;
        int batchSize = 16;
        int embeddingBatchCount = isV2 ? (childCount > 0 ? (int) Math.ceil((double) childCount / batchSize) : 0) : 0;
        boolean semanticMetadataIncluded = isV2;

        return new ResourceIngestionTraceDto(
                resource.getId(),
                resource.getTitle(),
                resource.getResourceType().name(),
                resource.getProcessingStatus().name(),
                maxVersion,
                isV2 ? parentCount : 0,
                isV2 ? childCount : chunks.size(),
                embeddingBatchCount,
                geminiProperties != null ? geminiProperties.embeddingModel() : "gemini-embedding-001",
                geminiProperties != null ? geminiProperties.embeddingDimensions() : 768,
                semanticMetadataIncluded
        );
    }

    private Resource find(Long ownerId, Long resourceId) {
        return resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Resource not found."));
    }

    private UserAccount owner(Long ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    public ResourceType resolveType(String filename, String mimeType) {
        String value = filename == null ? "" : filename.toLowerCase(java.util.Locale.ROOT);
        String cleanMime = mimeType != null ? mimeType.trim().toLowerCase(java.util.Locale.ROOT) : "";

        if (value.endsWith(".pdf")) {
            if (!cleanMime.isEmpty() && !cleanMime.equals("application/pdf") && !cleanMime.equals("application/octet-stream") && !cleanMime.equals("application/x-pdf")) {
                throw new BadRequestException("Tệp PDF có định dạng MIME không khớp: " + mimeType);
            }
            return ResourceType.PDF;
        }
        if (value.endsWith(".docx")) {
            if (!cleanMime.isEmpty()
                    && !cleanMime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    && !cleanMime.equals("application/msword")
                    && !cleanMime.equals("application/octet-stream")
                    && !cleanMime.equals("application/x-zip-compressed")
                    && !cleanMime.equals("application/zip")) {
                throw new BadRequestException("Tệp DOCX có định dạng MIME không khớp: " + mimeType);
            }
            return ResourceType.DOCX;
        }
        if (value.endsWith(".md") || value.endsWith(".markdown")) {
            if (!cleanMime.isEmpty()
                    && !cleanMime.equals("text/markdown")
                    && !cleanMime.equals("text/plain")
                    && !cleanMime.equals("text/x-markdown")
                    && !cleanMime.equals("application/octet-stream")) {
                throw new BadRequestException("Tệp Markdown có định dạng MIME không khớp: " + mimeType);
            }
            return ResourceType.MARKDOWN;
        }
        if (value.endsWith(".txt")) {
            if (!cleanMime.isEmpty()
                    && !cleanMime.equals("text/plain")
                    && !cleanMime.equals("text/plain; charset=utf-8")
                    && !cleanMime.equals("application/octet-stream")) {
                throw new BadRequestException("Tệp TXT có định dạng MIME không khớp: " + mimeType);
            }
            return ResourceType.TEXT;
        }
        throw new BadRequestException("Supported files are PDF, DOCX, TXT, and Markdown.");
    }

    private String title(String requested, String filename) {
        if (requested != null && !requested.isBlank()) return requested.trim();
        if (filename == null || filename.isBlank()) return "Untitled resource";
        int dot = filename.lastIndexOf('.');
        return (dot > 0 ? filename.substring(0, dot) : filename).trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
