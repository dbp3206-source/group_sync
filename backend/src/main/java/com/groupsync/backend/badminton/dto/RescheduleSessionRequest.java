package com.groupsync.backend.badminton.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record RescheduleSessionRequest(@NotNull OffsetDateTime start, @NotNull OffsetDateTime end, @NotNull OffsetDateTime registrationDeadline) { }
