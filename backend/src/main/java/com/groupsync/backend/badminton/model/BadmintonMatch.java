package com.groupsync.backend.badminton.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "badminton_matches", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_match_court_round", columnNames = {"session_id", "court_id", "round_number"}))
public class BadmintonMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false) private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "court_id", nullable = false) private Court court;
    @Column(name = "round_number", nullable = false) private int roundNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private MatchStatus status = MatchStatus.SCHEDULED;
    @Column(name = "score_a") private Integer scoreA;
    @Column(name = "score_b") private Integer scoreB;
    @Enumerated(EnumType.STRING) @Column(name = "winner_side", length = 2) private MatchSideCode winnerSide;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true) private Set<BadmintonMatchSide> sides = new LinkedHashSet<>();

    protected BadmintonMatch() { }
    public BadmintonMatch(BadmintonSession session, Court court, int roundNumber) { this.session = session; this.season = session.getSeason(); this.court = court; this.roundNumber = roundNumber; }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; } public BadmintonSession getSession() { return session; } public Season getSeason() { return season; } public Court getCourt() { return court; } public int getRoundNumber() { return roundNumber; } public MatchStatus getStatus() { return status; } public Integer getScoreA() { return scoreA; } public Integer getScoreB() { return scoreB; } public MatchSideCode getWinnerSide() { return winnerSide; } public Set<BadmintonMatchSide> getSides() { return sides; }
    public void addSide(BadmintonMatchSide side) { sides.add(side); }
    public void start() { if (status != MatchStatus.SCHEDULED) throw new IllegalStateException("Match must be scheduled before it starts."); status = MatchStatus.PLAYING; }
    public void submitResult(int scoreA, int scoreB) { if (status != MatchStatus.PLAYING) throw new IllegalStateException("Match must be playing before a result is submitted."); if (scoreA < 0 || scoreB < 0 || scoreA == scoreB) throw new IllegalArgumentException("Scores must be non-negative and cannot be tied."); this.scoreA = scoreA; this.scoreB = scoreB; winnerSide = scoreA > scoreB ? MatchSideCode.A : MatchSideCode.B; status = MatchStatus.RESULT_SUBMITTED; }
    public void confirmResult() { if (status != MatchStatus.RESULT_SUBMITTED) throw new IllegalStateException("Only a submitted result can be confirmed."); status = MatchStatus.CONFIRMED; }
    public void cancel() { if (status == MatchStatus.CONFIRMED) throw new IllegalStateException("A confirmed match cannot be cancelled."); status = MatchStatus.CANCELLED; }
}
