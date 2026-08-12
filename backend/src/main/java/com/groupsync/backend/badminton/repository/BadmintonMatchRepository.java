package com.groupsync.backend.badminton.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.badminton.model.BadmintonMatch;
public interface BadmintonMatchRepository extends JpaRepository<BadmintonMatch, Long> { List<BadmintonMatch> findBySessionGroupIdOrderByCreatedAtDesc(Long groupId); List<BadmintonMatch> findBySessionIdOrderByRoundNumberAscIdAsc(Long sessionId); Optional<BadmintonMatch> findByIdAndSessionGroupId(Long id, Long groupId); }
