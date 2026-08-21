package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record OrganizationBatchResult(
        int processed,
        int assigned,
        int suggested,
        int skipped,
        int failed,
        List<SemanticOrganizationResult> results
) {
    public OrganizationBatchResult {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
