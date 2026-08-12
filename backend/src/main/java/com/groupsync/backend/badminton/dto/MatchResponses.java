package com.groupsync.backend.badminton.dto;
import java.time.Instant;
import java.util.List;
public final class MatchResponses {
    private MatchResponses() { }
    public record Participant(Long userId, String displayName) { }
    public record Side(String code, List<Participant> participants) { }
    public record Match(Long id, Long sessionId, Long courtId, String courtName, int roundNumber, String status, Integer scoreA, Integer scoreB, String winnerSide, List<Side> sides) { }
    public record Stat(Long userId, String displayName, int matches, int wins, int losses, int points, int attended, int noShows, double winRate, String recentForm) { }
    public record History(Long id, Long matchId, Long userId, int pointsAfter, int winsAfter, int matchesAfter, Instant createdAt) { }
    public record News(Long id, String type, String title, String content, Long targetId, Instant createdAt) { }
}
