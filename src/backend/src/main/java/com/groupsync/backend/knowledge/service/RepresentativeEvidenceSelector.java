package com.groupsync.backend.knowledge.service;

import java.util.*;

/** Deterministically samples source evidence across the whole document. */
public class RepresentativeEvidenceSelector {
    public static final int DEFAULT_LIMIT = 10;

    public record EvidenceChunk(Long id, int chunkIndex, String section, String content) {
        public EvidenceChunk {
            section = section == null ? "" : section.trim();
            content = content == null ? "" : content.trim();
        }
    }

    public List<EvidenceChunk> select(List<EvidenceChunk> source) {
        return select(source, DEFAULT_LIMIT);
    }

    public List<EvidenceChunk> select(List<EvidenceChunk> source, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) return List.of();
        List<EvidenceChunk> ordered = source.stream()
                .filter(chunk -> chunk != null && chunk.id() != null && !chunk.content().isBlank())
                .sorted(Comparator.comparingInt(EvidenceChunk::chunkIndex))
                .toList();
        if (ordered.size() <= limit) return ordered;

        LinkedHashMap<Long, EvidenceChunk> selected = new LinkedHashMap<>();
        int last = ordered.size() - 1;
        add(selected, ordered.get(0));
        add(selected, ordered.get(last / 2));
        add(selected, ordered.get(last));
        add(selected, ordered.get((int) Math.round(last * 0.25d)));
        add(selected, ordered.get((int) Math.round(last * 0.75d)));

        Set<String> sections = new HashSet<>();
        for (EvidenceChunk chunk : ordered) {
            String section = chunk.section().toLowerCase(Locale.ROOT);
            if (!section.isBlank() && sections.add(section)) add(selected, chunk);
            if (selected.size() >= limit) break;
        }

        ordered.stream()
                .sorted(Comparator.comparingInt(RepresentativeEvidenceSelector::informationScore).reversed()
                        .thenComparingInt(EvidenceChunk::chunkIndex))
                .forEach(chunk -> {
                    if (selected.size() < limit) add(selected, chunk);
                });

        return selected.values().stream()
                .sorted(Comparator.comparingInt(EvidenceChunk::chunkIndex))
                .limit(limit)
                .toList();
    }

    private static void add(Map<Long, EvidenceChunk> selected, EvidenceChunk chunk) {
        selected.putIfAbsent(chunk.id(), chunk);
    }

    private static int informationScore(EvidenceChunk chunk) {
        int length = Math.min(chunk.content().length(), 1200);
        int heading = chunk.section().isBlank() ? 0 : 350;
        int structure = chunk.content().contains(":") || chunk.content().contains("\n") ? 100 : 0;
        return length + heading + structure;
    }
}
