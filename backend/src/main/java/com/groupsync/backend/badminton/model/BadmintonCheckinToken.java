package com.groupsync.backend.badminton.model;

import java.time.Instant;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "badminton_checkin_tokens")
public class BadmintonCheckinToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private BadmintonSession session;
    @Column(nullable = false, unique = true, length = 120) private String token;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private UserAccount createdBy;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected BadmintonCheckinToken() { }
    public BadmintonCheckinToken(BadmintonSession session, String token, UserAccount createdBy, Instant expiresAt) { this.session = session; this.token = token; this.createdBy = createdBy; this.expiresAt = expiresAt; }
    public Long getId() { return id; } public BadmintonSession getSession() { return session; } public String getToken() { return token; } public Instant getExpiresAt() { return expiresAt; }
}
