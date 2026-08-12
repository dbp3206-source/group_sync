package com.groupsync.backend.badminton.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.badminton.model.Court;

public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByVenueIdOrderByNameAsc(Long venueId);
}
