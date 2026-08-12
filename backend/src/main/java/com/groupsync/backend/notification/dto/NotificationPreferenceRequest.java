package com.groupsync.backend.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(@NotNull Boolean enabled) { }
