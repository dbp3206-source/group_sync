package com.groupsync.backend.badminton.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AnnouncementRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 1000) String content) { }
