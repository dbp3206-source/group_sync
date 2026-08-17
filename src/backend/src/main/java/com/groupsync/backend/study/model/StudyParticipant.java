package com.groupsync.backend.study.model;

import java.time.Instant;

import com.groupsync.backend.user.model.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "study_participants", uniqueConstraints = @UniqueConstraint(name = "uk_study_participant", columnNames = {"session_id", "user_id"}))
public class StudyParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id", nullable = false) private StudySession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AttendanceStatus attendance = AttendanceStatus.REGISTERED;
    @Column(name = "joined_at", nullable = false, updatable = false) private Instant joinedAt = Instant.now();
    protected StudyParticipant() { }
    public StudyParticipant(StudySession session, UserAccount user) { this.session = session; this.user = user; }
    public Long getId() { return id; }
    public StudySession getSession() { return session; }
    public UserAccount getUser() { return user; }
    public AttendanceStatus getAttendance() { return attendance; }
    public void markAttendance(AttendanceStatus attendance) { this.attendance = attendance; }
}
