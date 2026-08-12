package com.groupsync.backend.study.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.study.model.StudySession;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByGroupIdOrderByStartAtAsc(Long groupId);
    Optional<StudySession> findByIdAndGroupId(Long id, Long groupId);
}
