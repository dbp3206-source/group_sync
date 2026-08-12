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

@Entity
@Table(name = "group_invitations")
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private UserAccount invitee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private UserAccount inviter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Invitation() {
    }

    public Invitation(Group group, UserAccount invitee, UserAccount inviter) {
        this.group = group;
        this.invitee = invitee;
        this.inviter = inviter;
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public UserAccount getInvitee() { return invitee; }
    public UserAccount getInviter() { return inviter; }
    public InvitationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRespondedAt() { return respondedAt; }

    public void accept() {
        status = InvitationStatus.ACCEPTED;
        respondedAt = Instant.now();
    }

    public void decline() {
        status = InvitationStatus.DECLINED;
        respondedAt = Instant.now();
    }
}
