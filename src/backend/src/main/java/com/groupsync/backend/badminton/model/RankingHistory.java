package com.groupsync.backend.badminton.model;

import java.time.Instant;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity @Table(name = "badminton_ranking_history", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_ranking_history_match_user", columnNames = {"match_id", "user_id"}))
public class RankingHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "match_id", nullable = false) private BadmintonMatch match;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false) private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "points_after", nullable = false) private int pointsAfter;
    @Column(name = "wins_after", nullable = false) private int winsAfter;
    @Column(name = "matches_after", nullable = false) private int matchesAfter;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected RankingHistory() { }
    public RankingHistory(BadmintonMatch match, Group group, Season season, UserAccount user, int pointsAfter, int winsAfter, int matchesAfter) { this.match = match; this.group = group; this.season = season; this.user = user; this.pointsAfter = pointsAfter; this.winsAfter = winsAfter; this.matchesAfter = matchesAfter; }
    public Long getId() { return id; } public BadmintonMatch getMatch() { return match; } public UserAccount getUser() { return user; } public int getPointsAfter() { return pointsAfter; } public int getWinsAfter() { return winsAfter; } public int getMatchesAfter() { return matchesAfter; } public Instant getCreatedAt() { return createdAt; }
}
