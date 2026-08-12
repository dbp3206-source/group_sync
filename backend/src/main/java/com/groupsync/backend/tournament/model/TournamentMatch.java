package com.groupsync.backend.tournament.model;

import com.groupsync.backend.badminton.model.BadmintonMatch;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity @Table(name = "tournament_matches", uniqueConstraints = @UniqueConstraint(name = "uk_tournament_stage_number", columnNames = {"tournament_id", "stage", "match_number"}))
public class TournamentMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tournament_id", nullable = false) private Tournament tournament;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id", nullable = false, unique = true) private BadmintonMatch match;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TournamentStage stage;
    @Column(name = "match_number", nullable = false) private int matchNumber;
    @Column(name = "next_match_number") private Integer nextMatchNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "winner_user_id") private UserAccount winner;
    protected TournamentMatch() { }
    public TournamentMatch(Tournament tournament, BadmintonMatch match, TournamentStage stage, int matchNumber, Integer nextMatchNumber) { this.tournament = tournament; this.match = match; this.stage = stage; this.matchNumber = matchNumber; this.nextMatchNumber = nextMatchNumber; }
    public Long getId() { return id; } public Tournament getTournament() { return tournament; } public BadmintonMatch getMatch() { return match; } public TournamentStage getStage() { return stage; } public int getMatchNumber() { return matchNumber; } public Integer getNextMatchNumber() { return nextMatchNumber; } public UserAccount getWinner() { return winner; }
    public void setWinner(UserAccount winner) { this.winner = winner; }
}
