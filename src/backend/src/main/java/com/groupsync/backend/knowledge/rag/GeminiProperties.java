package com.groupsync.backend.knowledge.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        @DefaultValue("gemini-3.5-flash-lite") String chatModel,
        @DefaultValue("gemini-3.5-flash") String qualityModel,
        @DefaultValue("gemini-embedding-001") String embeddingModel,
        @DefaultValue("768") int embeddingDimensions,
        @DefaultValue("5") int ragTopK,
        @DefaultValue("2") int ragCandidateMultiplier,
        @DefaultValue("12") int ragMaxCandidateSize,
        @DefaultValue("60") int ragRrfK
) {
    public int ragCandidateMultiplier() {
        return ragCandidateMultiplier <= 0 ? 2 : ragCandidateMultiplier;
    }

    public int ragMaxCandidateSize() {
        return ragMaxCandidateSize <= 0 ? 12 : ragMaxCandidateSize;
    }

    public int ragRrfK() {
        return ragRrfK <= 0 ? 60 : ragRrfK;
    }
}

