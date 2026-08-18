package com.groupsync.backend.knowledge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.groupsync.backend.knowledge.model.TopicConcept;

public interface TopicConceptRepository extends JpaRepository<TopicConcept, Long> {

    List<TopicConcept> findByTopicIdOrderByPositionAsc(Long topicId);

    Optional<TopicConcept> findByIdAndTopicId(Long id, Long topicId);

    @Query("SELECT c FROM TopicConcept c WHERE c.topic.owner.id = :ownerId AND c.studyStatus = 'REVIEW_NEEDED' ORDER BY c.updatedAt DESC")
    List<TopicConcept> findReviewNeededByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM TopicConcept c WHERE c.topic.owner.id = :ownerId AND c.studyStatus IN ('REVIEW_NEEDED', 'LEARNING') ORDER BY CASE WHEN c.studyStatus = 'REVIEW_NEEDED' THEN 0 ELSE 1 END, c.updatedAt DESC")
    List<TopicConcept> findActiveQueueByOwnerId(@Param("ownerId") Long ownerId);
}
