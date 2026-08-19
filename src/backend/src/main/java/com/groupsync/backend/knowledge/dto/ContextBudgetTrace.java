package com.groupsync.backend.knowledge.dto;

public record ContextBudgetTrace(
        int parentsUsed,
        int charactersUsed,
        int maxCharactersBudget
) {}
