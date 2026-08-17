package com.groupsync.backend.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteUserRequest(@NotBlank @Email String email) {
}
