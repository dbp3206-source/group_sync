package com.groupsync.backend.availability.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.availability.dto.AvailabilityCandidateResponse;
import com.groupsync.backend.availability.dto.AvailabilityRequest;
import com.groupsync.backend.availability.model.AvailabilityCandidate;
import com.groupsync.backend.availability.strategy.EarliestPossibleStrategy;
import com.groupsync.backend.availability.strategy.MaximumAttendanceStrategy;
import com.groupsync.backend.availability.strategy.SchedulingStrategy;
import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.service.CalendarService;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ForbiddenException;

@Service
public class AvailabilityService {
    private final MembershipRepository membershipRepository;
    private final CalendarService calendarService;
    private final MaximumAttendanceStrategy maximumAttendanceStrategy;
    private final EarliestPossibleStrategy earliestPossibleStrategy;

    public AvailabilityService(MembershipRepository membershipRepository, CalendarService calendarService, MaximumAttendanceStrategy maximumAttendanceStrategy, EarliestPossibleStrategy earliestPossibleStrategy) {
        this.membershipRepository = membershipRepository; this.calendarService = calendarService; this.maximumAttendanceStrategy = maximumAttendanceStrategy; this.earliestPossibleStrategy = earliestPossibleStrategy;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityCandidateResponse> find(AuthenticatedUser actor, Long groupId, AvailabilityRequest request) {
        Membership actorMembership = membershipRepository.findByGroupIdAndUserId(groupId, actor.getId()).orElseThrow(() -> new ForbiddenException("You are not a member of this group."));
        if (actorMembership.getRole() != GroupRole.OWNER && actorMembership.getRole() != GroupRole.ORGANIZER) throw new ForbiddenException("Only the owner or an organizer can search group availability.");
        validate(request);
        List<Long> memberIds = membershipRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream().map(member -> member.getUser().getId()).toList();
        Set<Long> memberSet = new HashSet<>(memberIds);
        List<Long> required = request.requiredMemberIds() == null ? List.of() : request.requiredMemberIds();
        if (!memberSet.containsAll(required)) throw new BadRequestException("Required users must be active group members.");
        int minimumAttendance = request.minimumAttendance() == null ? required.size() : request.minimumAttendance();
        if (minimumAttendance < required.size() || minimumAttendance > memberIds.size()) throw new BadRequestException("Minimum attendance is outside the group member range.");

        List<List<CalendarItem>> busyItems = memberIds.stream().map(memberId -> calendarService.getItemsForUser(memberId, request.from(), request.to())).toList();
        List<AvailabilityCandidate> candidates = new ArrayList<>();
        Duration step = Duration.ofMinutes(30);
        Duration duration = Duration.ofMinutes(request.durationMinutes());
        for (Instant start = request.from(); !start.plus(duration).isAfter(request.to()); start = start.plus(step)) {
            Instant slotStart = start;
            Instant end = slotStart.plus(duration);
            List<Long> available = new ArrayList<>();
            for (int index = 0; index < memberIds.size(); index++) {
                boolean busy = busyItems.get(index).stream().anyMatch(item -> item.busy() && item.start().isBefore(end) && slotStart.isBefore(item.end()));
                if (!busy) available.add(memberIds.get(index));
            }
            if (available.containsAll(required)) candidates.add(new AvailabilityCandidate(slotStart, end, available));
        }
        SchedulingStrategy strategy = "EARLIEST".equalsIgnoreCase(request.strategy()) ? earliestPossibleStrategy : maximumAttendanceStrategy;
        return strategy.rank(candidates, minimumAttendance).stream().map(AvailabilityCandidateResponse::from).toList();
    }

    private void validate(AvailabilityRequest request) {
        if (request.from() == null || request.to() == null || !request.from().isBefore(request.to())) throw new BadRequestException("Availability end must be after start.");
        if (Duration.between(request.from(), request.to()).toDays() > 14) throw new BadRequestException("Availability range cannot exceed 14 days.");
        if (request.durationMinutes() == null || request.durationMinutes() % 30 != 0) throw new BadRequestException("Duration must be a positive multiple of 30 minutes.");
        if (request.from().getNano() != 0 || request.to().getNano() != 0 || Math.floorMod(request.from().getEpochSecond(), 60) != 0 || Math.floorMod(request.to().getEpochSecond(), 60) != 0) throw new BadRequestException("Availability range must use whole minutes.");
    }
}
