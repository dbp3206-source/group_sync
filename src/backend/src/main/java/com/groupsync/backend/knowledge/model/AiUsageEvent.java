package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "ai_usage_events")
public class AiUsageEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private UserAccount owner;
    @Column(name = "attempt_id") private Long attemptId;
    @Column(nullable = false, length = 40) private String provider;
    @Column(nullable = false, length = 120) private String model;
    @Column(name = "request_status", nullable = false, length = 20) private String requestStatus;
    @Enumerated(EnumType.STRING) @Column(name = "query_mode", length = 30) private QueryMode queryMode;
    @Column(name = "prompt_tokens") private Integer promptTokens;
    @Column(name = "output_tokens") private Integer outputTokens;
    @Column(name = "total_tokens") private Integer totalTokens;
    @Column(name = "context_chars") private Integer contextChars;
    @Column(name = "retrieved_chunks") private Integer retrievedChunks;
    @Column(name = "duration_ms") private Long durationMs;
    @Enumerated(EnumType.STRING) @Column(name = "failure_category", length = 40) private AskFailureCategory failureCategory;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected AiUsageEvent() { }
    public AiUsageEvent(UserAccount owner, Long attemptId, String provider, String model, String requestStatus,
                        QueryMode queryMode, Integer promptTokens, Integer outputTokens, Integer totalTokens,
                        Integer contextChars, Integer retrievedChunks, Long durationMs, AskFailureCategory failureCategory) {
        this.owner = owner; this.attemptId = attemptId; this.provider = provider; this.model = model;
        this.requestStatus = requestStatus; this.queryMode = queryMode; this.promptTokens = promptTokens;
        this.outputTokens = outputTokens; this.totalTokens = totalTokens; this.contextChars = contextChars;
        this.retrievedChunks = retrievedChunks; this.durationMs = durationMs; this.failureCategory = failureCategory;
    }
    public Long getId() { return id; }
    public Long getAttemptId() { return attemptId; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getRequestStatus() { return requestStatus; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public AskFailureCategory getFailureCategory() { return failureCategory; }
    public Instant getCreatedAt() { return createdAt; }
}
