package com.groupsync.backend.badminton.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.groupsync.backend.badminton.model.BadmintonSession;

import jakarta.persistence.LockModeType;

public interface BadmintonSessionRepository extends JpaRepository<BadmintonSession, Long> {
    List<BadmintonSession> findByGroupIdOrderByStartAtDesc(Long groupId);
    Optional<BadmintonSession> findByIdAndGroupId(Long id, Long groupId);

    @Query("select s from BadmintonSession s where s.id = :id")
    Optional<BadmintonSession> findForOperations(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BadmintonSession s where s.id = :id")
    Optional<BadmintonSession> findByIdForUpdate(Long id);
}
