package com.groupsync.backend.knowledge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByTopicIdOrderByCreatedAtDesc(Long topicId);

    Optional<QuizAttempt> findByIdAndOwnerId(Long id, Long ownerId);
}
