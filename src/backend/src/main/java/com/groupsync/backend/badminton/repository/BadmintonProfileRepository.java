package com.groupsync.backend.badminton.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.badminton.model.BadmintonProfile;

public interface BadmintonProfileRepository extends JpaRepository<BadmintonProfile, Long> {
    Optional<BadmintonProfile> findByMembershipId(Long membershipId);
}
