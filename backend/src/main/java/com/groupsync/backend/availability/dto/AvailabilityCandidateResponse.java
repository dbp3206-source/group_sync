package com.groupsync.backend.availability.dto;

import java.time.Instant;
import java.util.List;

import com.groupsync.backend.availability.model.AvailabilityCandidate;

public record AvailabilityCandidateResponse(Instant start, Instant end, int attendance, List<Long> availableMemberIds) {
    public static AvailabilityCandidateResponse from(AvailabilityCandidate candidate) { return new AvailabilityCandidateResponse(candidate.start(), candidate.end(), candidate.attendance(), candidate.availableMemberIds()); }
}
