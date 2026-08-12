package com.groupsync.backend.badminton.model;

import com.groupsync.backend.badminton.model.Venue;

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

@Entity
@Table(name = "badminton_courts", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_court_venue_name", columnNames = {"venue_id", "name"}))
public class Court {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "venue_id", nullable = false) private Venue venue;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false) private boolean active = true;

    protected Court() { }
    public Court(Venue venue, String name) { this.venue = venue; this.name = name; }
    public Long getId() { return id; }
    public Venue getVenue() { return venue; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
