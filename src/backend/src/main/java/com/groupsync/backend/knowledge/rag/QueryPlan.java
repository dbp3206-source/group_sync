package com.groupsync.backend.knowledge.rag;

/**
 * Strongly typed QueryPlan emitted by KnowledgeQueryPlanner.
 * Represents retrieval intent, execution mode, and validated structured constraints.
 */
public record QueryPlan(
        QueryMode mode,
        QueryOperation operation,
        String semanticQuery,
        KnowledgeQueryFilters filters,
        String explanation
) {
    public static QueryPlan defaultHybrid(String question) {
        return new QueryPlan(
                QueryMode.HYBRID,
                QueryOperation.SEARCH,
                question != null ? question.trim() : "",
                KnowledgeQueryFilters.empty(),
                "Default hybrid search plan"
        );
    }
}
