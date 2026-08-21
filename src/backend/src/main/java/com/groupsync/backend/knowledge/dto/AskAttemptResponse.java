package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import com.groupsync.backend.knowledge.model.AskAttemptStatus;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

public record AskAttemptResponse(
        Long attemptId,
        Long sessionId,
        Long userMessageId,
        AskAttemptStatus status,
        AskFailureCategory failureCategory,
        Instant createdAt,
        Instant completedAt
) { }
