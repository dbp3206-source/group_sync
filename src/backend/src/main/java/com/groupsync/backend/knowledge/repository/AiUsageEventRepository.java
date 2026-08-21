package com.groupsync.backend.knowledge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.AiUsageEvent;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, Long> {
    List<AiUsageEvent> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
