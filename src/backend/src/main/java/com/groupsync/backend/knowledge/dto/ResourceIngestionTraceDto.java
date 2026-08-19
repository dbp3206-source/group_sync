package com.groupsync.backend.knowledge.dto;

public record ResourceIngestionTraceDto(
        Long resourceId,
        String resourceTitle,
        String resourceType,
        String processingStatus,
        int chunkingVersion,
        int parentChunkCount,
        int childChunkCount,
        int embeddingBatchCount,
        String embeddingModel,
        int embeddingDimensions,
        boolean semanticMetadataIncluded
) {}
