package com.groupsync.backend.knowledge.model;

public enum ResourceProcessingStatus {
    UPLOADED, PARSING, CHUNKING, EMBEDDING, READY, FAILED;

    public boolean isTerminal() {
        return this == READY || this == FAILED;
    }
}
