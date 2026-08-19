package com.groupsync.backend.knowledge.dto;

import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.knowledge.rag.QueryOperation;

public record RagExecutionTrace(
        QueryMode mode,
        QueryOperation operation,
        PlannerTrace planner,
        FilterTrace filter,
        RetrievalTrace retrieval,
        FusionTrace fusion,
        ParentChildTrace parentChild,
        ContextBudgetTrace contextBudget,
        GenerationTrace generation,
        long durationMs
) {
    public static RagExecutionTrace forStructured(
            PlannerTrace planner,
            FilterTrace filter,
            long durationMs
    ) {
        return new RagExecutionTrace(
                QueryMode.STRUCTURED,
                planner != null ? planner.operation() : QueryOperation.COUNT,
                planner,
                filter,
                null,
                null,
                null,
                null,
                null,
                durationMs
        );
    }
}
