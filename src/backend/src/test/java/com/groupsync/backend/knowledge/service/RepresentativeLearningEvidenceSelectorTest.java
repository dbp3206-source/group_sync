package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.EvidenceChunk;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.ResourceSnapshot;

class RepresentativeLearningEvidenceSelectorTest {
    private final RepresentativeLearningEvidenceSelector selector = new RepresentativeLearningEvidenceSelector();

    @Test
    void evidenceSelectionIsNotFirstTwelveOnly() {
        ResourceSnapshot resource = resource(1L, 100, Set.of());
        List<EvidenceChunk> selected = selector.select(List.of(resource));
        assertTrue(selected.stream().anyMatch(chunk -> chunk.chunkIndex() >= 50));
        assertTrue(selected.stream().anyMatch(chunk -> chunk.chunkIndex() == 99));
        assertTrue(selected.size() <= RepresentativeLearningEvidenceSelector.MAX_PER_RESOURCE);
    }

    @Test
    void verifiedUnderstandingEvidenceIsPreferred() {
        ResourceSnapshot resource = resource(1L, 20, Set.of(1015L));
        assertTrue(selector.select(List.of(resource)).stream().anyMatch(chunk -> chunk.chunkId() == 1015L));
    }

    @Test
    void multiDocumentSelectionRepresentsEveryDocumentWhenBoundAllows() {
        List<ResourceSnapshot> resources = List.of(resource(1L, 100, Set.of()), resource(2L, 10, Set.of()), resource(3L, 30, Set.of()));
        Set<Long> represented = new HashSet<>();
        selector.select(resources).forEach(chunk -> represented.add(chunk.resourceId()));
        assertEquals(Set.of(1L, 2L, 3L), represented);
    }

    @Test
    void aLargeDocumentCannotDominateSmallerDocuments() {
        List<EvidenceChunk> selected = selector.select(List.of(resource(1L, 100, Set.of()), resource(2L, 2, Set.of()), resource(3L, 3, Set.of())));
        long largeCount = selected.stream().filter(chunk -> chunk.resourceId() == 1L).count();
        assertTrue(largeCount <= RepresentativeLearningEvidenceSelector.MAX_PER_RESOURCE);
        assertTrue(selected.stream().anyMatch(chunk -> chunk.resourceId() == 2L));
        assertTrue(selected.stream().anyMatch(chunk -> chunk.resourceId() == 3L));
    }

    @Test
    void totalEvidenceIsBoundedForLargeCollections() {
        List<ResourceSnapshot> resources = java.util.stream.LongStream.rangeClosed(1, 50).mapToObj(id -> resource(id, 8, Set.of())).toList();
        assertTrue(selector.select(resources).size() <= RepresentativeLearningEvidenceSelector.MAX_TOTAL_CHUNKS);
    }

    @Test
    void largeCollectionSamplingSpansTheWholeCollectionInsteadOfTakingFirstResources() {
        List<ResourceSnapshot> resources = java.util.stream.LongStream.rangeClosed(1, 50).mapToObj(id -> resource(id, 8, Set.of())).toList();
        Set<Long> represented = new LinkedHashSet<>();
        selector.select(resources).forEach(chunk -> represented.add(chunk.resourceId()));
        assertEquals(RepresentativeLearningEvidenceSelector.MAX_TOTAL_CHUNKS, represented.size());
        assertTrue(represented.contains(1L));
        assertTrue(represented.contains(50L));
        assertTrue(represented.stream().anyMatch(id -> id >= 24L && id <= 27L));
        assertTrue(represented.stream().anyMatch(id -> id > 32L));
    }

    @Test
    void blankChunksAreNeverSelected() {
        ResourceSnapshot resource = new ResourceSnapshot(1L, "Doc", "PDF", "sum", 1L, "v1", "Doc", "Summary",
                List.of(), List.of(), List.of(), Set.of(), List.of(new EvidenceChunk(1L, 1L, 0, "A", "  "), new EvidenceChunk(2L, 1L, 1, "B", "Valid")));
        assertEquals(List.of(2L), selector.select(List.of(resource)).stream().map(EvidenceChunk::chunkId).toList());
    }

    private ResourceSnapshot resource(Long id, int count, Set<Long> verified) {
        List<EvidenceChunk> chunks = new ArrayList<>();
        for (int index = 0; index < count; index++) chunks.add(new EvidenceChunk(id * 1000 + index, id, index, "Section " + index, "Evidence " + index));
        return new ResourceSnapshot(id, "Resource " + id, "PDF", "checksum" + id, id, "v1", "Resource " + id,
                "Summary", List.of("Idea"), List.of("Theme"), List.of("semantic-tag"), verified, chunks);
    }
}
