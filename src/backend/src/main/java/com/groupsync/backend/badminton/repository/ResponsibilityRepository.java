package com.groupsync.backend.badminton.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.badminton.model.SessionResponsibility;

public interface ResponsibilityRepository extends JpaRepository<SessionResponsibility, Long> {
    List<SessionResponsibility> findBySessionIdOrderByItemNameAsc(Long sessionId);
    List<SessionResponsibility> findBySessionIdAndAssigneeId(Long sessionId, Long userId);
}
