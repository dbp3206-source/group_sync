package com.groupsync.backend.badminton.model;

import java.time.Instant;

import com.groupsync.backend.group.model.Membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "badminton_profiles")
public class BadmintonProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "membership_id", nullable = false, unique = true) private Membership membership;
    @Enumerated(EnumType.STRING) @Column(name = "skill_level", nullable = false, length = 20) private BadmintonSkillLevel skillLevel;
    @Column(length = 500) private String bio;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected BadmintonProfile() { }

    public BadmintonProfile(Membership membership, BadmintonSkillLevel skillLevel, String bio) {
        this.membership = membership;
        this.skillLevel = skillLevel;
        this.bio = bio;
    }

    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Membership getMembership() { return membership; }
    public BadmintonSkillLevel getSkillLevel() { return skillLevel; }
    public String getBio() { return bio; }
    public void update(BadmintonSkillLevel skillLevel, String bio) { this.skillLevel = skillLevel; this.bio = bio; }
}
