package com.groupsync.backend.knowledge.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkResourceIdsRequest(
        @NotEmpty(message = "Select at least one resource.")
        @Size(max = 100, message = "Select 100 resources or fewer.")
        List<Long> resourceIds
) {}
