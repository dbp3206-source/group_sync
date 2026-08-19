package com.groupsync.backend.knowledge.dto;

public record RetrievalTrace(
        int semanticCandidates,
        int lexicalCandidates,
        int totalCandidates
) {}
