package com.groupsync.backend.shared.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    String code,
    String message,
    Instant timestamp,
    Map<String, String> fieldErrors
) {
    public ApiError(String code, String message) {
        this(code, message, Instant.now(), Map.of());
    }
}
