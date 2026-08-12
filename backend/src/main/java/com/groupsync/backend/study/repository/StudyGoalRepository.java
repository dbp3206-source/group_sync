package com.groupsync.backend.study.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.study.model.StudyGoal;

public interface StudyGoalRepository extends JpaRepository<StudyGoal, Long> {
    List<StudyGoal> findBySessionIdOrderByIdAsc(Long sessionId);
}
