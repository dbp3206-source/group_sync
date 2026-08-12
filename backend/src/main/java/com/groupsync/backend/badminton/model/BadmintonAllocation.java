package com.groupsync.backend.badminton.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "badminton_allocations", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_allocation_court_round", columnNames = {"session_id", "court_id", "round_number"}))
public class BadmintonAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "court_id", nullable = false) private Court court;
    @Column(name = "round_number", nullable = false) private int roundNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AllocationStatus status = AllocationStatus.DRAFT;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "allocation", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true) private Set<BadmintonAllocationPlayer> players = new LinkedHashSet<>();

    protected BadmintonAllocation() { }
    public BadmintonAllocation(BadmintonSession session, Court court, int roundNumber) { this.session = session; this.court = court; this.roundNumber = roundNumber; }
    public Long getId() { return id; }
    public BadmintonSession getSession() { return session; }
    public Court getCourt() { return court; }
    public int getRoundNumber() { return roundNumber; }
    public AllocationStatus getStatus() { return status; }
    public Set<BadmintonAllocationPlayer> getPlayers() { return players; }
    public void addPlayer(BadmintonAllocationPlayer player) { players.add(player); }
    public void confirm() { status = AllocationStatus.CONFIRMED; }
}
