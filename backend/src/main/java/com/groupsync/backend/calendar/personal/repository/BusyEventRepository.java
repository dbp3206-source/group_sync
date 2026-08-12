package com.groupsync.backend.calendar.personal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.calendar.personal.model.BusyEvent;

public interface BusyEventRepository extends JpaRepository<BusyEvent, Long> {
    List<BusyEvent> findByUserIdAndStartAtLessThanAndEndAtGreaterThan(Long userId, java.time.Instant to, java.time.Instant from);
    Optional<BusyEvent> findByIdAndUserId(Long id, Long userId);
}
