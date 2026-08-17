package com.groupsync.backend.knowledge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
