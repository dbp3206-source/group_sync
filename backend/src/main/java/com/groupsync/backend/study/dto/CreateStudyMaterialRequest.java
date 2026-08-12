package com.groupsync.backend.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudyMaterialRequest(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 1000) String url) { }
