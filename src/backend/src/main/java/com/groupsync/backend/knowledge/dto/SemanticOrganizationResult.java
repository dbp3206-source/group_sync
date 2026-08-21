package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record SemanticOrganizationResult(
        Long resourceId,
        String understandingStatus,
        List<String> tagsAssigned,
        List<Long> collectionsAssigned,
        List<OrganizationCollectionSuggestionResponse> collectionSuggestions,
        List<OrganizationCollectionSuggestionResponse> newCollectionSuggestions,
        List<String> warnings
) {
    public SemanticOrganizationResult {
        tagsAssigned = tagsAssigned == null ? List.of() : List.copyOf(tagsAssigned);
        collectionsAssigned = collectionsAssigned == null ? List.of() : List.copyOf(collectionsAssigned);
        collectionSuggestions = collectionSuggestions == null ? List.of() : List.copyOf(collectionSuggestions);
        newCollectionSuggestions = newCollectionSuggestions == null ? List.of() : List.copyOf(newCollectionSuggestions);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean assignedAnything() {
        return !tagsAssigned.isEmpty() || !collectionsAssigned.isEmpty();
    }

    public boolean hasSuggestions() {
        return !collectionSuggestions.isEmpty() || !newCollectionSuggestions.isEmpty();
    }
}
