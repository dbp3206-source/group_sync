package com.groupsync.backend.calendar.dto;

import java.time.Instant;

import com.groupsync.backend.calendar.model.CalendarItem;

public record CalendarItemResponse(String sourceType, Long sourceId, String title, Instant start, Instant end, boolean busy) {
    public static CalendarItemResponse from(CalendarItem item) {
        return new CalendarItemResponse(item.sourceType().name(), item.sourceId(), item.title(), item.start(), item.end(), item.busy());
    }
}
