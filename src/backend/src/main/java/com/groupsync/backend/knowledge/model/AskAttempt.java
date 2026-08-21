package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "ask_attempts")
public class AskAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private UserAccount owner;
    @Column(name = "session_id", nullable = false) private Long sessionId;
    @Column(name = "user_message_id", nullable = false, unique = true) private Long userMessageId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AskAttemptStatus status = AskAttemptStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(name = "query_mode", length = 30) private QueryMode queryMode;
    @Enumerated(EnumType.STRING) @Column(name = "failure_category", length = 40) private AskFailureCategory failureCategory;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "completed_at") private Instant completedAt;

    protected AskAttempt() { }

    public AskAttempt(UserAccount owner, Long sessionId, Long userMessageId) {
        this.owner = owner;
        this.sessionId = sessionId;
        this.userMessageId = userMessageId;
    }

    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public UserAccount getOwner() { return owner; }
    public Long getSessionId() { return sessionId; }
    public Long getUserMessageId() { return userMessageId; }
    public AskAttemptStatus getStatus() { return status; }
    public QueryMode getQueryMode() { return queryMode; }
    public AskFailureCategory getFailureCategory() { return failureCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void markRunning() { status = AskAttemptStatus.RUNNING; failureCategory = null; }
    public void markComplete(QueryMode mode) { status = AskAttemptStatus.COMPLETE; queryMode = mode; failureCategory = null; completedAt = Instant.now(); }
    public void markFailed(AskFailureCategory category, QueryMode mode) { status = AskAttemptStatus.FAILED; queryMode = mode; failureCategory = category; completedAt = Instant.now(); }
    public void resetForRetry() { status = AskAttemptStatus.PENDING; failureCategory = null; completedAt = null; }
}
