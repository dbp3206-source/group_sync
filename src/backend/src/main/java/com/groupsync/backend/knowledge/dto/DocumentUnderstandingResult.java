package com.groupsync.backend.knowledge.dto;

import java.util.List;

/** Typed, source-grounded semantic artifact shared by Library, Workspace and Focus. */
public record DocumentUnderstandingResult(
        String normalizedTitle,
        String summary,
        List<String> keyIdeas,
        List<String> candidateTags,
        List<String> broadThemes,
        String difficultyOrLevel,
        List<Long> evidenceChunkIds
) {
    public DocumentUnderstandingResult {
        keyIdeas = keyIdeas == null ? List.of() : List.copyOf(keyIdeas);
        candidateTags = candidateTags == null ? List.of() : List.copyOf(candidateTags);
        broadThemes = broadThemes == null ? List.of() : List.copyOf(broadThemes);
        evidenceChunkIds = evidenceChunkIds == null ? List.of() : List.copyOf(evidenceChunkIds);
    }
}
