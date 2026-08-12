package com.groupsync.backend.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.availability.model.AvailabilityCandidate;
import com.groupsync.backend.availability.strategy.MaximumAttendanceStrategy;

class SchedulingStrategyTest {
    @Test
    void maximumAttendanceRanksMoreMembersFirstAndUsesEarliestTieBreak() {
        AvailabilityCandidate lateBest = new AvailabilityCandidate(Instant.parse("2026-08-12T10:00:00Z"), Instant.parse("2026-08-12T11:00:00Z"), List.of(1L, 2L, 3L));
        AvailabilityCandidate earlyTie = new AvailabilityCandidate(Instant.parse("2026-08-12T08:00:00Z"), Instant.parse("2026-08-12T09:00:00Z"), List.of(1L, 2L));
        AvailabilityCandidate lateTie = new AvailabilityCandidate(Instant.parse("2026-08-12T09:00:00Z"), Instant.parse("2026-08-12T10:00:00Z"), List.of(1L, 2L));

        assertThat(new MaximumAttendanceStrategy().rank(List.of(lateTie, lateBest, earlyTie), 2))
            .containsExactly(lateBest, earlyTie, lateTie);
    }
}
