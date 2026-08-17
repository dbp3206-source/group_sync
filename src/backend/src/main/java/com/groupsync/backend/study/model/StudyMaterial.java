package com.groupsync.backend.study.model;

import com.groupsync.backend.study.model.StudySession;
import jakarta.persistence.*;

@Entity @Table(name = "study_materials")
public class StudyMaterial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private StudySession session;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 1000) private String url;
    protected StudyMaterial() { }
    public StudyMaterial(StudySession session, String title, String url) { this.session = session; this.title = title; this.url = url; }
    public Long getId() { return id; } public StudySession getSession() { return session; } public String getTitle() { return title; } public String getUrl() { return url; }
}
