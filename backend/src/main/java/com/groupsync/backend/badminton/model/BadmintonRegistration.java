package com.groupsync.backend.badminton.model;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "badminton_registrations", uniqueConstraints = @UniqueConstraint(name = "uk_badminton_registration_session_user", columnNames = {"session_id", "user_id"}))
public class BadmintonRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RegistrationStatus status;
    @Column(name = "queued_at") private Instant queuedAt;
    @Column(name = "registered_at", nullable = false, updatable = false) private Instant registeredAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected BadmintonRegistration() { }
    public BadmintonRegistration(BadmintonSession session, UserAccount user) { this.session = session; this.user = user; this.status = RegistrationStatus.CANCELLED; }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public BadmintonSession getSession() { return session; }
    public UserAccount getUser() { return user; }
    public RegistrationStatus getStatus() { return status; }
    public Instant getQueuedAt() { return queuedAt; }
    public Instant getRegisteredAt() { return registeredAt; }
    public boolean isActive() { return status == RegistrationStatus.REGISTERED || status == RegistrationStatus.CHECKED_IN; }
    public void register(Instant now) { status = RegistrationStatus.REGISTERED; queuedAt = null; registeredAt = now; }
    public void waitlist(Instant now) { status = RegistrationStatus.WAITLISTED; queuedAt = now; registeredAt = now; }
    public void promote(Instant now) { status = RegistrationStatus.REGISTERED; queuedAt = null; registeredAt = now; }
    public void cancel() { if (status != RegistrationStatus.REGISTERED && status != RegistrationStatus.WAITLISTED && status != RegistrationStatus.CHECKED_IN) throw new IllegalStateException("Registration cannot be cancelled now."); status = RegistrationStatus.CANCELLED; }
    public void checkIn() { if (status != RegistrationStatus.REGISTERED) throw new IllegalStateException("Only a registered player can check in."); status = RegistrationStatus.CHECKED_IN; }
    public void noShow() { if (status != RegistrationStatus.REGISTERED) throw new IllegalStateException("Only a registered player can be marked no-show."); status = RegistrationStatus.NO_SHOW; }
}
