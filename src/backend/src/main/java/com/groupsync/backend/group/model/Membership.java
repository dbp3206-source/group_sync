package com.groupsync.backend.group.model;

import java.time.Instant;

import com.groupsync.backend.user.model.UserAccount;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_membership_group_user", columnNames = {"group_id", "user_id"}))
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Membership() {
    }

    public Membership(Group group, UserAccount user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public UserAccount getUser() { return user; }
    public GroupRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void changeRole(GroupRole newRole) { this.role = newRole; }
}
