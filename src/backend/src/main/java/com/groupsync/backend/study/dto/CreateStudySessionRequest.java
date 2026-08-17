package com.groupsync.backend.study.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateStudySessionRequest(
    @NotBlank @Size(max = 160) String topic,
    @Size(max = 500) String goal,
    @Size(max = 240) String location,
    @NotNull Instant start,
    @NotNull Instant end,
    @Positive Integer capacity
) { }
