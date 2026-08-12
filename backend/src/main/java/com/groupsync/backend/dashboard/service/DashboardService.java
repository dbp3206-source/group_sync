package com.groupsync.backend.dashboard.service;

import java.time.Instant;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.MatchResponses;
import com.groupsync.backend.badminton.model.BadmintonSessionStatus;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.badminton.service.StatisticsService;
import com.groupsync.backend.dashboard.dto.DashboardResponse;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.news.service.NewsService;
import com.groupsync.backend.badminton.service.MatchService;
import com.groupsync.backend.shared.exception.ForbiddenException;

@Service
public class DashboardService {
    private final MembershipRepository membershipRepository;
    private final BadmintonSessionRepository sessionRepository;
    private final BadmintonRegistrationRepository registrationRepository;
    private final MatchService matchService;
    private final StatisticsService statisticsService;
    private final NewsService newsService;

    public DashboardService(MembershipRepository membershipRepository, BadmintonSessionRepository sessionRepository, BadmintonRegistrationRepository registrationRepository, MatchService matchService, StatisticsService statisticsService, NewsService newsService) {
        this.membershipRepository = membershipRepository; this.sessionRepository = sessionRepository; this.registrationRepository = registrationRepository; this.matchService = matchService; this.statisticsService = statisticsService; this.newsService = newsService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(AuthenticatedUser actor, Long groupId, Long seasonId) {
        membershipRepository.findByGroupIdAndUserId(groupId, actor.getId()).orElseThrow(() -> new ForbiddenException("You are not a member of this group."));
        var sessions = sessionRepository.findByGroupIdOrderByStartAtDesc(groupId);
        var upcoming = sessions.stream().filter(s -> s.getStartAt().isAfter(Instant.now()) && s.getStatus() != BadmintonSessionStatus.CANCELLED).sorted(Comparator.comparing(s -> s.getStartAt())).limit(3).map(s -> new DashboardResponse.NextActivity(s.getId(), s.getTitle(), s.getStartAt(), s.getEndAt(), s.getStatus().name())).toList();
        long registrations = sessions.stream().mapToLong(s -> registrationRepository.countActiveBySessionId(s.getId())).sum();
        return new DashboardResponse(upcoming, registrations, matchService.list(actor, groupId).stream().limit(5).toList(), statisticsService.leaderboard(actor, groupId, seasonId).stream().limit(10).toList(), newsService.list(actor, groupId).stream().limit(10).toList());
    }
}
