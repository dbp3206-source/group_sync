package com.groupsync.backend.knowledge.dto;

import java.time.Instant;

public record AskTraceEvent(
        Long attemptId,
        long sequence,
        AskTraceStage stage,
        AskTraceStatus status,
        Instant occurredAt,
        long durationMs,
        String beginnerMessage,
        String technicalSummary,
        AskTraceTechnicalDetails technicalDetails
) { }
