package com.groupsync.backend.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResponsibilityRequest(@NotBlank @Size(max = 100) String itemName, @Size(max = 300) String note) { }
