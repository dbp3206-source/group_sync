package com.groupsync.backend.knowledge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.groupsync.backend.knowledge.model.StudyTopic;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {

    List<StudyTopic> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    Optional<StudyTopic> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT t FROM StudyTopic t JOIN t.resources r WHERE t.owner.id = :ownerId AND r.id = :resourceId")
    List<StudyTopic> findByOwnerIdAndResourceId(@Param("ownerId") Long ownerId, @Param("resourceId") Long resourceId);
}
