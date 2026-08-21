package com.groupsync.backend.knowledge.dto;

public record AskTraceTechnicalDetails(
        String mode,
        String operation,
        Integer semanticCandidates,
        Integer lexicalCandidates,
        Integer totalCandidates,
        Integer selectedChildren,
        Integer parentsUsed,
        Integer charactersUsed,
        Integer maxCharactersBudget,
        Integer citationsVerified,
        String model,
        String failureCategory
) { }
