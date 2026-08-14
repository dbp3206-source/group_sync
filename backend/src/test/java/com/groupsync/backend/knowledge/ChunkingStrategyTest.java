package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.chunking.ParagraphChunkingStrategy;
import com.groupsync.backend.knowledge.chunking.RecursiveChunkingStrategy;

class ChunkingStrategyTest {
    @Test void paragraphStrategyKeepsMeaningfulParagraphs() { assertEquals(2, new ParagraphChunkingStrategy().chunk("First paragraph.\n\nSecond paragraph.").size()); }
    @Test void recursiveStrategyProducesNonEmptyChunks() { var chunks = new RecursiveChunkingStrategy().chunk("word ".repeat(600)); assertFalse(chunks.isEmpty()); assertFalse(chunks.stream().anyMatch(String::isBlank)); }
}
