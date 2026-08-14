package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private UserAccount owner;
    @Column(nullable = false, length = 240) private String title;
    @Enumerated(EnumType.STRING) @Column(name = "scope_type", nullable = false, length = 30) private RetrievalScope scopeType;
    @Column(name = "collection_id") private Long collectionId;
    @ManyToMany @JoinTable(name = "chat_session_resources", joinColumns = @JoinColumn(name = "chat_session_id"), inverseJoinColumns = @JoinColumn(name = "resource_id"))
    private Set<Resource> resources = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected ChatSession() { }
    public ChatSession(UserAccount owner, String title, RetrievalScope scopeType, Long collectionId, Set<Resource> resources) { this.owner = owner; this.title = title; this.scopeType = scopeType; this.collectionId = collectionId; this.resources = new LinkedHashSet<>(resources); }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; } public UserAccount getOwner() { return owner; } public String getTitle() { return title; } public RetrievalScope getScopeType() { return scopeType; } public Long getCollectionId() { return collectionId; } public Set<Resource> getResources() { return Set.copyOf(resources); }
}
