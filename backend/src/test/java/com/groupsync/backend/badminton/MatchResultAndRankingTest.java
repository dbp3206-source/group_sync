package com.groupsync.backend.badminton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.badminton.model.BadmintonMatch;
import com.groupsync.backend.badminton.model.BadmintonMatchParticipant;
import com.groupsync.backend.badminton.model.BadmintonMatchSide;
import com.groupsync.backend.badminton.model.BadmintonPlayerStat;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.MatchSideCode;
import com.groupsync.backend.badminton.model.MatchStatus;
import com.groupsync.backend.badminton.model.RankingHistory;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.model.Venue;
import com.groupsync.backend.badminton.repository.BadmintonPlayerStatRepository;
import com.groupsync.backend.badminton.repository.RankingHistoryRepository;
import com.groupsync.backend.badminton.ranking.PointsRankingStrategy;
import com.groupsync.backend.badminton.service.RankingService;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.user.model.UserAccount;

class MatchResultAndRankingTest {
    @Test
    void resultDerivesWinnerAndConfirmedResultCannotBeConfirmedTwice() {
        Group group = new Group("Badminton", null, com.groupsync.backend.group.model.GroupType.BADMINTON);
        Season season = new Season(group, "Season 1", LocalDate.now(), null, true);
        Venue venue = new Venue(group, "Hall", null);
        Instant start = Instant.parse("2026-08-20T10:00:00Z");
        BadmintonSession session = new BadmintonSession(group, season, venue, "Play", start, start.plusSeconds(3600), start.minusSeconds(3600), 16, Set.of());
        BadmintonMatch match = new BadmintonMatch(session, null, 1);
        match.submitResult(21, 17);
        assertThat(match.getWinnerSide()).isEqualTo(MatchSideCode.A);
        match.confirmResult();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThatThrownBy(match::confirmResult).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pointsStrategyAndRankingServiceApplyWinnerThreeLoserOneOnlyOnce() {
        var stats = mock(BadmintonPlayerStatRepository.class); var history = mock(RankingHistoryRepository.class);
        var service = new RankingService(stats, history);
        Group group = mock(Group.class); Season season = mock(Season.class); BadmintonSession session = mock(BadmintonSession.class); BadmintonMatch match = mock(BadmintonMatch.class); BadmintonMatchSide side = mock(BadmintonMatchSide.class); BadmintonMatchParticipant participant = mock(BadmintonMatchParticipant.class); UserAccount user = mock(UserAccount.class);
        when(match.getId()).thenReturn(9L); when(match.getSession()).thenReturn(session); when(match.getSeason()).thenReturn(season); when(match.getWinnerSide()).thenReturn(MatchSideCode.A); when(match.getSides()).thenReturn(Set.of(side)); when(side.getCode()).thenReturn(MatchSideCode.A); when(side.getParticipants()).thenReturn(Set.of(participant)); when(participant.getUser()).thenReturn(user); when(user.getId()).thenReturn(7L); when(session.getGroup()).thenReturn(group); when(group.getId()).thenReturn(2L); when(season.getId()).thenReturn(3L); when(stats.findByGroupIdAndSeasonIdAndUserId(2L, 3L, 7L)).thenReturn(java.util.Optional.of(new BadmintonPlayerStat(group, season, user))); when(history.existsByMatchIdAndUserId(9L, 7L)).thenReturn(false);
        service.applyConfirmedMatch(match);
        verify(history).save(any(RankingHistory.class));
        assertThat(new PointsRankingStrategy().winnerPoints()).isEqualTo(3);
        assertThat(new PointsRankingStrategy().loserPoints()).isEqualTo(1);
    }
}
