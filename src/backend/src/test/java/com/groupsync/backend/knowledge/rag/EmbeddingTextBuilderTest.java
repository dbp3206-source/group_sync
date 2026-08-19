package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder.SemanticMetadata;

class EmbeddingTextBuilderTest {

    private EmbeddingTextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new EmbeddingTextBuilder();
    }

    @Test
    void build_includesSemanticMetadataWhenAvailable() {
        SemanticMetadata meta = new SemanticMetadata(
                "Advanced RAG Architecture",
                List.of("AI Agent", "KnowledgeOS"),
                List.of("RAG", "Vectors", "HNSW"),
                "Parent-Child Retrieval"
        );
        String chunkContent = "Parent-child retrieval searches smaller child chunks and expands to parent contexts.";

        String result = builder.build(meta, chunkContent);

        assertTrue(result.contains("Document: Advanced RAG Architecture"));
        assertTrue(result.contains("Collection: AI Agent, KnowledgeOS"));
        assertTrue(result.contains("Tags: RAG, Vectors, HNSW"));
        assertTrue(result.contains("Section: Parent-Child Retrieval"));
        assertTrue(result.contains("Content:\nParent-child retrieval searches smaller child chunks"));

        // Critical safety verification: No logical DB fields
        assertFalse(result.contains("ownerId"));
        assertFalse(result.contains("storageKey"));
        assertFalse(result.contains("checksum"));
        assertFalse(result.contains("fileSize"));
        assertFalse(result.contains("favorite"));
        assertFalse(result.contains("created_at"));
    }

    @Test
    void build_handlesMinimalMetadataGracefully() {
        SemanticMetadata meta = new SemanticMetadata(null, null, null, null);
        String content = "Simple plain text content.";

        String result = builder.build(meta, content);

        assertEquals("Simple plain text content.", result);
        assertFalse(result.contains("Document:"));
        assertFalse(result.contains("Collection:"));
        assertFalse(result.contains("Tags:"));
        assertFalse(result.contains("Section:"));
    }

    @Test
    void build_handlesPartialMetadata() {
        SemanticMetadata meta = new SemanticMetadata(
                "Knowledge Base",
                List.of(),
                List.of("Tutorial"),
                null
        );
        String content = "How to configure PostgreSQL.";

        String result = builder.build(meta, content);

        assertTrue(result.contains("Document: Knowledge Base"));
        assertFalse(result.contains("Collection:"));
        assertTrue(result.contains("Tags: Tutorial"));
        assertFalse(result.contains("Section:"));
        assertTrue(result.contains("How to configure PostgreSQL."));
    }
}
