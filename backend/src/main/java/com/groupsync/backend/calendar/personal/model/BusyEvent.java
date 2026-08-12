package com.groupsync.backend.calendar.personal.model;

import java.time.Instant;

import com.groupsync.backend.user.model.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "personal_busy_events")
public class BusyEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(length = 500) private String description;
    @Column(length = 40) private String category;
    @Column(length = 200) private String location;
    @Column(nullable = false, length = 20) private String visibility = "PRIVATE";
    @Column(name = "reminder_minutes") private Integer reminderMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected BusyEvent() {
    }

    public BusyEvent(UserAccount user, String title, Instant startAt, Instant endAt) {
        this.user = user;
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public BusyEvent(UserAccount user, String title, Instant startAt, Instant endAt, String description, String category, String location, String visibility, Integer reminderMinutes) {
        this(user, title, startAt, endAt);
        updateDetails(description, category, location, visibility, reminderMinutes);
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getTitle() { return title; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public void update(String title, Instant startAt, Instant endAt) { update(title, startAt, endAt, description, category, location, visibility, reminderMinutes); }
    public void update(String title, Instant startAt, Instant endAt, String description, String category, String location, String visibility, Integer reminderMinutes) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        updateDetails(description, category, location, visibility, reminderMinutes);
    }
    private void updateDetails(String description, String category, String location, String visibility, Integer reminderMinutes) {
        this.description = description; this.category = category; this.location = location;
        this.visibility = visibility == null || visibility.isBlank() ? "PRIVATE" : visibility;
        this.reminderMinutes = reminderMinutes;
    }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getVisibility() { return visibility; }
    public Integer getReminderMinutes() { return reminderMinutes; }
}
