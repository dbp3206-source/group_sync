package com.groupsync.backend.badminton.ranking;
public class PointsRankingStrategy implements RankingStrategy {
    public int winnerPoints() { return 3; }
    public int loserPoints() { return 1; }
    @Override public int pointsForWin(int currentPoints, int opponentPoints) { return winnerPoints(); }
    @Override public int pointsForLoss(int currentPoints, int opponentPoints) { return loserPoints(); }
    @Override public String name() { return "POINTS"; }
}
