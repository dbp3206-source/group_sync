package com.groupsync.backend.knowledge.model;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_items")
public class QuizItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private TopicConcept concept;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_option", nullable = false)
    private int correctOption;

    @Column(name = "user_answer")
    private Integer userAnswer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_resource_id")
    private Resource sourceResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_chunk_id")
    private DocumentChunk sourceChunk;

    @Column(name = "source_snippet", columnDefinition = "TEXT")
    private String sourceSnippet;

    protected QuizItem() {}

    public QuizItem(QuizAttempt attempt, TopicConcept concept, String question, String optionsJson,
                    int correctOption, String explanation, Resource sourceResource,
                    DocumentChunk sourceChunk, String sourceSnippet) {
        this.attempt = attempt;
        this.concept = concept;
        this.question = question;
        this.optionsJson = optionsJson;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.sourceResource = sourceResource;
        this.sourceChunk = sourceChunk;
        this.sourceSnippet = sourceSnippet;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QuizAttempt getAttempt() { return attempt; }
    public TopicConcept getConcept() { return concept; }
    public String getQuestion() { return question; }
    public String getOptionsJson() { return optionsJson; }
    public int getCorrectOption() { return correctOption; }
    public Integer getUserAnswer() { return userAnswer; }
    public void setUserAnswer(Integer userAnswer) { this.userAnswer = userAnswer; }
    public String getExplanation() { return explanation; }
    public Resource getSourceResource() { return sourceResource; }
    public DocumentChunk getSourceChunk() { return sourceChunk; }
    public String getSourceSnippet() { return sourceSnippet; }

    public boolean isCorrect() {
        return userAnswer != null && userAnswer == correctOption;
    }
}
