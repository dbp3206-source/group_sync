package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record ChatSessionDetailResponse(
        Long id,
        String title,
        String scope,
        Long collectionId,
        List<Long> resourceIds,
        List<ChatMessageDto> messages
) {}
