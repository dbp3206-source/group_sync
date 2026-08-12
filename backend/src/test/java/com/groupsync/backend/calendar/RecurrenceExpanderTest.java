package com.groupsync.backend.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.calendar.personal.model.WeeklySchedule;
import com.groupsync.backend.calendar.personal.service.RecurrenceExpander;
import com.groupsync.backend.user.model.UserAccount;

class RecurrenceExpanderTest {
    @Test
    void expandsEachMatchingWeekdayWithoutPersistingOccurrences() {
        WeeklySchedule schedule = new WeeklySchedule(
            new UserAccount("person@example.com", "hash", "Person"), "OOP class",
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(8, 0), LocalTime.of(10, 0),
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "Asia/Bangkok");

        var occurrences = new RecurrenceExpander().expand(schedule,
            Instant.parse("2026-08-10T00:00:00Z"), Instant.parse("2026-08-17T00:00:00Z"));

        assertThat(occurrences).hasSize(2);
        assertThat(occurrences).allMatch(item -> item.sourceType().name().equals("RECURRING"));
    }

    @Test
    void excludesOccurrenceOutsideValidDateRange() {
        WeeklySchedule schedule = new WeeklySchedule(
            new UserAccount("person@example.com", "hash", "Person"), "Class",
            Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(10, 0),
            LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 17), "Asia/Bangkok");

        assertThat(new RecurrenceExpander().expand(schedule,
            Instant.parse("2026-08-10T00:00:00Z"), Instant.parse("2026-08-17T00:00:00Z"))).isEmpty();
    }
}
