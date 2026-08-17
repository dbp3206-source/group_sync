package com.groupsync.backend.badminton.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSeasonRequest(@NotBlank @Size(max = 120) String name, @NotNull LocalDate startsOn, LocalDate endsOn, @Size(max = 20) String rankingStrategy) { }
