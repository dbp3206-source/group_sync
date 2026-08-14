package com.groupsync.backend.tournament.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_entries")
public class TournamentEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "seed_number")
    private Integer seedNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentEntryMember> members = new ArrayList<>();

    protected TournamentEntry() {
    }

    public TournamentEntry(Tournament tournament, String displayName, Integer seedNumber) {
        this.tournament = tournament;
        this.displayName = displayName;
        this.seedNumber = seedNumber;
    }

    public void addMember(com.groupsync.backend.user.model.UserAccount user) {
        members.add(new TournamentEntryMember(this, user));
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Integer getSeedNumber() { return seedNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TournamentEntryMember> getMembers() { return List.copyOf(members); }
    public void changeSeed(Integer seedNumber) { this.seedNumber = seedNumber; }
}
