package com.groupsync.backend.knowledge.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import com.groupsync.backend.knowledge.chunking.RecursiveChunkingStrategy;
import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.ingestion.ResourceProcessingRequestedEvent;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.storage.StorageService;

@Service
public class ResourceIngestionService {
    private final ResourceRepository resourceRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceParserRegistry parserRegistry;
    private final RecursiveChunkingStrategy chunkingStrategy;
    private final StorageService storageService;
    private final EmbeddingProvider embeddingProvider;
    private final GeminiProperties geminiProperties;

    public ResourceIngestionService(ResourceRepository resourceRepository, DocumentChunkRepository chunkRepository,
            ResourceParserRegistry parserRegistry, RecursiveChunkingStrategy chunkingStrategy,
            StorageService storageService, EmbeddingProvider embeddingProvider, GeminiProperties geminiProperties) {
        this.resourceRepository = resourceRepository;
        this.chunkRepository = chunkRepository;
        this.parserRegistry = parserRegistry;
        this.chunkingStrategy = chunkingStrategy;
        this.storageService = storageService;
        this.embeddingProvider = embeddingProvider;
        this.geminiProperties = geminiProperties;
    }

    @Async
    @TransactionalEventListener
    public void processAfterUpload(ResourceProcessingRequestedEvent event) {
        process(event.resourceId());
    }

    @Transactional
    public void process(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || resource.getProcessingStatus().isTerminal()) {
            return;
        }
        try {
            resource.beginParsing();
            ParsedResourceContent parsed = parse(resource);
            List<String> pieces = chunkingStrategy.chunk(parsed.content());
            if (pieces.isEmpty()) {
                throw new IllegalArgumentException("No readable text was found in this resource.");
            }
            resource.beginChunking();
            chunkRepository.deleteByResourceId(resource.getId());
            List<DocumentChunk> chunks = new java.util.ArrayList<>();
            for (int index = 0; index < pieces.size(); index++) {
                chunks.add(new DocumentChunk(resource, index, parsed.pageNumber(), parsed.section(), pieces.get(index)));
            }
            chunkRepository.saveAll(chunks);

            resource.beginEmbedding();
            for (DocumentChunk chunk : chunks) {
                chunk.embed(embeddingProvider.embedDocument(chunk.getContent()), geminiProperties.embeddingModel());
            }
            resource.markReady();
        } catch (Exception exception) {
            resource.markFailed(safeMessage(exception));
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
