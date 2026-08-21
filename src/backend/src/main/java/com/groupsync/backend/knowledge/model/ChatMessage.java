package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_session_id", nullable = false) private ChatSession session;
    @Enumerated(EnumType.STRING) @Column(name = "message_role", nullable = false, length = 20) private ChatMessageRole role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(name = "message_status", nullable = false, length = 20) private ChatMessageStatus status = ChatMessageStatus.COMPLETE;
    @Enumerated(EnumType.STRING) @Column(name = "failure_category", length = 40) private AskFailureCategory failureCategory;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected ChatMessage() { }
    public ChatMessage(ChatSession session, ChatMessageRole role, String content) { this.session = session; this.role = role; this.content = content; }
    public ChatMessage(ChatSession session, ChatMessageRole role, String content, ChatMessageStatus status) { this(session, role, content); this.status = status; }
    public Long getId() { return id; } public ChatSession getSession() { return session; } public ChatMessageRole getRole() { return role; } public String getContent() { return content; } public Instant getCreatedAt() { return createdAt; }
    public ChatMessageStatus getStatus() { return status; }
    public AskFailureCategory getFailureCategory() { return failureCategory; }
    public void markComplete() { status = ChatMessageStatus.COMPLETE; failureCategory = null; }
    public void markFailed(AskFailureCategory category) { status = ChatMessageStatus.FAILED; failureCategory = category; }
    public void markPending() { status = ChatMessageStatus.PENDING; failureCategory = null; }
}
