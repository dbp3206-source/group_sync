package com.groupsync.backend.badminton.dto;

import com.groupsync.backend.badminton.model.BadmintonSkillLevel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileRequest(@NotNull BadmintonSkillLevel skillLevel, @Size(max = 500) String bio) { }
