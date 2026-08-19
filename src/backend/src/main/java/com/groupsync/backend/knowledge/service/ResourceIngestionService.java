package com.groupsync.backend.knowledge.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy;
import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy.HierarchicalChunk;
import com.groupsync.backend.knowledge.ingestion.ParsedDocument;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.ingestion.ResourceProcessingRequestedEvent;
import com.groupsync.backend.knowledge.model.ChunkLevel;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder.SemanticMetadata;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.storage.StorageService;

/**
 * Coordinates resource ingestion workflow for RAG v2.
 * Executes structure-aware parsing, hierarchical chunking, rich embedding text generation,
 * and batch Gemini embedding completely outside database transactions.
 */
@Service
public class ResourceIngestionService {
    private static final Logger log = LoggerFactory.getLogger(ResourceIngestionService.class);

    private final ResourceRepository resourceRepository;
    private final ResourceParserRegistry parserRegistry;
    private final StructureAwareChunkingStrategy chunkingStrategy;
    private final StorageService storageService;
    private final EmbeddingProvider embeddingProvider;
    private final EmbeddingTextBuilder embeddingTextBuilder;
    private final ResourceIngestionTransactionService transactionService;

    public ResourceIngestionService(
            ResourceRepository resourceRepository,
            ResourceParserRegistry parserRegistry,
            StructureAwareChunkingStrategy chunkingStrategy,
            StorageService storageService,
            EmbeddingProvider embeddingProvider,
            EmbeddingTextBuilder embeddingTextBuilder,
            ResourceIngestionTransactionService transactionService) {
        this.resourceRepository = resourceRepository;
        this.parserRegistry = parserRegistry;
        this.chunkingStrategy = chunkingStrategy;
        this.storageService = storageService;
        this.embeddingProvider = embeddingProvider;
        this.embeddingTextBuilder = embeddingTextBuilder;
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

        // Step 1: Atomic claim UPLOADED -> PARSING
        boolean claimed = transactionService.claim(resourceId);
        if (!claimed) {
            return;
        }

        executeIngestion(resourceId);
    }

    public void reindex(Long resourceId) {
        if (resourceId == null) return;
        boolean claimed = transactionService.claimForReindex(resourceId);
        if (!claimed) {
            return;
        }
        executeIngestion(resourceId);
    }

    private void executeIngestion(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return;
        }

        try {
            // Step 2: Parse document into structured blocks (OUTSIDE DB transaction)
            ParsedDocument parsedDoc = parseDocument(resource);

            // Step 3: Structure-aware hierarchical chunking (Parent & Child chunks)
            List<HierarchicalChunk> chunks = chunkingStrategy.chunkDocument(parsedDoc);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("No readable text was found in this resource.");
            }

            // Step 4: Pre-load resource semantic metadata (collections, tags) in one short read query
            SemanticMetadata metadata = transactionService.fetchSemanticMetadata(resourceId);

            // Step 5: Construct rich embedding texts for all CHILD chunks
            List<HierarchicalChunk> childChunks = chunks.stream()
                    .filter(c -> c.level() == ChunkLevel.CHILD)
                    .toList();

            List<String> richTexts = new ArrayList<>(childChunks.size());
            for (HierarchicalChunk child : childChunks) {
                SemanticMetadata chunkMetadata = new SemanticMetadata(
                        metadata.documentTitle(),
                        metadata.collectionNames(),
                        metadata.tagNames(),
                        child.sectionTitle()
                );
                richTexts.add(embeddingTextBuilder.build(chunkMetadata, child.content()));
            }

            // Step 6: Batch Gemini Embeddings for child chunks (OUTSIDE DB transaction)
            List<float[]> embeddings = embeddingProvider.embedDocuments(richTexts);

            if (embeddings == null || embeddings.size() != childChunks.size()) {
                int received = embeddings == null ? 0 : embeddings.size();
                throw new IllegalStateException("Embedding count mismatch: expected " + childChunks.size() + " embeddings but received " + received);
            }

            Map<Integer, float[]> childEmbeddingMap = new HashMap<>();
            for (int i = 0; i < childChunks.size(); i++) {
                float[] emb = embeddings.get(i);
                if (emb == null) {
                    throw new IllegalStateException("Embedding for child chunk at index " + i + " was null.");
                }
                if (emb.length != 768) {
                    throw new IllegalStateException("Embedding for child chunk at index " + i + " has invalid dimension " + emb.length + " (expected 768).");
                }
                childEmbeddingMap.put(childChunks.get(i).index(), emb);
            }

            // Step 7: Persist hierarchical parent + child chunks and mark READY
            transactionService.saveReadyHierarchical(resourceId, chunks, childEmbeddingMap);

        } catch (Exception exception) {
            log.error("Resource processing failed for id {}: {}", resourceId, exception.getMessage(), exception);
            transactionService.markFailed(resourceId, safeMessage(exception));
        }
    }

    private ParsedDocument parseDocument(Resource resource) throws IOException {
        try (InputStream input = storageService.open(resource.getStorageKey())) {
            return parserRegistry.forType(resource.getResourceType()).parseDocument(input);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Resource processing could not be completed." : message;
    }
}
