package com.groupsync.backend.study.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.study.model.StudyMaterial;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findBySessionIdOrderByIdAsc(Long sessionId);
}
