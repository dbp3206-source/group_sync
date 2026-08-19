package com.groupsync.backend.knowledge.chunking;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy.HierarchicalChunk;
import com.groupsync.backend.knowledge.ingestion.BlockType;
import com.groupsync.backend.knowledge.ingestion.ParsedBlock;
import com.groupsync.backend.knowledge.ingestion.ParsedDocument;
import com.groupsync.backend.knowledge.model.ChunkLevel;

class StructureAwareChunkingStrategyTest {

    private StructureAwareChunkingStrategy strategy;

    @BeforeEach
    void setUp() {
        RecursiveChunkingStrategy recursive = new RecursiveChunkingStrategy();
        strategy = new StructureAwareChunkingStrategy(1500, 500, 80, recursive);
    }

    @Test
    void headingIntegrity_preservesSectionTitleAcrossChildChunks() {
        ParsedDocument doc = new ParsedDocument("RAG Architecture", "full", List.of(
                new ParsedBlock(BlockType.HEADING, "Metadata Filtering", "## Metadata Filtering", 1, 0),
                new ParsedBlock(BlockType.PARAGRAPH, "Metadata Filtering", "Metadata filtering restricts candidate chunks using structured SQL constraints.", 1, 1),
                new ParsedBlock(BlockType.HEADING, "Parent-Child Retrieval", "## Parent-Child Retrieval", 2, 2),
                new ParsedBlock(BlockType.PARAGRAPH, "Parent-Child Retrieval", "Parent-child retrieval separates indexed chunks from contextual synthesis chunks.", 2, 3)
        ));

        List<HierarchicalChunk> chunks = strategy.chunkDocument(doc);
        assertFalse(chunks.isEmpty());

        List<HierarchicalChunk> parents = chunks.stream().filter(c -> c.level() == ChunkLevel.PARENT).toList();
        List<HierarchicalChunk> children = chunks.stream().filter(c -> c.level() == ChunkLevel.CHILD).toList();

        assertFalse(parents.isEmpty(), "Must contain parent chunks");
        assertFalse(children.isEmpty(), "Must contain child chunks");

        // Verify child chunks have parent references and correct section names
        for (HierarchicalChunk child : children) {
            assertNotNull(child.parentIndex(), "Child chunk must reference parentIndex");
            assertNotNull(child.sectionTitle(), "Child chunk must preserve section title");
        }
    }

    @Test
    void oversizedSection_recursivelySplitsIntoMultipleChildChunks() {
        String longParagraph = "This is a detailed analysis of distributed vector search indexing and cosine similarity. ".repeat(25); // ~2200 chars
        ParsedDocument doc = new ParsedDocument("Large Doc", longParagraph, List.of(
                new ParsedBlock(BlockType.HEADING, "Vector Search Deep Dive", "## Vector Search Deep Dive", 1, 0),
                new ParsedBlock(BlockType.PARAGRAPH, "Vector Search Deep Dive", longParagraph, 1, 1)
        ));

        List<HierarchicalChunk> chunks = strategy.chunkDocument(doc);
        List<HierarchicalChunk> children = chunks.stream().filter(c -> c.level() == ChunkLevel.CHILD).toList();

        assertTrue(children.size() > 1, "Oversized paragraph must be split into multiple child chunks");
        for (HierarchicalChunk child : children) {
            assertTrue(child.content().length() <= 1000, "Individual child chunk should not exceed maximum size threshold");
            assertEquals("Vector Search Deep Dive", child.sectionTitle());
        }
    }

    @Test
    void smallNote_doesNotFragmentUnnecessarily() {
        ParsedDocument doc = new ParsedDocument("Quick Note", "Brief note on RRF formula.", List.of(
                new ParsedBlock(BlockType.PARAGRAPH, null, "Brief note on RRF formula.", null, 0)
        ));

        List<HierarchicalChunk> chunks = strategy.chunkDocument(doc);
        assertEquals(2, chunks.size(), "Small note should produce exactly 1 parent and 1 child chunk");
        assertEquals(ChunkLevel.PARENT, chunks.get(0).level());
        assertEquals(ChunkLevel.CHILD, chunks.get(1).level());
        assertEquals(chunks.get(0).index(), chunks.get(1).parentIndex());
        assertEquals("Brief note on RRF formula.", chunks.get(1).content());
    }

    @Test
    void paragraphBoundary_avoidsCuttingWordsInTheMiddle() {
        String para1 = "First cohesive paragraph discussing HNSW vector index construction.";
        String para2 = "Second paragraph describing BM25 lexical ranking and term frequency.";
        ParsedDocument doc = new ParsedDocument("Two Paragraphs", para1 + "\n\n" + para2, List.of(
                new ParsedBlock(BlockType.PARAGRAPH, "Overview", para1, 1, 0),
                new ParsedBlock(BlockType.PARAGRAPH, "Overview", para2, 1, 1)
        ));

        List<HierarchicalChunk> chunks = strategy.chunkDocument(doc);
        List<HierarchicalChunk> children = chunks.stream().filter(c -> c.level() == ChunkLevel.CHILD).toList();

        assertFalse(children.isEmpty());
        for (HierarchicalChunk child : children) {
            assertFalse(child.content().startsWith(" "), "Child chunk should not have leading whitespace");
            assertFalse(child.content().endsWith(" "), "Child chunk should not have trailing whitespace");
        }
    }
}
