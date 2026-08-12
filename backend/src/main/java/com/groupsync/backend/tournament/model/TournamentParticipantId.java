package com.groupsync.backend.tournament.model;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

@Embeddable public class TournamentParticipantId implements Serializable {
    private Long tournamentId; private Long userId;
    protected TournamentParticipantId() { }
    public TournamentParticipantId(Long tournamentId, Long userId) { this.tournamentId = tournamentId; this.userId = userId; }
    @Override public boolean equals(Object other) { if (!(other instanceof TournamentParticipantId id)) return false; return java.util.Objects.equals(tournamentId, id.tournamentId) && java.util.Objects.equals(userId, id.userId); }
    @Override public int hashCode() { return java.util.Objects.hash(tournamentId, userId); }
}
