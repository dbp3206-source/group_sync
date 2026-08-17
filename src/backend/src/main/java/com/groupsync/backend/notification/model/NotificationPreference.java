package com.groupsync.backend.notification.model;

import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_notification_preference_user_type", columnNames = {"user_id", "notification_type"}))
public class NotificationPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "notification_type", nullable = false, length = 60) private String type;
    @Column(nullable = false) private boolean enabled = true;
    protected NotificationPreference() { }
    public NotificationPreference(UserAccount user, String type, boolean enabled) { this.user = user; this.type = type; this.enabled = enabled; }
    public Long getId() { return id; }
    public String getType() { return type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
