package com.groupsync.backend.study.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.study.model.StudyParticipant;

public interface StudyParticipantRepository extends JpaRepository<StudyParticipant, Long> {
    List<StudyParticipant> findBySessionIdOrderByJoinedAtAsc(Long sessionId);
    List<StudyParticipant> findByUserId(Long userId);
    Optional<StudyParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);
    long countBySessionId(Long sessionId);
}
