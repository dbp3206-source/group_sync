package com.groupsync.backend.dashboard.dto;

import java.time.Instant;
import java.util.List;

import com.groupsync.backend.badminton.dto.MatchResponses;

public record DashboardResponse(
    List<NextActivity> nextActivities,
    long registrationCount,
    List<MatchResponses.Match> recentMatches,
    List<MatchResponses.Stat> leaderboard,
    List<MatchResponses.News> news
) {
    public record NextActivity(Long sessionId, String title, Instant start, Instant end, String status) { }
}
