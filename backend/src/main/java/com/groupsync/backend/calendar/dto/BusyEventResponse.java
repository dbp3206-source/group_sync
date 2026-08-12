package com.groupsync.backend.calendar.dto;

import java.time.Instant;

import com.groupsync.backend.calendar.personal.model.BusyEvent;

public record BusyEventResponse(Long id, String title, Instant start, Instant end) {
    public static BusyEventResponse from(BusyEvent event) {
        return new BusyEventResponse(event.getId(), event.getTitle(), event.getStartAt(), event.getEndAt());
    }
}
