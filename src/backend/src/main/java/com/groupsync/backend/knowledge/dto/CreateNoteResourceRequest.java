package com.groupsync.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateNoteResourceRequest(@NotBlank @Size(max = 240) String title, @NotBlank @Size(max = 10000) String content, @Size(max = 5000) String description) { }
