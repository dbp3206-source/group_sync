package com.groupsync.backend.badminton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.badminton.ranking.EloRankingStrategy;
import com.groupsync.backend.badminton.ranking.PointsRankingStrategy;

class RankingStrategyTest {
    @Test
    void pointsStrategyKeepsTheSimpleDefault() {
        var strategy = new PointsRankingStrategy();
        assertEquals(3, strategy.pointsForWin(0, 0));
        assertEquals(1, strategy.pointsForLoss(0, 0));
        assertEquals("POINTS", strategy.name());
    }

    @Test
    void eloStrategyChangesWinnerAndLoserByDifferentSigns() {
        var strategy = new EloRankingStrategy();
        assertEquals("ELO", strategy.name());
        org.junit.jupiter.api.Assertions.assertTrue(strategy.pointsForWin(1000, 1000) > 0);
        org.junit.jupiter.api.Assertions.assertTrue(strategy.pointsForLoss(1000, 1000) < 0);
    }
}
