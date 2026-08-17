package com.groupsync.backend.badminton.dto;

import jakarta.validation.constraints.NotNull;

public record AssignResponsibilityRequest(@NotNull Long userId) { }
