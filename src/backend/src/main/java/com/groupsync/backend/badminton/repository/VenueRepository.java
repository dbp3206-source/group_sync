package com.groupsync.backend.badminton.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.badminton.model.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByGroupIdOrderByNameAsc(Long groupId);
    Optional<Venue> findByIdAndGroupId(Long id, Long groupId);
}
