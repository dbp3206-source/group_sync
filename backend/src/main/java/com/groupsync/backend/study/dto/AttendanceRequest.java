package com.groupsync.backend.study.dto;

import com.groupsync.backend.study.model.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(@NotNull AttendanceStatus attendance) { }
