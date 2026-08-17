package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
public record UpdateResourceRequest(@NotBlank @Size(max = 240) String title, @Size(max = 5000) String description, boolean favorite, @Min(0) @Max(5) int priority) { }
