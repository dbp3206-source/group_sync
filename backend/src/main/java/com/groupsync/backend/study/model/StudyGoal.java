package com.groupsync.backend.study.model;

import jakarta.persistence.*;

@Entity @Table(name = "study_goals")
public class StudyGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private StudySession session;
    @Column(nullable = false, length = 300) private String description;
    @Column(nullable = false) private boolean completed;
    protected StudyGoal() { }
    public StudyGoal(StudySession session, String description) { this.session = session; this.description = description; }
    public Long getId() { return id; } public StudySession getSession() { return session; } public String getDescription() { return description; } public boolean isCompleted() { return completed; } public void toggle() { completed = !completed; }
}
