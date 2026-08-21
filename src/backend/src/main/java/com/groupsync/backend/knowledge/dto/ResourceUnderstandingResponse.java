package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import java.util.List;

public record ResourceUnderstandingResponse(
        String status,
        String normalizedTitle,
        String summary,
        List<String> keyIdeas,
        List<String> broadThemes,
        int evidenceCount,
        Instant updatedAt) {

    public ResourceUnderstandingResponse {
        keyIdeas = keyIdeas == null ? List.of() : List.copyOf(keyIdeas);
        broadThemes = broadThemes == null ? List.of() : List.copyOf(broadThemes);
    }
}
