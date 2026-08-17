package com.groupsync.backend.badminton.model;

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

@Entity @Table(name = "badminton_player_stats", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_player_stats_scope", columnNames = {"group_id", "season_id", "user_id"}))
public class BadmintonPlayerStat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false) private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "matches_played", nullable = false) private int matchesPlayed;
    @Column(nullable = false) private int wins;
    @Column(nullable = false) private int losses;
    @Column(nullable = false) private int points;
    @Column(nullable = false) private int attended;
    @Column(name = "no_shows", nullable = false) private int noShows;
    protected BadmintonPlayerStat() { }
    public BadmintonPlayerStat(Group group, Season season, UserAccount user) { this.group = group; this.season = season; this.user = user; }
    public Long getId() { return id; } public Group getGroup() { return group; } public Season getSeason() { return season; } public UserAccount getUser() { return user; } public int getMatchesPlayed() { return matchesPlayed; } public int getWins() { return wins; } public int getLosses() { return losses; } public int getPoints() { return points; } public int getAttended() { return attended; } public int getNoShows() { return noShows; }
    public void recordWin(int points) { matchesPlayed++; wins++; this.points += points; }
    public void recordLoss(int points) { matchesPlayed++; losses++; this.points += points; }
    public void recordAttendance(boolean noShow) { if (noShow) noShows++; else attended++; }
}
