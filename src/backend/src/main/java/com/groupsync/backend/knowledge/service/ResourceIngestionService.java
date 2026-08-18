package com.groupsync.backend.knowledge.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import com.groupsync.backend.knowledge.chunking.RecursiveChunkingStrategy;
import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.ingestion.ResourceProcessingRequestedEvent;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.storage.StorageService;

/**
 * Coordinates resource ingestion workflow.
 * Parsing, recursive chunking, and external Gemini embedding generation are executed
 * outside active database transactions to prevent connection pool starvation.
 * Database claims and final status transitions are delegated to {@link ResourceIngestionTransactionService}.
 */
@Service
public class ResourceIngestionService {
    private static final Logger log = LoggerFactory.getLogger(ResourceIngestionService.class);

    private final ResourceRepository resourceRepository;
    private final ResourceParserRegistry parserRegistry;
    private final RecursiveChunkingStrategy chunkingStrategy;
    private final StorageService storageService;
    private final EmbeddingProvider embeddingProvider;
    private final ResourceIngestionTransactionService transactionService;

    public ResourceIngestionService(ResourceRepository resourceRepository,
            ResourceParserRegistry parserRegistry, RecursiveChunkingStrategy chunkingStrategy,
            StorageService storageService, EmbeddingProvider embeddingProvider,
            ResourceIngestionTransactionService transactionService) {
        this.resourceRepository = resourceRepository;
        this.parserRegistry = parserRegistry;
        this.chunkingStrategy = chunkingStrategy;
        this.storageService = storageService;
        this.embeddingProvider = embeddingProvider;
        this.transactionService = transactionService;
    }

    @Async
    @TransactionalEventListener
    public void processAfterUpload(ResourceProcessingRequestedEvent event) {
        process(event.resourceId());
    }

    @Scheduled(fixedDelay = 10000)
    public void sweepPendingUploads() {
        List<Resource> pending = resourceRepository.findByProcessingStatus(ResourceProcessingStatus.UPLOADED);
        for (Resource res : pending) {
            try {
                process(res.getId());
            } catch (Exception e) {
                log.warn("Sweeper failed to process resource {}: {}", res.getId(), e.getMessage());
            }
        }
    }

    public void process(Long resourceId) {
        if (resourceId == null) return;

        // Step 1: Atomic claim UPLOADED -> PARSING via dedicated transaction service
        boolean claimed = transactionService.claim(resourceId);
        if (!claimed) {
            return;
        }

        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return;
        }

        try {
            // Step 2: Parse file (in memory / stream - OUTSIDE database transaction)
            ParsedResourceContent parsed = parse(resource);

            // Step 3: Chunk content (in memory - OUTSIDE database transaction)
            List<String> pieces = chunkingStrategy.chunk(parsed.content());
            if (pieces.isEmpty()) {
                throw new IllegalArgumentException("No readable text was found in this resource.");
            }

            // Step 4: External Gemini Embeddings (OUTSIDE database transaction)
            List<float[]> embeddings = new ArrayList<>(pieces.size());
            for (String piece : pieces) {
                embeddings.add(embeddingProvider.embedDocument(piece));
            }

            // Step 5: Save chunks and mark READY via dedicated transaction service
            transactionService.saveReady(resourceId, pieces, embeddings, parsed);

        } catch (Exception exception) {
            log.error("Resource processing failed for id {}: {}", resourceId, exception.getMessage(), exception);
            transactionService.markFailed(resourceId, safeMessage(exception));
        }
    }

    private ParsedResourceContent parse(Resource resource) throws IOException {
        try (InputStream input = storageService.open(resource.getStorageKey())) {
            return parserRegistry.forType(resource.getResourceType()).parse(input);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Resource processing could not be completed." : message;
    }
}
