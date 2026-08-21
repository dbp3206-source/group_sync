package com.groupsync.backend.knowledge.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy.HierarchicalChunk;
import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.model.ChunkLevel;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder.SemanticMetadata;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;

/**
 * Dedicated transactional boundary service for resource ingestion.
 * Keeps short, isolated database transactions separate from memory parsing and
 * external AI embedding calls.
 */
@Service
public class ResourceIngestionTransactionService {
    private static final Logger log = LoggerFactory.getLogger(ResourceIngestionTransactionService.class);

    private final ResourceRepository resourceRepository;
    private final DocumentChunkRepository chunkRepository;
    private final GeminiProperties geminiProperties;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ResourceIngestionTransactionService(
            ResourceRepository resourceRepository,
            DocumentChunkRepository chunkRepository,
            GeminiProperties geminiProperties,
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.resourceRepository = resourceRepository;
        this.chunkRepository = chunkRepository;
        this.geminiProperties = geminiProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Atomically claims an UPLOADED resource for processing by transitioning it to PARSING.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long resourceId) {
        if (resourceId == null) return false;
        int updated = resourceRepository.updateStatusIfMatches(resourceId, ResourceProcessingStatus.UPLOADED, ResourceProcessingStatus.PARSING);
        return updated > 0;
    }

    /**
     * Claims a READY or FAILED resource for reindexing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimForReindex(Long resourceId) {
        if (resourceId == null) return false;
        Resource res = resourceRepository.findById(resourceId).orElse(null);
        if (res == null) return false;
        res.beginParsing();
        resourceRepository.save(res);
        return true;
    }

    /**
     * Preloads collection and tag names for a resource in a single short query without N+1 overhead.
     */
    @Transactional(readOnly = true)
    public SemanticMetadata fetchSemanticMetadata(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) {
            return new SemanticMetadata(null, List.of(), List.of(), null);
        }

        MapSqlParameterSource params = new MapSqlParameterSource("resourceId", resourceId);
        List<String> collections = jdbcTemplate.query(
                "SELECT c.name FROM collections c JOIN resource_collections rc ON rc.collection_id = c.id WHERE rc.resource_id = :resourceId ORDER BY c.name",
                params,
                (rs, rowNum) -> rs.getString("name")
        );

        List<String> tags = jdbcTemplate.query(
                "SELECT t.name FROM tags t JOIN resource_tags rt ON rt.tag_id = t.id WHERE rt.resource_id = :resourceId ORDER BY t.name",
                params,
                (rs, rowNum) -> rs.getString("name")
        );

        return new SemanticMetadata(resource.getTitle(), collections, tags, null);
    }

    /**
     * Saves hierarchical parent and child chunks in a short dedicated write transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReadyHierarchical(Long resourceId, List<HierarchicalChunk> hierarchicalChunks,
                                      Map<Integer, float[]> childEmbeddings) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalStateException("Resource not found during final save: " + resourceId));

        resource.beginChunking();
        chunkRepository.deleteByResourceId(resource.getId());

        Map<Integer, DocumentChunk> parentEntityMap = new HashMap<>();
        List<DocumentChunk> parents = new ArrayList<>();

        // Phase 1: Persist parent chunks
        for (HierarchicalChunk hc : hierarchicalChunks) {
            if (hc.level() == ChunkLevel.PARENT) {
                DocumentChunk parent = new DocumentChunk(
                        resource, null, ChunkLevel.PARENT, 2,
                        hc.index(), hc.pageNumber(), hc.sectionTitle(), hc.content()
                );
                parentEntityMap.put(hc.index(), parent);
                parents.add(parent);
            }
        }
        if (!parents.isEmpty()) {
            chunkRepository.saveAll(parents);
        }

        // Phase 2: Persist child chunks with parent linkage and vector embeddings
        List<DocumentChunk> children = new ArrayList<>();
        for (HierarchicalChunk hc : hierarchicalChunks) {
            if (hc.level() == ChunkLevel.CHILD) {
                DocumentChunk parent = hc.parentIndex() != null ? parentEntityMap.get(hc.parentIndex()) : null;
                DocumentChunk child = new DocumentChunk(
                        resource, parent, ChunkLevel.CHILD, 2,
                        hc.index(), hc.pageNumber(), hc.sectionTitle(), hc.content()
                );
                float[] emb = childEmbeddings != null ? childEmbeddings.get(hc.index()) : null;
                if (emb == null) {
                    throw new IllegalStateException("Child chunk index " + hc.index() + " is missing vector embedding.");
                }
                if (emb.length != geminiProperties.embeddingDimensions()) {
                    throw new IllegalStateException("Child chunk index " + hc.index() + " has invalid dimension " + emb.length + " (expected " + geminiProperties.embeddingDimensions() + ").");
                }
                child.embed(emb, geminiProperties.embeddingModel());
                children.add(child);
            }
        }
        if (!children.isEmpty()) {
            chunkRepository.saveAll(children);
        }

        resource.markReady();
        resourceRepository.save(resource);

    }

    /**
     * Backward-compatible saveReady method for legacy flat chunks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReady(Long resourceId, List<String> pieces, List<float[]> embeddings, ParsedResourceContent parsed) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalStateException("Resource not found during final save: " + resourceId));

        resource.beginChunking();
        chunkRepository.deleteByResourceId(resource.getId());

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int index = 0; index < pieces.size(); index++) {
            DocumentChunk chunk = new DocumentChunk(resource, null, ChunkLevel.CHILD, 1, index,
                    parsed.pageNumber(), parsed.section(), pieces.get(index));
            if (index < embeddings.size() && embeddings.get(index) != null) {
                chunk.embed(embeddings.get(index), geminiProperties.embeddingModel());
            }
            chunks.add(chunk);
        }
        chunkRepository.saveAll(chunks);

        resource.markReady();
        resourceRepository.save(resource);

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
