package com.groupsync.backend.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.badminton.repository.SeasonRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.tournament.model.Tournament;
import com.groupsync.backend.tournament.model.TournamentEntry;
import com.groupsync.backend.tournament.model.TournamentMatch;
import com.groupsync.backend.tournament.repository.TournamentEntryRepository;
import com.groupsync.backend.tournament.repository.TournamentMatchRepository;
import com.groupsync.backend.tournament.repository.TournamentRepository;
import com.groupsync.backend.user.repository.UserAccountRepository;

class TournamentServiceTest {
    @Test
    void knockoutGeneratorUsesSharedNextMatchNumbersAndPromotesByes() {
        TournamentRepository tournaments = mock(TournamentRepository.class);
        TournamentEntryRepository entries = mock(TournamentEntryRepository.class);
        TournamentMatchRepository matches = mock(TournamentMatchRepository.class);
        TournamentService service = new TournamentService(
            tournaments, entries, matches, mock(MembershipRepository.class), mock(SeasonRepository.class),
            mock(BadmintonSessionRepository.class), mock(UserAccountRepository.class));
        Tournament tournament = mock(Tournament.class);
        when(tournament.getId()).thenReturn(10L);

        List<TournamentMatch> saved = new ArrayList<>();
        when(matches.save(org.mockito.ArgumentMatchers.any(TournamentMatch.class))).thenAnswer((Answer<TournamentMatch>) invocation -> {
            TournamentMatch match = invocation.getArgument(0);
            saved.add(match);
            return match;
        });
        when(matches.findByTournamentIdOrderByStageAscMatchNumberAsc(10L)).thenAnswer(invocation -> saved);
        when(matches.findByTournamentIdAndMatchNumber(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> {
                Integer matchNumber = invocation.getArgument(1, Integer.class);
                return saved.stream().filter(match -> match.getMatchNumber() == matchNumber).findFirst();
            });
        when(matches.findByTournamentIdAndNextMatchNumber(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> {
                Integer nextMatchNumber = invocation.getArgument(1, Integer.class);
                return saved.stream().filter(match -> nextMatchNumber.equals(match.getNextMatchNumber())).toList();
            });

        List<TournamentEntry> roster = List.of(entry(1), entry(2), entry(3), entry(4), entry(5));
        service.generateKnockout(tournament, roster);
        service.resolveByes(tournament);

        assertThat(saved).hasSize(7);
        assertThat(saved.stream().map(TournamentMatch::getNextMatchNumber).toList()).containsExactly(5, 5, 6, 6, 7, 7, null);
        assertThat(match(saved, 1).getWinnerEntry()).isNotNull();
        assertThat(match(saved, 2).getWinnerEntry()).isNotNull();
        assertThat(match(saved, 3).getWinnerEntry()).isNotNull();
        assertThat(match(saved, 4).getWinnerEntry()).isNull();
        assertThat(match(saved, 5).getEntryA()).isNotNull();
        assertThat(match(saved, 5).getEntryB()).isNotNull();
        assertThat(match(saved, 6).hasExactlyOneEntry()).isTrue();
    }

    private TournamentEntry entry(int seed) {
        TournamentEntry entry = mock(TournamentEntry.class);
        when(entry.getSeedNumber()).thenReturn(seed);
        when(entry.getCreatedAt()).thenReturn(Instant.parse("2026-08-01T00:00:00Z").plusSeconds(seed));
        return entry;
    }

    private TournamentMatch match(List<TournamentMatch> matches, int number) {
        return matches.stream().filter(match -> match.getMatchNumber() == number).findFirst().orElseThrow();
    }
}
