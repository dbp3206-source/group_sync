package com.groupsync.backend.badminton.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.MatchRequests.CreateMatchRequest;
import com.groupsync.backend.badminton.dto.MatchRequests.ScoreRequest;
import com.groupsync.backend.badminton.dto.MatchResponses;
import com.groupsync.backend.badminton.event.MatchConfirmedEvent;
import com.groupsync.backend.badminton.model.BadmintonMatch;
import com.groupsync.backend.badminton.model.BadmintonMatchParticipant;
import com.groupsync.backend.badminton.model.BadmintonMatchSide;
import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.MatchSideCode;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.repository.BadmintonMatchRepository;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class MatchService {
    private final BadmintonMatchRepository matchRepository; private final BadmintonSessionRepository sessionRepository; private final BadmintonRegistrationRepository registrationRepository; private final MembershipRepository membershipRepository; private final UserAccountRepository userRepository; private final RankingService rankingService; private final ApplicationEventPublisher events;
    public MatchService(BadmintonMatchRepository matchRepository, BadmintonSessionRepository sessionRepository, BadmintonRegistrationRepository registrationRepository, MembershipRepository membershipRepository, UserAccountRepository userRepository, RankingService rankingService, ApplicationEventPublisher events) { this.matchRepository = matchRepository; this.sessionRepository = sessionRepository; this.registrationRepository = registrationRepository; this.membershipRepository = membershipRepository; this.userRepository = userRepository; this.rankingService = rankingService; this.events = events; }

    @Transactional
    public MatchResponses.Match create(AuthenticatedUser actor, Long sessionId, CreateMatchRequest request) {
        BadmintonSession session = organizerSession(actor, sessionId); requirePlayingSession(session); var court = session.getCourts().stream().filter(c -> c.getId().equals(request.courtId())).findFirst().orElseThrow(() -> new ConflictException("Court does not belong to this session."));
        if (request.sideAUserIds().size() < 1 || request.sideAUserIds().size() > 2 || request.sideBUserIds().size() < 1 || request.sideBUserIds().size() > 2 || request.sideAUserIds().size() != request.sideBUserIds().size()) throw new BadRequestException("A match must have equal 1-player or 2-player sides.");
        Set<Long> all = new HashSet<>(request.sideAUserIds()); all.addAll(request.sideBUserIds()); if (all.size() != request.sideAUserIds().size() + request.sideBUserIds().size()) throw new ConflictException("A player cannot appear on both sides.");
        Set<Long> usedInRound = matchRepository.findBySessionIdOrderByRoundNumberAscIdAsc(sessionId).stream().filter(m -> m.getRoundNumber() == request.roundNumber()).flatMap(m -> m.getSides().stream()).flatMap(s -> s.getParticipants().stream()).map(p -> p.getUser().getId()).collect(Collectors.toSet());
        if (usedInRound.stream().anyMatch(all::contains)) throw new ConflictException("A player cannot appear in more than one match in the same round.");
        for (Long userId : all) { BadmintonRegistration r = registrationRepository.findBySessionIdAndUserId(sessionId, userId).orElseThrow(() -> new ConflictException("Every player must be registered.")); if (r.getStatus() != RegistrationStatus.CHECKED_IN) throw new ConflictException("Only checked-in players can play a match."); }
        BadmintonMatch match = matchRepository.save(new BadmintonMatch(session, court, request.roundNumber()));
        addSide(match, MatchSideCode.A, request.sideAUserIds()); addSide(match, MatchSideCode.B, request.sideBUserIds()); matchRepository.save(match); return toResponse(match);
    }
    @Transactional public MatchResponses.Match start(AuthenticatedUser actor, Long id) { BadmintonMatch match = findMatchForUpdate(id); requireOrganizer(actor, match.getSession().getGroup().getId()); requirePlayingSession(match.getSession()); match.start(); return toResponse(match); }
    @Transactional public MatchResponses.Match submit(AuthenticatedUser actor, Long id, ScoreRequest request) { BadmintonMatch match = findMatchForUpdate(id); boolean participant = match.getSides().stream().flatMap(s -> s.getParticipants().stream()).anyMatch(p -> p.getUser().getId().equals(actor.getId())); if (!participant) requireOrganizer(actor, match.getSession().getGroup().getId()); requirePlayingSession(match.getSession()); match.submitResult(request.scoreA(), request.scoreB()); return toResponse(match); }
    @Transactional public MatchResponses.Match confirm(AuthenticatedUser actor, Long id) { BadmintonMatch match = findMatchForUpdate(id); requireOrganizer(actor, match.getSession().getGroup().getId()); if (match.getStatus() == com.groupsync.backend.badminton.model.MatchStatus.CONFIRMED) return toResponse(match); match.confirmResult(); rankingService.applyConfirmedMatch(match); events.publishEvent(new MatchConfirmedEvent(match.getId(), match.getSession().getGroup().getId(), match.getSession().getGroup().getName() + " badminton result", match.getScoreA() + "-" + match.getScoreB(), match.getSides().stream().flatMap(s -> s.getParticipants().stream()).map(p -> p.getUser().getId()).toList())); return toResponse(match); }
    @Transactional(readOnly = true) public List<MatchResponses.Match> list(AuthenticatedUser actor, Long groupId) { requireMember(groupId, actor.getId()); return matchRepository.findBySessionGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::toResponse).toList(); }

    private void addSide(BadmintonMatch match, MatchSideCode code, List<Long> ids) { BadmintonMatchSide side = new BadmintonMatchSide(match, code); for (Long id : ids) side.addParticipant(new BadmintonMatchParticipant(side, userRepository.findById(id).orElseThrow(() -> new NotFoundException("Player not found.")))); match.addSide(side); }
    private MatchResponses.Match toResponse(BadmintonMatch m) { return new MatchResponses.Match(m.getId(), m.getSession().getId(), m.getCourt().getId(), m.getCourt().getName(), m.getRoundNumber(), m.getStatus().name(), m.getScoreA(), m.getScoreB(), m.getWinnerSide() == null ? null : m.getWinnerSide().name(), m.getSides().stream().sorted(java.util.Comparator.comparing(s -> s.getCode().name())).map(s -> new MatchResponses.Side(s.getCode().name(), s.getParticipants().stream().map(p -> new MatchResponses.Participant(p.getUser().getId(), p.getUser().getDisplayName())).toList())).toList()); }
    private BadmintonMatch findMatchForUpdate(Long id) { return matchRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Match not found.")); }
    private void requirePlayingSession(BadmintonSession session) { if (session.getStatus() != com.groupsync.backend.badminton.model.BadmintonSessionStatus.PLAYING) throw new ConflictException("The session must be PLAYING for match operations."); }
    private BadmintonSession organizerSession(AuthenticatedUser actor, Long id) { BadmintonSession s = sessionRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Badminton session not found.")); requireOrganizer(actor, s.getGroup().getId()); return s; }
    private Membership requireMember(Long groupId, Long userId) { return membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
    private void requireOrganizer(AuthenticatedUser actor, Long groupId) { if (requireMember(groupId, actor.getId()).getRole() == GroupRole.MEMBER) throw new ForbiddenException("Only the owner or organizer can manage matches."); }
}
