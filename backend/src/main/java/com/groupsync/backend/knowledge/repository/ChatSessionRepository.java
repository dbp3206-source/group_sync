package com.groupsync.backend.knowledge.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByIdAndOwnerId(Long id, Long ownerId);
    List<ChatSession> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
