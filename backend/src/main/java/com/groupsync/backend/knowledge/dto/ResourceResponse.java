package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.model.ResourceType;
public record ResourceResponse(Long id, String title, String description, ResourceType resourceType, ResourceProcessingStatus processingStatus, String originalFilename, String mimeType, Long sizeBytes, boolean favorite, int priority, String processingError, Instant createdAt, Instant updatedAt) {
    public static ResourceResponse from(Resource resource) { return new ResourceResponse(resource.getId(), resource.getTitle(), resource.getDescription(), resource.getResourceType(), resource.getProcessingStatus(), resource.getOriginalFilename(), resource.getMimeType(), resource.getSizeBytes(), resource.isFavorite(), resource.getPriority(), resource.getProcessingError(), resource.getCreatedAt(), resource.getUpdatedAt()); }
}
