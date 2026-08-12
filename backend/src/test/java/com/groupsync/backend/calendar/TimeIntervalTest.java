package com.groupsync.backend.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.calendar.model.TimeInterval;

class TimeIntervalTest {
    @Test
    void adjacentIntervalsDoNotOverlap() {
        TimeInterval interval = new TimeInterval(Instant.parse("2026-08-12T09:00:00Z"), Instant.parse("2026-08-12T10:00:00Z"));
        assertThat(interval.overlaps(Instant.parse("2026-08-12T10:00:00Z"), Instant.parse("2026-08-12T11:00:00Z"))).isFalse();
    }

    @Test
    void overlappingIntervalsAreDetected() {
        TimeInterval interval = new TimeInterval(Instant.parse("2026-08-12T09:00:00Z"), Instant.parse("2026-08-12T10:00:00Z"));
        assertThat(interval.overlaps(Instant.parse("2026-08-12T09:30:00Z"), Instant.parse("2026-08-12T10:30:00Z"))).isTrue();
    }
}
