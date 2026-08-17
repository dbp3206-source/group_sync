package com.groupsync.backend.badminton.repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.badminton.model.BadmintonMatch;
public interface BadmintonMatchRepository extends JpaRepository<BadmintonMatch, Long> { List<BadmintonMatch> findBySessionGroupIdOrderByCreatedAtDesc(Long groupId); List<BadmintonMatch> findBySessionIdOrderByRoundNumberAscIdAsc(Long sessionId); Optional<BadmintonMatch> findByIdAndSessionGroupId(Long id, Long groupId); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from BadmintonMatch m where m.id = :id") Optional<BadmintonMatch> findByIdForUpdate(@Param("id") Long id); }
