package com.groupsync.backend.study.dto;

import com.groupsync.backend.study.model.StudyMaterial;

public record MaterialResponse(Long id, String title, String url) {
    public static MaterialResponse from(StudyMaterial material) { return new MaterialResponse(material.getId(), material.getTitle(), material.getUrl()); }
}
