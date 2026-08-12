package com.groupsync.backend.calendar.model;

import java.time.Instant;

public record TimeInterval(Instant start, Instant end) {
    public TimeInterval {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Interval end must be after start.");
        }
    }

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }

    public boolean contains(Instant otherStart, Instant otherEnd) {
        return !otherStart.isBefore(start) && !otherEnd.isAfter(end);
    }
}
