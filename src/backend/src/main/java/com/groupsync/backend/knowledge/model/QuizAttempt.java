package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import com.groupsync.backend.user.model.UserAccount;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private StudyTopic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private TopicConcept concept;

    @Column(name = "score_correct", nullable = false)
    private int scoreCorrect = 0;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuizAttempt() {}

    public QuizAttempt(StudyTopic topic, UserAccount owner, TopicConcept concept, int totalQuestions) {
        this.topic = topic;
        this.owner = owner;
        this.concept = concept;
        this.totalQuestions = totalQuestions;
        this.scoreCorrect = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public StudyTopic getTopic() { return topic; }
    public UserAccount getOwner() { return owner; }
    public TopicConcept getConcept() { return concept; }
    public int getScoreCorrect() { return scoreCorrect; }
    public void setScoreCorrect(int scoreCorrect) { this.scoreCorrect = scoreCorrect; }
    public int getTotalQuestions() { return totalQuestions; }
    public List<QuizItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }

    public void addItem(QuizItem item) {
        this.items.add(item);
    }
}
