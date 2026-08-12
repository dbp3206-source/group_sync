package com.groupsync.backend.availability.strategy;

import java.util.List;

import com.groupsync.backend.availability.model.AvailabilityCandidate;

public interface SchedulingStrategy {
    List<AvailabilityCandidate> rank(List<AvailabilityCandidate> candidates, int minimumAttendance);
}
