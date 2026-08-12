package com.groupsync.backend.badminton.ranking;

public class EloRankingStrategy implements RankingStrategy {
    private static final int START_RATING = 1000;
    private static final int K_FACTOR = 32;
    @Override public int pointsForWin(int currentPoints, int opponentPoints) { return (int) Math.round(K_FACTOR * (1 - expected(currentPoints, opponentPoints))); }
    @Override public int pointsForLoss(int currentPoints, int opponentPoints) { return (int) Math.round(K_FACTOR * (0 - expected(currentPoints, opponentPoints))); }
    @Override public String name() { return "ELO"; }
    private double expected(int currentPoints, int opponentPoints) { int current = currentPoints == 0 ? START_RATING : currentPoints; int opponent = opponentPoints == 0 ? START_RATING : opponentPoints; return 1.0 / (1.0 + Math.pow(10, (opponent - current) / 400.0)); }
}
