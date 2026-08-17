package com.groupsync.backend.calendar.personal.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.model.CalendarSourceType;
import com.groupsync.backend.calendar.personal.model.WeeklySchedule;

@Component
public class RecurrenceExpander {
    public List<CalendarItem> expand(WeeklySchedule schedule, Instant from, Instant to) {
        ZoneId zone = ZoneId.of(schedule.getTimezone());
        ZonedDateTime queryStart = from.atZone(zone);
        ZonedDateTime queryEnd = to.atZone(zone);
        LocalDate firstDate = queryStart.toLocalDate();
        LocalDate lastDate = queryEnd.toLocalDate();
        List<CalendarItem> occurrences = new ArrayList<>();

        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            boolean matchesDay = "DAILY".equals(schedule.getFrequency()) || schedule.getWeekdays().contains(date.getDayOfWeek());
            if (date.isBefore(schedule.getValidFrom()) || date.isAfter(schedule.getValidUntil()) || !matchesDay) {
                continue;
            }
            Instant start = date.atTime(schedule.getStartTime()).atZone(zone).toInstant();
            Instant end = date.atTime(schedule.getEndTime()).atZone(zone).toInstant();
            if (start.isBefore(to) && from.isBefore(end)) {
                occurrences.add(new CalendarItem(CalendarSourceType.RECURRING, schedule.getId(), schedule.getTitle(), start, end, true));
            }
        }
        return occurrences;
    }
}
