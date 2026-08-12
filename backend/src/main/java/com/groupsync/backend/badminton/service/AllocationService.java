package com.groupsync.backend.badminton.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.AllocationResponses;
import com.groupsync.backend.badminton.model.AllocationStatus;
import com.groupsync.backend.badminton.model.BadmintonAllocation;
import com.groupsync.backend.badminton.model.BadmintonAllocationPlayer;
import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.repository.BadmintonAllocationRepository;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;

@Service
public class AllocationService {
    private final BadmintonSessionRepository sessionRepository;
    private final BadmintonRegistrationRepository registrationRepository;
    private final BadmintonAllocationRepository allocationRepository;
    private final MembershipRepository membershipRepository;

    public AllocationService(BadmintonSessionRepository sessionRepository, BadmintonRegistrationRepository registrationRepository, BadmintonAllocationRepository allocationRepository, MembershipRepository membershipRepository) { this.sessionRepository = sessionRepository; this.registrationRepository = registrationRepository; this.allocationRepository = allocationRepository; this.membershipRepository = membershipRepository; }

    @Transactional
    public List<AllocationResponses.Allocation> generate(AuthenticatedUser actor, Long sessionId, int roundNumber) {
        BadmintonSession session = organizerSession(actor, sessionId);
        if (roundNumber < 1) throw new ConflictException("Round number must be positive.");
        List<BadmintonAllocation> existing = allocationRepository.findBySessionIdOrderByRoundNumberAscIdAsc(sessionId).stream().filter(a -> a.getRoundNumber() == roundNumber).toList();
        if (existing.stream().anyMatch(a -> a.getStatus() == AllocationStatus.CONFIRMED)) throw new ConflictException("A confirmed allocation cannot be regenerated.");
        allocationRepository.deleteAll(existing);
        List<BadmintonRegistration> checkedIn = registrationRepository.findBySessionIdAndStatusOrderByQueuedAtAscIdAsc(sessionId, RegistrationStatus.CHECKED_IN);
        List<com.groupsync.backend.badminton.model.Court> courts = session.getCourts().stream().filter(com.groupsync.backend.badminton.model.Court::isActive).sorted(Comparator.comparing(com.groupsync.backend.badminton.model.Court::getName)).toList();
        if (courts.isEmpty()) throw new ConflictException("The session has no active courts.");
        List<BadmintonAllocation> allocations = courts.stream().map(c -> allocationRepository.save(new BadmintonAllocation(session, c, roundNumber))).toList();
        for (int i = 0; i < checkedIn.size(); i++) { BadmintonRegistration registration = checkedIn.get(i); BadmintonAllocation allocation = allocations.get(i % allocations.size()); allocation.addPlayer(new BadmintonAllocationPlayer(allocation, registration.getUser(), allocation.getPlayers().size() + 1)); }
        allocationRepository.saveAll(allocations);
        return allocations.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AllocationResponses.Allocation> confirm(AuthenticatedUser actor, Long sessionId, int roundNumber) {
        organizerSession(actor, sessionId);
        List<BadmintonAllocation> allocations = allocationRepository.findBySessionIdOrderByRoundNumberAscIdAsc(sessionId).stream().filter(a -> a.getRoundNumber() == roundNumber).toList();
        if (allocations.isEmpty()) throw new NotFoundException("Allocation not found.");
        allocations.forEach(BadmintonAllocation::confirm); return allocations.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationResponses.Allocation> list(AuthenticatedUser actor, Long sessionId) { requireMember(sessionId, actor.getId()); return allocationRepository.findBySessionIdOrderByRoundNumberAscIdAsc(sessionId).stream().map(this::toResponse).toList(); }

    private AllocationResponses.Allocation toResponse(BadmintonAllocation allocation) { return new AllocationResponses.Allocation(allocation.getId(), allocation.getCourt().getId(), allocation.getCourt().getName(), allocation.getRoundNumber(), allocation.getStatus().name(), allocation.getPlayers().stream().sorted(Comparator.comparingInt(BadmintonAllocationPlayer::getPosition)).map(p -> new AllocationResponses.Player(p.getUser().getId(), p.getUser().getDisplayName(), p.getPosition())).toList()); }
    private BadmintonSession organizerSession(AuthenticatedUser actor, Long id) { BadmintonSession session = sessionRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Badminton session not found.")); Membership member = membershipRepository.findByGroupIdAndUserId(session.getGroup().getId(), actor.getId()).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); if (member.getRole() == GroupRole.MEMBER) throw new ForbiddenException("Only the owner or an organizer can allocate courts."); return session; }
    private Membership requireMember(Long sessionId, Long userId) { BadmintonSession session = sessionRepository.findByIdForUpdate(sessionId).orElseThrow(() -> new NotFoundException("Badminton session not found.")); return membershipRepository.findByGroupIdAndUserId(session.getGroup().getId(), userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
}
