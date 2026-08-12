package com.groupsync.backend.study.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record RescheduleStudySessionRequest(@NotNull Instant start, @NotNull Instant end) { }
