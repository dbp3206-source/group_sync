package com.groupsync.backend.knowledge.model;

public enum AskFailureCategory {
    RATE_LIMIT,
    TIMEOUT,
    RETRIEVAL,
    PROVIDER,
    VALIDATION,
    INTERRUPTED,
    UNKNOWN
}
