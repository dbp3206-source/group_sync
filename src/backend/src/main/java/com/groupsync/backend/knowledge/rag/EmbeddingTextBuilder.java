package com.groupsync.backend.knowledge.rag;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds rich semantic embedding text for child chunks during ingestion.
 * Includes document title, collection names, tags, and section titles to enrich
 * vector representation with contextual domain semantics.
 * Strictly excludes logical/internal database columns (ownerId, storageKey, SHA-256, etc.).
 */
@Component
public class EmbeddingTextBuilder {

    public record SemanticMetadata(
            String documentTitle,
            List<String> collectionNames,
            List<String> tagNames,
            String sectionTitle
    ) {
        public SemanticMetadata {
            if (collectionNames == null) collectionNames = List.of();
            if (tagNames == null) tagNames = List.of();
        }
    }

    public String build(SemanticMetadata metadata, String chunkContent) {
        StringBuilder sb = new StringBuilder();

        if (metadata != null && metadata.documentTitle() != null && !metadata.documentTitle().isBlank()) {
            sb.append("Document: ").append(metadata.documentTitle().trim()).append("\n");
        }

        if (metadata != null && metadata.collectionNames() != null && !metadata.collectionNames().isEmpty()) {
            sb.append("Collection: ").append(String.join(", ", metadata.collectionNames())).append("\n");
        }

        if (metadata != null && metadata.tagNames() != null && !metadata.tagNames().isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", metadata.tagNames())).append("\n");
        }

        if (metadata != null && metadata.sectionTitle() != null && !metadata.sectionTitle().isBlank()) {
            sb.append("Section: ").append(metadata.sectionTitle().trim()).append("\n");
        }

        if (!sb.isEmpty()) {
            sb.append("\nContent:\n");
        }

        sb.append(chunkContent != null ? chunkContent.trim() : "");
        return sb.toString().trim();
    }
}
