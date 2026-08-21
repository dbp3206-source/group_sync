package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "topic_concepts")
public class TopicConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "why_it_matters", columnDefinition = "TEXT")
    private String whyItMatters;

    @Column(name = "study_status", nullable = false, length = 30)
    private String studyStatus = "NOT_STARTED";

    @Column(nullable = false)
    private int position = 0;

    @Column(name = "stable_key", length = 240)
    private String stableKey;

    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private String lifecycleStatus = "ACTIVE";

    @ManyToMany
    @JoinTable(
        name = "topic_concept_sources",
        joinColumns = @JoinColumn(name = "concept_id"),
        inverseJoinColumns = @JoinColumn(name = "document_chunk_id")
    )
    private Set<DocumentChunk> sourceChunks = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TopicConcept() {}

    public TopicConcept(StudyTopic topic, String title, String summary, String whyItMatters, int position) {
        this.topic = topic;
        this.title = title;
        this.summary = summary;
        this.whyItMatters = whyItMatters;
        this.position = position;
        this.studyStatus = "NOT_STARTED";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public StudyTopic getTopic() { return topic; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; this.updatedAt = Instant.now(); }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; this.updatedAt = Instant.now(); }
    public String getWhyItMatters() { return whyItMatters; }
    public void setWhyItMatters(String whyItMatters) { this.whyItMatters = whyItMatters; this.updatedAt = Instant.now(); }
    public String getStudyStatus() { return studyStatus; }
    public void setStudyStatus(String studyStatus) { this.studyStatus = studyStatus; this.updatedAt = Instant.now(); }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getStableKey() { return stableKey; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public Set<DocumentChunk> getSourceChunks() { return sourceChunks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void markLearning() {
        if (!"CHECKED".equals(this.studyStatus)) {
            this.studyStatus = "LEARNING";
            this.updatedAt = Instant.now();
        }
    }

    public void markReviewNeeded() {
        this.studyStatus = "REVIEW_NEEDED";
        this.updatedAt = Instant.now();
    }

    public void markChecked() {
        this.studyStatus = "CHECKED";
        this.updatedAt = Instant.now();
    }
}
