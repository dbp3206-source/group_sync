package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record InsightOverviewResponse(long totalResources, long readyResources, long inProgressResources,
        long completedResources, List<InsightTopicCount> composition) {
    public record InsightTopicCount(String resourceType, long count) { }
}
