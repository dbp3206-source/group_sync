package com.groupsync.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(min = 2, max = 100) String displayName,
    @NotBlank @Pattern(regexp = "[A-Za-z_]+/[A-Za-z_]+", message = "Time zone must use the Region/City format.") @Size(max = 64) String timeZone
) {
}
