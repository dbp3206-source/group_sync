package com.groupsync.backend.availability.model;

import java.time.Instant;
import java.util.List;

public record AvailabilityCandidate(Instant start, Instant end, List<Long> availableMemberIds) {
    public int attendance() { return availableMemberIds.size(); }
}
