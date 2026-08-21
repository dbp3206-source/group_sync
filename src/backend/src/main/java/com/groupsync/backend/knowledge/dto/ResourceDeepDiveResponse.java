package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import java.util.List;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.DeepDiveAreaDto;

public record ResourceDeepDiveResponse(
        boolean available,
        Long topicId,
        String topicTitle,
        String goal,
        String topicStatus,
        int conceptCount,
        int checkedCount,
        int reviewNeededCount,
        int learningCount,
        int notStartedCount,
        Instant updatedAt,
        List<DeepDiveAreaDto> learningAreas) {

    public static ResourceDeepDiveResponse unavailable() {
        return new ResourceDeepDiveResponse(false, null, null, null, null, 0, 0, 0, 0, 0, null, List.of());
    }
}
