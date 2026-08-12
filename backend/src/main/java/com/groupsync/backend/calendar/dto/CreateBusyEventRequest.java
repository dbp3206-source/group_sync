package com.groupsync.backend.calendar.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBusyEventRequest(
    @NotBlank @Size(max = 160) String title,
    @NotNull Instant start,
    @NotNull Instant end
) {
}
