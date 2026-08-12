package com.groupsync.backend.badminton.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.AllocationResponses;
import com.groupsync.backend.badminton.dto.AnnouncementRequest;
import com.groupsync.backend.badminton.dto.MatchRequests.CreateMatchRequest;
import com.groupsync.backend.badminton.dto.MatchRequests.ScoreRequest;
import com.groupsync.backend.badminton.dto.MatchResponses;
import com.groupsync.backend.badminton.model.PairingStrategyType;
import com.groupsync.backend.badminton.service.AllocationService;
import com.groupsync.backend.badminton.service.MatchService;
import com.groupsync.backend.badminton.service.PairingService;
import com.groupsync.backend.badminton.service.StatisticsService;
import com.groupsync.backend.news.service.NewsService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/badminton")
public class BadmintonCompetitionController {
    private final AllocationService allocationService; private final PairingService pairingService; private final MatchService matchService; private final NewsService newsService; private final StatisticsService statisticsService;
    public BadmintonCompetitionController(AllocationService allocationService, PairingService pairingService, MatchService matchService, NewsService newsService, StatisticsService statisticsService) { this.allocationService = allocationService; this.pairingService = pairingService; this.matchService = matchService; this.newsService = newsService; this.statisticsService = statisticsService; }
    @PostMapping("/sessions/{id}/allocations/generate") public List<AllocationResponses.Allocation> generate(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @RequestParam(defaultValue = "1") int round) { return allocationService.generate(actor, id, round); }
    @PostMapping("/sessions/{id}/allocations/confirm") public List<AllocationResponses.Allocation> confirmAllocation(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @RequestParam(defaultValue = "1") int round) { return allocationService.confirm(actor, id, round); }
    @GetMapping("/sessions/{id}/allocations") public List<AllocationResponses.Allocation> allocations(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return allocationService.list(actor, id); }
    @GetMapping("/sessions/{id}/pairings") public List<AllocationResponses.Pairing> pairings(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @RequestParam(defaultValue = "1") int round, @RequestParam(defaultValue = "BALANCED") PairingStrategyType strategy, @RequestParam(defaultValue = "42") long seed) { return pairingService.suggest(actor, id, round, strategy, seed); }
    @PostMapping("/sessions/{id}/matches") @ResponseStatus(HttpStatus.CREATED) public MatchResponses.Match createMatch(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody CreateMatchRequest request) { return matchService.create(actor, id, request); }
    @GetMapping("/groups/{groupId}/matches") public List<MatchResponses.Match> matches(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return matchService.list(actor, groupId); }
    @PostMapping("/matches/{id}/start") public MatchResponses.Match start(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return matchService.start(actor, id); }
    @PostMapping("/matches/{id}/result") public MatchResponses.Match submit(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody ScoreRequest request) { return matchService.submit(actor, id, request); }
    @PostMapping("/matches/{id}/confirm") public MatchResponses.Match confirm(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return matchService.confirm(actor, id); }
    @GetMapping("/groups/{groupId}/news") public List<MatchResponses.News> news(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return newsService.list(actor, groupId); }
    @PostMapping("/groups/{groupId}/news") @ResponseStatus(HttpStatus.CREATED) public MatchResponses.News announce(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody AnnouncementRequest request) { return newsService.announce(actor, groupId, request.title(), request.content()); }
    @GetMapping("/groups/{groupId}/leaderboard") public List<MatchResponses.Stat> leaderboard(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @RequestParam Long seasonId) { return statisticsService.leaderboard(actor, groupId, seasonId); }
    @GetMapping("/groups/{groupId}/players/{userId}/stats") public MatchResponses.Stat playerStats(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @PathVariable Long userId, @RequestParam Long seasonId) { return statisticsService.player(actor, groupId, seasonId, userId); }
}
