package com.groupsync.backend.availability.strategy;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.groupsync.backend.availability.model.AvailabilityCandidate;

@Component("earliestPossibleStrategy")
public class EarliestPossibleStrategy implements SchedulingStrategy {
    @Override
    public List<AvailabilityCandidate> rank(List<AvailabilityCandidate> candidates, int minimumAttendance) {
        return candidates.stream()
            .filter(candidate -> candidate.attendance() >= minimumAttendance)
            .sorted(Comparator.comparing(AvailabilityCandidate::start))
            .toList();
    }
}
