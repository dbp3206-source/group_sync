package com.groupsync.backend.badminton.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.badminton.model.Season;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    List<Season> findByGroupIdOrderByStartsOnDesc(Long groupId);
    Optional<Season> findByGroupIdAndActiveTrue(Long groupId);
}
