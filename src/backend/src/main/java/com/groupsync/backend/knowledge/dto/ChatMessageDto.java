package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import java.util.List;
import com.groupsync.backend.knowledge.model.AskFailureCategory;
import com.groupsync.backend.knowledge.model.ChatMessageStatus;

public record ChatMessageDto(
        Long id,
        String role,
        String content,
        Instant createdAt,
        List<CitationResponse> citations,
        ChatMessageStatus status,
        AskFailureCategory failureCategory
) {}
