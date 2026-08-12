package com.groupsync.backend.badminton.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.badminton.model.BadmintonMatch;
import com.groupsync.backend.badminton.model.BadmintonMatchParticipant;
import com.groupsync.backend.badminton.model.BadmintonPlayerStat;
import com.groupsync.backend.badminton.model.RankingHistory;
import com.groupsync.backend.badminton.ranking.PointsRankingStrategy;
import com.groupsync.backend.badminton.repository.BadmintonPlayerStatRepository;
import com.groupsync.backend.badminton.repository.RankingHistoryRepository;

@Service
public class RankingService {
    private final BadmintonPlayerStatRepository statRepository;
    private final RankingHistoryRepository historyRepository;
    private final PointsRankingStrategy strategy = new PointsRankingStrategy();
    public RankingService(BadmintonPlayerStatRepository statRepository, RankingHistoryRepository historyRepository) { this.statRepository = statRepository; this.historyRepository = historyRepository; }

    @Transactional
    public void applyConfirmedMatch(BadmintonMatch match) {
        if (match.getId() == null) throw new IllegalStateException("Match must be saved before ranking is applied.");
        Set<Long> seen = new HashSet<>();
        for (var side : match.getSides()) for (BadmintonMatchParticipant participant : side.getParticipants()) {
            if (!seen.add(participant.getUser().getId())) continue;
            BadmintonPlayerStat stat = statRepository.findByGroupIdAndSeasonIdAndUserId(match.getSession().getGroup().getId(), match.getSeason().getId(), participant.getUser().getId()).orElseGet(() -> statRepository.save(new BadmintonPlayerStat(match.getSession().getGroup(), match.getSeason(), participant.getUser())));
            boolean winner = side.getCode() == match.getWinnerSide();
            if (!historyRepository.existsByMatchIdAndUserId(match.getId(), participant.getUser().getId())) {
                if (winner) stat.recordWin(strategy.winnerPoints()); else stat.recordLoss(strategy.loserPoints());
                statRepository.save(stat);
                historyRepository.save(new RankingHistory(match, match.getSession().getGroup(), match.getSeason(), participant.getUser(), stat.getPoints(), stat.getWins(), stat.getMatchesPlayed()));
            }
        }
    }
}
