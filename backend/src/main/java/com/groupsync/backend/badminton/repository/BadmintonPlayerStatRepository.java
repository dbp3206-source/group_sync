package com.groupsync.backend.badminton.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.groupsync.backend.badminton.model.BadmintonPlayerStat;
public interface BadmintonPlayerStatRepository extends JpaRepository<BadmintonPlayerStat, Long> { Optional<BadmintonPlayerStat> findByGroupIdAndSeasonIdAndUserId(Long groupId, Long seasonId, Long userId); @Query("select s from BadmintonPlayerStat s join fetch s.user where s.group.id = :groupId and s.season.id = :seasonId order by s.points desc, s.wins desc, s.matchesPlayed asc, s.user.displayName asc") List<BadmintonPlayerStat> findLeaderboard(Long groupId, Long seasonId); }
