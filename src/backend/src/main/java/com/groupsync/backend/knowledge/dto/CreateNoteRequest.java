package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
        @NotBlank(message = "Note content is required") String content
) {}
