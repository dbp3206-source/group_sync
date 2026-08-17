package com.groupsync.backend.badminton.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.badminton.model.BadmintonAllocation;
public interface BadmintonAllocationRepository extends JpaRepository<BadmintonAllocation, Long> { List<BadmintonAllocation> findBySessionIdOrderByRoundNumberAscIdAsc(Long sessionId); }
