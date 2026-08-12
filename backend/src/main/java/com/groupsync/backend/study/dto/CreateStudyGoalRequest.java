package com.groupsync.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudyGoalRequest(@NotBlank @Size(max = 300) String description) { }
