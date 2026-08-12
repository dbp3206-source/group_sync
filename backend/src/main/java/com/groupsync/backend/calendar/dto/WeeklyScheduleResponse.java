package com.groupsync.backend.calendar.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import com.groupsync.backend.calendar.personal.model.WeeklySchedule;

public record WeeklyScheduleResponse(Long id, String title, Set<DayOfWeek> weekdays, LocalTime startTime, LocalTime endTime, LocalDate validFrom, LocalDate validUntil, String timezone) {
    public static WeeklyScheduleResponse from(WeeklySchedule schedule) {
        return new WeeklyScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getWeekdays(), schedule.getStartTime(), schedule.getEndTime(), schedule.getValidFrom(), schedule.getValidUntil(), schedule.getTimezone());
    }
}
