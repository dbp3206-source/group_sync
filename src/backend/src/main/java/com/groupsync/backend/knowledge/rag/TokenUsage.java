package com.groupsync.backend.knowledge.rag;

public record TokenUsage(Integer promptTokens, Integer outputTokens, Integer totalTokens) { }
