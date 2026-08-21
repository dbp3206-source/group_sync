package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.service.RepresentativeEvidenceSelector.EvidenceChunk;

class RepresentativeEvidenceSelectorTest {
    private final RepresentativeEvidenceSelector selector = new RepresentativeEvidenceSelector();

    @Test void selectionIsNotFirstNOnly() {
        List<EvidenceChunk> chunks = chunks(24);
        List<EvidenceChunk> selected = selector.select(chunks, 8);
        assertTrue(selected.stream().anyMatch(chunk -> chunk.chunkIndex() >= 18));
        assertFalse(selected.stream().map(EvidenceChunk::chunkIndex).toList().equals(List.of(0,1,2,3,4,5,6,7)));
    }

    @Test void selectionIncludesEarlyMiddleAndLateRegions() {
        List<Integer> indexes = selector.select(chunks(20), 8).stream().map(EvidenceChunk::chunkIndex).toList();
        assertTrue(indexes.stream().anyMatch(index -> index <= 2));
        assertTrue(indexes.stream().anyMatch(index -> index >= 8 && index <= 12));
        assertTrue(indexes.stream().anyMatch(index -> index >= 17));
    }

    @Test void selectionIsDeterministicAndBounded() {
        assertEquals(selector.select(chunks(30), 7), selector.select(chunks(30), 7));
        assertEquals(7, selector.select(chunks(30), 7).size());
    }

    private List<EvidenceChunk> chunks(int count) {
        List<EvidenceChunk> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(new EvidenceChunk((long) i + 1, i, "Section " + (i / 4), "Informative content for region " + i + " with source facts and details."));
        return result;
    }
}
