package com.groupsync.backend.tournament.model;

import java.time.Instant;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity @Table(name = "tournaments")
public class Tournament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false) private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 30) private String format;
    @Enumerated(EnumType.STRING) @Column(name = "competition_mode", nullable = false, length = 20) private TournamentCompetitionMode competitionMode = TournamentCompetitionMode.SINGLES;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TournamentStatus status = TournamentStatus.DRAFT;
    @Column(name = "max_participants", nullable = false) private int maxParticipants;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "champion_id") private UserAccount champion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "champion_entry_id") private TournamentEntry championEntry;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Tournament() { }
    public Tournament(Group group, Season season, BadmintonSession session, String name, String format, TournamentCompetitionMode competitionMode, int maxParticipants) { this.group = group; this.season = season; this.session = session; this.name = name; this.format = format; this.competitionMode = competitionMode; this.maxParticipants = maxParticipants; }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; } public Group getGroup() { return group; } public Season getSeason() { return season; } public BadmintonSession getSession() { return session; } public String getName() { return name; } public String getFormat() { return format; } public TournamentCompetitionMode getCompetitionMode() { return competitionMode; } public TournamentStatus getStatus() { return status; } public int getMaxParticipants() { return maxParticipants; } public UserAccount getChampion() { return champion; } public TournamentEntry getChampionEntry() { return championEntry; }
    public void open() { if (status != TournamentStatus.DRAFT) throw new IllegalStateException("Tournament must be draft before registration opens."); status = TournamentStatus.REGISTRATION_OPEN; }
    public void start() { if (status != TournamentStatus.REGISTRATION_OPEN) throw new IllegalStateException("Tournament registration must be open before starting."); status = TournamentStatus.IN_PROGRESS; }
    public void complete(UserAccount user) { if (status != TournamentStatus.IN_PROGRESS) throw new IllegalStateException("Tournament must be in progress before completion."); champion = user; status = TournamentStatus.COMPLETED; }
    public void complete(TournamentEntry entry) { if (status != TournamentStatus.IN_PROGRESS) throw new IllegalStateException("Tournament must be in progress before completion."); championEntry = entry; champion = entry.getMembers().getFirst().getUser(); status = TournamentStatus.COMPLETED; }
    public void cancel() { if (status == TournamentStatus.COMPLETED) throw new IllegalStateException("Completed tournament cannot be cancelled."); status = TournamentStatus.CANCELLED; }
}
