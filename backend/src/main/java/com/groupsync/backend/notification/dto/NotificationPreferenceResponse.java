package com.groupsync.backend.notification.dto;

import com.groupsync.backend.notification.model.NotificationPreference;

public record NotificationPreferenceResponse(String type, boolean enabled) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) { return new NotificationPreferenceResponse(preference.getType(), preference.isEnabled()); }
}
