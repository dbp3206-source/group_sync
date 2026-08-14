package com.groupsync.backend.knowledge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<Resource> findByOwnerIdAndChecksumSha256(Long ownerId, String checksumSha256);
    List<Resource> findByOwnerIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long ownerId, String query);
    List<Resource> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
