package com.groupsync.backend.knowledge.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.model.AskAttempt;
import com.groupsync.backend.knowledge.model.AskAttemptStatus;
import com.groupsync.backend.knowledge.model.AskFailureCategory;

public interface AskAttemptRepository extends JpaRepository<AskAttempt, Long> {
    Optional<AskAttempt> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<AskAttempt> findByUserMessageIdAndOwnerId(Long userMessageId, Long ownerId);

    @Transactional
    @Modifying
    @Query("update AskAttempt a set a.status = :next, a.failureCategory = null, a.completedAt = null "
            + "where a.id = :id and a.owner.id = :ownerId and a.status = :expected")
    int claimRetry(@Param("id") Long id, @Param("ownerId") Long ownerId,
                   @Param("expected") AskAttemptStatus expected, @Param("next") AskAttemptStatus next);

    @Transactional
    @Modifying
    @Query("update AskAttempt a set a.status = :next where a.id = :id and a.status = :expected")
    int claimExecution(@Param("id") Long id, @Param("expected") AskAttemptStatus expected,
                       @Param("next") AskAttemptStatus next);

    @Transactional
    @Modifying
    @Query("update AskAttempt a set a.status = :failed, a.failureCategory = :category, "
            + "a.completedAt = CURRENT_TIMESTAMP where a.id = :id and a.owner.id = :ownerId "
            + "and a.status in (:pending, :running)")
    int markInterruptedIfActive(@Param("id") Long id, @Param("ownerId") Long ownerId,
                                @Param("pending") AskAttemptStatus pending,
                                @Param("running") AskAttemptStatus running,
                                @Param("failed") AskAttemptStatus failed,
                                @Param("category") AskFailureCategory category);
}
