package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record OrganizationSuggestionsResponse(
        Long resourceId,
        List<OrganizationTagSuggestionResponse> suggestedTags,
        List<OrganizationCollectionSuggestionResponse> suggestedCollections,
        List<OrganizationRelatedSuggestionResponse> suggestedRelatedResources
) {}
