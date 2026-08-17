package com.groupsync.backend.badminton.dto;

import java.time.OffsetDateTime;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @NotBlank @Size(max = 160) String title,
    @NotNull OffsetDateTime start,
    @NotNull OffsetDateTime end,
    @NotNull OffsetDateTime registrationDeadline,
    @Min(1) Integer capacity,
    @NotNull Long seasonId,
    @NotNull Long venueId,
    @NotEmpty Set<Long> courtIds
) { }
