package com.groupsync.backend.calendar.aggregation;

import java.time.Instant;
import java.util.List;

import com.groupsync.backend.calendar.model.CalendarItem;

public interface CalendarSource {
    List<CalendarItem> getItems(Long userId, Instant from, Instant to);
}
