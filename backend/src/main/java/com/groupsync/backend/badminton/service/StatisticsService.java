package com.groupsync.backend.badminton.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.MatchResponses;
import com.groupsync.backend.badminton.model.BadmintonMatch;
import com.groupsync.backend.badminton.model.BadmintonPlayerStat;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.repository.BadmintonMatchRepository;
import com.groupsync.backend.badminton.repository.BadmintonPlayerStatRepository;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;

@Service
public class StatisticsService {
    private final BadmintonPlayerStatRepository statRepository; private final BadmintonRegistrationRepository registrationRepository; private final BadmintonMatchRepository matchRepository; private final MembershipRepository membershipRepository;
    public StatisticsService(BadmintonPlayerStatRepository statRepository, BadmintonRegistrationRepository registrationRepository, BadmintonMatchRepository matchRepository, MembershipRepository membershipRepository) { this.statRepository = statRepository; this.registrationRepository = registrationRepository; this.matchRepository = matchRepository; this.membershipRepository = membershipRepository; }
    @Transactional public void recordAttendance(BadmintonSession session) { registrationRepository.findBySessionIdOrderByRegisteredAtAscIdAsc(session.getId()).stream().filter(r -> r.getStatus() == RegistrationStatus.CHECKED_IN || r.getStatus() == RegistrationStatus.NO_SHOW).forEach(r -> { BadmintonPlayerStat stat = statRepository.findByGroupIdAndSeasonIdAndUserId(session.getGroup().getId(), session.getSeason().getId(), r.getUser().getId()).orElseGet(() -> statRepository.save(new BadmintonPlayerStat(session.getGroup(), session.getSeason(), r.getUser()))); stat.recordAttendance(r.getStatus() == RegistrationStatus.NO_SHOW); statRepository.save(stat); }); }
    @Transactional(readOnly = true) public List<MatchResponses.Stat> leaderboard(AuthenticatedUser actor, Long groupId, Long seasonId) { requireMember(groupId, actor.getId()); return statRepository.findLeaderboard(groupId, seasonId).stream().map(s -> toStat(s, recentForm(s))).toList(); }
    @Transactional(readOnly = true) public MatchResponses.Stat player(AuthenticatedUser actor, Long groupId, Long seasonId, Long userId) { requireMember(groupId, actor.getId()); BadmintonPlayerStat stat = statRepository.findByGroupIdAndSeasonIdAndUserId(groupId, seasonId, userId).orElseThrow(() -> new NotFoundException("Player statistics not found.")); return toStat(stat, recentForm(stat)); }
    private String recentForm(BadmintonPlayerStat stat) { return matchRepository.findBySessionGroupIdOrderByCreatedAtDesc(stat.getGroup().getId()).stream().filter(m -> m.getSeason().getId().equals(stat.getSeason().getId()) && m.getStatus() == com.groupsync.backend.badminton.model.MatchStatus.CONFIRMED).filter(m -> m.getSides().stream().anyMatch(side -> side.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(stat.getUser().getId())))).limit(5).map(m -> m.getSides().stream().filter(side -> side.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(stat.getUser().getId()))).findFirst().orElseThrow().getCode() == m.getWinnerSide() ? "W" : "L").reduce("", (a, b) -> a + b); }
    private MatchResponses.Stat toStat(BadmintonPlayerStat s, String form) { double rate = s.getMatchesPlayed() == 0 ? 0 : (100.0 * s.getWins() / s.getMatchesPlayed()); return new MatchResponses.Stat(s.getUser().getId(), s.getUser().getDisplayName(), s.getMatchesPlayed(), s.getWins(), s.getLosses(), s.getPoints(), s.getAttended(), s.getNoShows(), rate, form); }
    private void requireMember(Long groupId, Long userId) { membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
}
