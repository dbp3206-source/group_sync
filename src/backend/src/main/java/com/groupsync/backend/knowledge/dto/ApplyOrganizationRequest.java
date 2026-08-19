package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record ApplyOrganizationRequest(
        List<String> tagNames,
        List<Long> collectionIds,
        List<String> newCollectionNames,
        List<Long> relatedResourceIds
) {
    public ApplyOrganizationRequest {
        tagNames = tagNames == null ? List.of() : tagNames.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList();
        collectionIds = collectionIds == null ? List.of() : collectionIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        newCollectionNames = newCollectionNames == null ? List.of() : newCollectionNames.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList();
        relatedResourceIds = relatedResourceIds == null ? List.of() : relatedResourceIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    }
}
