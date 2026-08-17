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
    @NotBlank @Size(max = 50) String timezone,
    @Size(max = 500) String description,
    @Size(max = 40) String category,
    @Size(max = 200) String location,
    @Size(max = 20) String visibility,
    Integer reminderMinutes,
    @Size(max = 20) String frequency
) {
}
