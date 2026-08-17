package com.groupsync.backend.badminton.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.AllocationResponses;
import com.groupsync.backend.badminton.model.BadmintonProfile;
import com.groupsync.backend.badminton.model.BadmintonSkillLevel;
import com.groupsync.backend.badminton.model.BadmintonAllocation;
import com.groupsync.backend.badminton.model.PairingStrategyType;
import com.groupsync.backend.badminton.pairing.BalancedPairingStrategy;
import com.groupsync.backend.badminton.pairing.PairingPlayer;
import com.groupsync.backend.badminton.pairing.PairingStrategy;
import com.groupsync.backend.badminton.pairing.PairingSuggestion;
import com.groupsync.backend.badminton.pairing.RandomPairingStrategy;
import com.groupsync.backend.badminton.repository.BadmintonAllocationRepository;
import com.groupsync.backend.badminton.repository.BadmintonProfileRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;

@Service
public class PairingService {
    private final BadmintonAllocationRepository allocationRepository; private final BadmintonSessionRepository sessionRepository; private final MembershipRepository membershipRepository; private final BadmintonProfileRepository profileRepository;
    public PairingService(BadmintonAllocationRepository allocationRepository, BadmintonSessionRepository sessionRepository, MembershipRepository membershipRepository, BadmintonProfileRepository profileRepository) { this.allocationRepository = allocationRepository; this.sessionRepository = sessionRepository; this.membershipRepository = membershipRepository; this.profileRepository = profileRepository; }

    @Transactional(readOnly = true)
    public List<AllocationResponses.Pairing> suggest(AuthenticatedUser actor, Long sessionId, int roundNumber, PairingStrategyType type, long seed) {
        var session = sessionRepository.findForOperations(sessionId).orElseThrow(() -> new NotFoundException("Badminton session not found."));
        membershipRepository.findByGroupIdAndUserId(session.getGroup().getId(), actor.getId()).orElseThrow(() -> new com.groupsync.backend.shared.exception.ForbiddenException("You are not a member of this group."));
        if (type == PairingStrategyType.MANUAL) throw new BadRequestException("Manual pairing is supplied when creating a match.");
        PairingStrategy strategy = type == PairingStrategyType.RANDOM ? new RandomPairingStrategy() : new BalancedPairingStrategy();
        return allocationRepository.findBySessionIdOrderByRoundNumberAscIdAsc(sessionId).stream().filter(a -> a.getRoundNumber() == roundNumber).map(a -> toResponse(a, type, strategy.create(players(a), seed))).toList();
    }

    private List<PairingPlayer> players(BadmintonAllocation allocation) { return allocation.getPlayers().stream().sorted(java.util.Comparator.comparingInt(com.groupsync.backend.badminton.model.BadmintonAllocationPlayer::getPosition)).map(p -> { var membership = membershipRepository.findByGroupIdAndUserId(allocation.getSession().getGroup().getId(), p.getUser().getId()).orElseThrow(); BadmintonProfile profile = profileRepository.findByMembershipId(membership.getId()).orElse(null); return new PairingPlayer(p.getUser().getId(), p.getUser().getDisplayName(), weight(profile)); }).toList(); }
    private int weight(BadmintonProfile profile) { if (profile == null) return 1; return switch (profile.getSkillLevel()) { case BEGINNER -> 1; case INTERMEDIATE -> 2; case ADVANCED -> 3; }; }
    private AllocationResponses.Pairing toResponse(BadmintonAllocation allocation, PairingStrategyType type, PairingSuggestion suggestion) { return new AllocationResponses.Pairing(allocation.getCourt().getId(), allocation.getCourt().getName(), allocation.getRoundNumber(), type.name(), map(suggestion.sideA()), map(suggestion.sideB()), map(suggestion.unassigned())); }
    private List<AllocationResponses.PairingPlayer> map(List<PairingPlayer> players) { return players.stream().map(p -> new AllocationResponses.PairingPlayer(p.userId(), p.displayName())).toList(); }
}
