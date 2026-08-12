package com.groupsync.backend.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourtRequest(@NotBlank @Size(max = 80) String name) { }
