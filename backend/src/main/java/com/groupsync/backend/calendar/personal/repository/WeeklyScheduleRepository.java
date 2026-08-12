package com.groupsync.backend.calendar.personal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.calendar.personal.model.WeeklySchedule;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {
    List<WeeklySchedule> findByUserId(Long userId);
    Optional<WeeklySchedule> findByIdAndUserId(Long id, Long userId);
}
