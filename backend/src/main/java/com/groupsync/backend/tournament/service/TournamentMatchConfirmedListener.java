package com.groupsync.backend.tournament.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.badminton.event.MatchConfirmedEvent;
import com.groupsync.backend.tournament.model.TournamentStage;
import com.groupsync.backend.tournament.model.TournamentStatus;
import com.groupsync.backend.tournament.repository.TournamentMatchRepository;

/** Copies the winner derived by the normal match workflow into tournament state. */
@Component
public class TournamentMatchConfirmedListener {
    private final TournamentMatchRepository tournamentMatches;

    public TournamentMatchConfirmedListener(TournamentMatchRepository tournamentMatches) {
        this.tournamentMatches = tournamentMatches;
    }

    @EventListener
    @Transactional
    public void onMatchConfirmed(MatchConfirmedEvent event) {
        tournamentMatches.findByMatchId(event.matchId()).ifPresent(tournamentMatch -> {
            var match = tournamentMatch.getMatch();
            var winner = match.getSides().stream()
                    .filter(side -> side.getCode() == match.getWinnerSide())
                    .flatMap(side -> side.getParticipants().stream())
                    .map(participant -> participant.getUser())
                    .findFirst()
                    .orElse(null);
            if (winner == null) {
                return;
            }
            tournamentMatch.setWinner(winner);
            if (tournamentMatch.getStage() == TournamentStage.FINAL
                    && tournamentMatch.getTournament().getStatus() == TournamentStatus.IN_PROGRESS) {
                tournamentMatch.getTournament().complete(winner);
            }
        });
    }
}
