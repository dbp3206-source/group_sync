package com.groupsync.backend.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(@NotBlank @Size(max = 160) String name, @Size(max = 300) String address) { }
