package com.groupsync.backend.badminton.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.groupsync.backend.group.model.Group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "badminton_sessions")
public class BadmintonSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false) private Season season;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "venue_id", nullable = false) private Venue venue;
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "start_at", nullable = false) private Instant startAt;
    @Column(name = "end_at", nullable = false) private Instant endAt;
    @Column(name = "registration_deadline", nullable = false) private Instant registrationDeadline;
    @Column(nullable = false) private int capacity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BadmintonSessionStatus status;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "badminton_session_courts", joinColumns = @JoinColumn(name = "session_id"), inverseJoinColumns = @JoinColumn(name = "court_id"))
    private Set<Court> courts = new HashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected BadmintonSession() { }
    public BadmintonSession(Group group, Season season, Venue venue, String title, Instant startAt, Instant endAt, Instant registrationDeadline, int capacity, Set<Court> courts) {
        this.group = group; this.season = season; this.venue = venue; this.title = title; this.startAt = startAt; this.endAt = endAt;
        this.registrationDeadline = registrationDeadline; this.capacity = capacity; this.status = BadmintonSessionStatus.DRAFT; this.courts = new HashSet<>(courts);
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public Season getSeason() { return season; }
    public Venue getVenue() { return venue; }
    public String getTitle() { return title; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Instant getRegistrationDeadline() { return registrationDeadline; }
    public int getCapacity() { return capacity; }
    public BadmintonSessionStatus getStatus() { return status; }
    public Set<Court> getCourts() { return courts; }
    public void open() { requireStatus(BadmintonSessionStatus.DRAFT); status = BadmintonSessionStatus.OPEN; }
    public void confirm() { requireStatus(BadmintonSessionStatus.OPEN); status = BadmintonSessionStatus.CONFIRMED; }
    public void start() { requireStatus(BadmintonSessionStatus.CONFIRMED); status = BadmintonSessionStatus.PLAYING; }
    public void complete() { requireStatus(BadmintonSessionStatus.PLAYING); status = BadmintonSessionStatus.COMPLETED; }
    public void cancel() { if (status == BadmintonSessionStatus.COMPLETED || status == BadmintonSessionStatus.CANCELLED) throw new IllegalStateException("Badminton session cannot be cancelled now."); status = BadmintonSessionStatus.CANCELLED; }
    public void reschedule(Instant startAt, Instant endAt, Instant deadline) {
        if (status == BadmintonSessionStatus.PLAYING || status == BadmintonSessionStatus.COMPLETED || status == BadmintonSessionStatus.CANCELLED) throw new IllegalStateException("Badminton session cannot be changed now.");
        this.startAt = startAt; this.endAt = endAt; this.registrationDeadline = deadline;
    }
    private void requireStatus(BadmintonSessionStatus expected) { if (status != expected) throw new IllegalStateException("Badminton session must be " + expected + "."); }
}
