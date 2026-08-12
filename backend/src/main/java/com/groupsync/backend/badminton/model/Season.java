package com.groupsync.backend.badminton.model;

import java.time.Instant;
import java.time.LocalDate;

import com.groupsync.backend.group.model.Group;

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
@Table(name = "badminton_seasons", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_season_group_name", columnNames = {"group_id", "name"}))
public class Season {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "starts_on", nullable = false) private LocalDate startsOn;
    @Column(name = "ends_on") private LocalDate endsOn;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected Season() { }
    public Season(Group group, String name, LocalDate startsOn, LocalDate endsOn, boolean active) {
        this.group = group; this.name = name; this.startsOn = startsOn; this.endsOn = endsOn; this.active = active;
    }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public String getName() { return name; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public boolean isActive() { return active; }
    public void activate() { active = true; }
    public void deactivate() { active = false; }
}
