package com.groupsync.backend.calendar.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWeeklyScheduleRequest(
    @NotBlank @Size(max = 160) String title,
    @NotEmpty Set<DayOfWeek> weekdays,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull LocalDate validFrom,
    @NotNull LocalDate validUntil,
    @NotBlank @Size(max = 50) String timezone
) {
}
