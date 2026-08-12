package com.groupsync.backend.badminton.model;

import java.time.Instant;

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
@Table(name = "badminton_venues", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_venue_group_name", columnNames = {"group_id", "name"}))
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 300) private String address;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected Venue() { }
    public Venue(Group group, String name, String address) { this.group = group; this.name = name; this.address = address; }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public String getName() { return name; }
    public String getAddress() { return address; }
}
