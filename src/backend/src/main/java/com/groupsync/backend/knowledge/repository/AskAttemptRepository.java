package com.groupsync.backend.knowledge.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.AskAttempt;

public interface AskAttemptRepository extends JpaRepository<AskAttempt, Long> {
    Optional<AskAttempt> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<AskAttempt> findByUserMessageIdAndOwnerId(Long userMessageId, Long ownerId);
}
