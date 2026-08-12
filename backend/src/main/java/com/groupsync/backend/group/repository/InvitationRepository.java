package com.groupsync.backend.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.group.model.Invitation;
import com.groupsync.backend.group.model.InvitationStatus;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsByGroupIdAndInviteeIdAndStatus(Long groupId, Long inviteeId, InvitationStatus status);
    Optional<Invitation> findByIdAndInviteeId(Long id, Long inviteeId);
    List<Invitation> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, InvitationStatus status);
}
