package com.groupsync.backend.availability.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AvailabilityRequest(
    @NotNull Instant from,
    @NotNull Instant to,
    @Positive Integer durationMinutes,
    List<Long> requiredMemberIds,
    Integer minimumAttendance,
    String strategy
) { }
