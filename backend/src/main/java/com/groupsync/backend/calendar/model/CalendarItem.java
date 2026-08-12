package com.groupsync.backend.calendar.model;

import java.time.Instant;

public record CalendarItem(
    CalendarSourceType sourceType,
    Long sourceId,
    String title,
    Instant start,
    Instant end,
    boolean busy
) {
    public CalendarItem {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Calendar item end must be after start.");
        }
    }
}
