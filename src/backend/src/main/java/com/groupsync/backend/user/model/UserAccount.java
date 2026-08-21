package com.groupsync.backend.user.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 20)
    private SystemRole systemRole = SystemRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserAccount() {
    }

    public UserAccount(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.profileCompleted = hasRequiredProfileFields();
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getTimeZone() { return timeZone; }
    public boolean isProfileCompleted() { return hasRequiredProfileFields(); }
    public SystemRole getSystemRole() { return systemRole; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateProfile(String displayName, String timeZone) {
        this.displayName = displayName;
        this.timeZone = timeZone;
        this.profileCompleted = hasRequiredProfileFields();
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    private boolean hasRequiredProfileFields() {
        return displayName != null && !displayName.isBlank();
    }
}
