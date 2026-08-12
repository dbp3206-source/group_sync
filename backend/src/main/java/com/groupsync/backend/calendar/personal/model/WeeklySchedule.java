package com.groupsync.backend.calendar.personal.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.groupsync.backend.user.model.UserAccount;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "weekly_schedules")
public class WeeklySchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 160)
    private String title;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "weekly_schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> weekdays = new LinkedHashSet<>();

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(length = 500) private String description;
    @Column(length = 40) private String category;
    @Column(length = 200) private String location;
    @Column(nullable = false, length = 20) private String visibility = "PRIVATE";
    @Column(name = "reminder_minutes") private Integer reminderMinutes;
    @Column(nullable = false, length = 20) private String frequency = "WEEKLY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected WeeklySchedule() {
    }

    public WeeklySchedule(
        UserAccount user,
        String title,
        Set<DayOfWeek> weekdays,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validUntil,
        String timezone
    ) {
        this.user = user;
        this.title = title;
        this.weekdays = new LinkedHashSet<>(weekdays);
        this.startTime = startTime;
        this.endTime = endTime;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.timezone = timezone;
    }

    public WeeklySchedule(UserAccount user, String title, Set<DayOfWeek> weekdays, LocalTime startTime, LocalTime endTime, LocalDate validFrom, LocalDate validUntil, String timezone, String description, String category, String location, String visibility, Integer reminderMinutes, String frequency) {
        this(user, title, weekdays, startTime, endTime, validFrom, validUntil, timezone);
        updateDetails(description, category, location, visibility, reminderMinutes, frequency);
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getTitle() { return title; }
    public Set<DayOfWeek> getWeekdays() { return weekdays; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getTimezone() { return timezone; }
    public void update(String title, Set<DayOfWeek> weekdays, LocalTime startTime, LocalTime endTime, LocalDate validFrom, LocalDate validUntil, String timezone) { update(title, weekdays, startTime, endTime, validFrom, validUntil, timezone, description, category, location, visibility, reminderMinutes, frequency); }
    public void update(String title, Set<DayOfWeek> weekdays, LocalTime startTime, LocalTime endTime, LocalDate validFrom, LocalDate validUntil, String timezone, String description, String category, String location, String visibility, Integer reminderMinutes, String frequency) {
        this.title = title;
        this.weekdays = new LinkedHashSet<>(weekdays);
        this.startTime = startTime;
        this.endTime = endTime;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.timezone = timezone;
        updateDetails(description, category, location, visibility, reminderMinutes, frequency);
    }
    private void updateDetails(String description, String category, String location, String visibility, Integer reminderMinutes, String frequency) {
        this.description = description; this.category = category; this.location = location;
        this.visibility = visibility == null || visibility.isBlank() ? "PRIVATE" : visibility;
        this.reminderMinutes = reminderMinutes; this.frequency = frequency == null || frequency.isBlank() ? "WEEKLY" : frequency;
    }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getVisibility() { return visibility; }
    public Integer getReminderMinutes() { return reminderMinutes; }
    public String getFrequency() { return frequency; }
}
