package com.groupsync.backend.badminton.ranking;

public interface RankingStrategy {
    int pointsForWin(int currentPoints, int opponentPoints);
    int pointsForLoss(int currentPoints, int opponentPoints);
    String name();
}
