package com.groupsync.backend.study.model;

import java.time.Instant;

import com.groupsync.backend.group.model.Group;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_sessions")
public class StudySession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organizer_id", nullable = false) private UserAccount organizer;
    @Column(nullable = false, length = 160) private String topic;
    @Column(length = 500) private String goal;
    @Column(length = 240) private String location;
    @Column(name = "start_at", nullable = false) private Instant startAt;
    @Column(name = "end_at", nullable = false) private Instant endAt;
    private Integer capacity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StudySessionStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected StudySession() { }
    public StudySession(Group group, UserAccount organizer, String topic, String goal, String location, Instant startAt, Instant endAt, Integer capacity) {
        this.group = group; this.organizer = organizer; this.topic = topic; this.goal = goal; this.location = location; this.startAt = startAt; this.endAt = endAt; this.capacity = capacity; this.status = StudySessionStatus.OPEN;
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public UserAccount getOrganizer() { return organizer; }
    public String getTopic() { return topic; }
    public String getGoal() { return goal; }
    public String getLocation() { return location; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public Integer getCapacity() { return capacity; }
    public StudySessionStatus getStatus() { return status; }
    public void reschedule(Instant startAt, Instant endAt) { this.startAt = startAt; this.endAt = endAt; }
    public void complete() { requireStatus(StudySessionStatus.CONFIRMED); status = StudySessionStatus.COMPLETED; }
    public void cancel() { if (status == StudySessionStatus.COMPLETED || status == StudySessionStatus.CANCELLED) throw new IllegalStateException("Study session cannot be cancelled now."); status = StudySessionStatus.CANCELLED; }
    public void confirm() { requireStatus(StudySessionStatus.OPEN); status = StudySessionStatus.CONFIRMED; }
    private void requireStatus(StudySessionStatus expected) { if (status != expected) throw new IllegalStateException("Study session must be " + expected + "."); }
}
