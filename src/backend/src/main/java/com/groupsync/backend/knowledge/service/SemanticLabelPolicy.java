package com.groupsync.backend.knowledge.service;

import java.text.Normalizer;
import java.util.*;

public final class SemanticLabelPolicy {
    private static final Set<String> VAGUE = Set.of(
            "document", "information", "chapter", "study", "content", "important", "final", "pdf",
            "file", "notes", "general", "general knowledge", "tai lieu", "tài liệu", "noi dung", "nội dung");
    private static final Map<String, String> EQUIVALENCE_KEYS = Map.ofEntries(
            Map.entry("rag", "retrieval augmented generation"),
            Map.entry("retrieval augmented generation", "retrieval augmented generation"),
            Map.entry("oop", "object oriented programming"),
            Map.entry("object oriented programming", "object oriented programming"),
            Map.entry("postgres", "postgresql"),
            Map.entry("postgresql", "postgresql"));

    private SemanticLabelPolicy() { }

    public static List<String> usefulTags(List<String> candidates) {
        if (candidates == null) return List.of();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String candidate : candidates) {
            String display = cleanDisplay(candidate);
            String normalized = normalize(display);
            if (display == null || normalized.length() < 2 || VAGUE.contains(normalized)) continue;
            result.putIfAbsent(equivalenceKey(display), display);
            if (result.size() >= 6) break;
        }
        return List.copyOf(result.values());
    }

    public static String equivalenceKey(String value) {
        String normalized = normalize(value);
        return EQUIVALENCE_KEYS.getOrDefault(normalized, normalized);
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('&', ' ').replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String cleanDisplay(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        return clean.substring(0, Math.min(80, clean.length()));
    }
}
