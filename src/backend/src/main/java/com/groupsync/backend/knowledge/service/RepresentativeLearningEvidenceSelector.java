package com.groupsync.backend.knowledge.service;

import java.util.*;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.EvidenceChunk;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.ResourceSnapshot;

/** Selects a bounded, deterministic early/middle/late sample for every represented document. */
public final class RepresentativeLearningEvidenceSelector {
    static final int MAX_TOTAL_CHUNKS = 32;
    static final int MAX_PER_RESOURCE = 4;

    public List<EvidenceChunk> select(List<ResourceSnapshot> resources) {
        if (resources == null || resources.isEmpty()) return List.of();
        List<ResourceSnapshot> ordered = resources.stream().sorted(Comparator.comparing(ResourceSnapshot::id)).toList();
        List<ResourceSnapshot> represented = representativeResources(ordered);
        int perResource = Math.max(1, Math.min(MAX_PER_RESOURCE, MAX_TOTAL_CHUNKS / Math.max(1, represented.size())));
        List<EvidenceChunk> selected = new ArrayList<>();
        for (ResourceSnapshot resource : represented) {
            if (selected.size() >= MAX_TOTAL_CHUNKS) break;
            selected.addAll(selectOne(resource, Math.min(perResource, MAX_TOTAL_CHUNKS - selected.size())));
        }
        return List.copyOf(selected);
    }

    private List<ResourceSnapshot> representativeResources(List<ResourceSnapshot> ordered) {
        if (ordered.size() <= MAX_TOTAL_CHUNKS) return ordered;
        List<ResourceSnapshot> represented = new ArrayList<>(MAX_TOTAL_CHUNKS);
        for (int index = 0; index < MAX_TOTAL_CHUNKS; index++) {
            int sourceIndex = (int) Math.round(index * (ordered.size() - 1d) / (MAX_TOTAL_CHUNKS - 1d));
            represented.add(ordered.get(sourceIndex));
        }
        return represented;
    }

    List<EvidenceChunk> selectOne(ResourceSnapshot resource, int limit) {
        if (limit <= 0 || resource.chunks().isEmpty()) return List.of();
        List<EvidenceChunk> chunks = resource.chunks().stream()
                .filter(chunk -> chunk.content() != null && !chunk.content().isBlank())
                .sorted(Comparator.comparingInt(EvidenceChunk::chunkIndex))
                .toList();
        if (chunks.isEmpty()) return List.of();

        LinkedHashMap<Long, EvidenceChunk> chosen = new LinkedHashMap<>();
        for (EvidenceChunk chunk : chunks) {
            if (resource.verifiedEvidenceIds().contains(chunk.chunkId())) chosen.put(chunk.chunkId(), chunk);
            if (chosen.size() >= Math.min(2, limit)) break;
        }
        int[] positions = {0, chunks.size() / 2, chunks.size() - 1};
        for (int position : positions) {
            EvidenceChunk chunk = chunks.get(Math.max(0, Math.min(position, chunks.size() - 1)));
            chosen.putIfAbsent(chunk.chunkId(), chunk);
            if (chosen.size() >= limit) break;
        }
        if (chosen.size() < limit) {
            for (EvidenceChunk chunk : chunks) {
                chosen.putIfAbsent(chunk.chunkId(), chunk);
                if (chosen.size() >= limit) break;
            }
        }
        return chosen.values().stream().limit(limit).toList();
    }
}
