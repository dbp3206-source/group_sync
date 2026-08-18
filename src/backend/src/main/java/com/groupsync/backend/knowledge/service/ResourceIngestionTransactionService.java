package com.groupsync.backend.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;

/**
 * Dedicated transactional boundary service for resource ingestion.
 * Keeps short, isolated database transactions separate from memory parsing and
 * external AI embedding calls, ensuring Spring transactional proxies intercept
 * REQUIRES_NEW boundaries correctly without self-invocation bypass.
 */
@Service
public class ResourceIngestionTransactionService {
    private static final Logger log = LoggerFactory.getLogger(ResourceIngestionTransactionService.class);

    private final ResourceRepository resourceRepository;
    private final DocumentChunkRepository chunkRepository;
    private final GeminiProperties geminiProperties;
    private final AutoOrganizationService autoOrganizationService;

    public ResourceIngestionTransactionService(ResourceRepository resourceRepository,
            DocumentChunkRepository chunkRepository, GeminiProperties geminiProperties,
            AutoOrganizationService autoOrganizationService) {
        this.resourceRepository = resourceRepository;
        this.chunkRepository = chunkRepository;
        this.geminiProperties = geminiProperties;
        this.autoOrganizationService = autoOrganizationService;
    }

    /**
     * Atomically claims an UPLOADED resource for processing by transitioning it to PARSING.
     * Returns true if this caller successfully claimed the resource; false if already claimed/processed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long resourceId) {
        if (resourceId == null) return false;
        int updated = resourceRepository.updateStatusIfMatches(resourceId, ResourceProcessingStatus.UPLOADED, ResourceProcessingStatus.PARSING);
        return updated > 0;
    }

    /**
     * Saves chunks and transitions the resource to READY inside a short dedicated transaction.
     * Deletes any previous chunks first to prevent duplicate chunk persistence.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReady(Long resourceId, List<String> pieces, List<float[]> embeddings, ParsedResourceContent parsed) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalStateException("Resource not found during final save: " + resourceId));

        resource.beginChunking();
        chunkRepository.deleteByResourceId(resource.getId());

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int index = 0; index < pieces.size(); index++) {
            DocumentChunk chunk = new DocumentChunk(resource, index, parsed.pageNumber(), parsed.section(), pieces.get(index));
            if (index < embeddings.size() && embeddings.get(index) != null) {
                chunk.embed(embeddings.get(index), geminiProperties.embeddingModel());
            }
            chunks.add(chunk);
        }
        chunkRepository.saveAll(chunks);

        resource.markReady();
        resourceRepository.save(resource);

        try {
            autoOrganizationService.autoOrganize(resource.getOwner().getId(), resource.getId());
        } catch (Exception e) {
            log.warn("Auto-organization skipped for resource {}: {}", resourceId, e.getMessage());
        }
    }

    /**
     * Marks a resource as FAILED inside a dedicated transaction upon unrecoverable error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long resourceId, String errorMessage) {
        try {
            Resource resource = resourceRepository.findById(resourceId).orElse(null);
            if (resource != null) {
                resource.markFailed(errorMessage);
                resourceRepository.save(resource);
            }
        } catch (Exception e) {
            log.error("Failed to mark resource {} as FAILED: {}", resourceId, e.getMessage());
        }
    }
}
