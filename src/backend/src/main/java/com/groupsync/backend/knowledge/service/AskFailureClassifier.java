package com.groupsync.backend.knowledge.service;

import java.util.Locale;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

/** One deterministic failure taxonomy for synchronous and asynchronous Ask paths. */
public final class AskFailureClassifier {
    private AskFailureClassifier() { }

    public static AskFailureCategory classify(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("429") || message.contains("quota") || message.contains("resource_exhausted")) {
                return AskFailureCategory.RATE_LIMIT;
            }
            if (message.contains("timeout") || message.contains("timed out")) {
                return AskFailureCategory.TIMEOUT;
            }
            if (message.contains("retriev") || message.contains("vector") || message.contains("database")) {
                return AskFailureCategory.RETRIEVAL;
            }
            current = current.getCause();
        }
        return error instanceof IllegalArgumentException
                ? AskFailureCategory.VALIDATION
                : AskFailureCategory.PROVIDER;
    }
}
