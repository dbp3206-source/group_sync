package com.groupsync.backend.tournament.model;

import java.time.Instant;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity @Table(name = "tournament_participants")
public class TournamentParticipant {
    @EmbeddedId private TournamentParticipantId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("tournamentId") @JoinColumn(name = "tournament_id") private Tournament tournament;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("userId") @JoinColumn(name = "user_id") private UserAccount user;
    @Column(name = "seed_number") private Integer seedNumber;
    @Column(name = "registered_at", nullable = false, updatable = false) private Instant registeredAt = Instant.now();
    protected TournamentParticipant() { }
    public TournamentParticipant(Tournament tournament, UserAccount user) { this.tournament = tournament; this.user = user; this.id = new TournamentParticipantId(tournament.getId(), user.getId()); }
    public Tournament getTournament() { return tournament; } public UserAccount getUser() { return user; } public Integer getSeedNumber() { return seedNumber; } public Instant getRegisteredAt() { return registeredAt; }
}
