package com.groupsync.backend.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.group.model.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId);
    List<Membership> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Membership> findByGroupIdOrderByCreatedAtAsc(Long groupId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupIdAndRole(Long groupId, com.groupsync.backend.group.model.GroupRole role);
}
