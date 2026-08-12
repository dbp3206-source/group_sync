package com.groupsync.backend.badminton.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.badminton.model.RankingHistory;
public interface RankingHistoryRepository extends JpaRepository<RankingHistory, Long> { List<RankingHistory> findByUserIdAndSeasonIdOrderByCreatedAtDesc(Long userId, Long seasonId); List<RankingHistory> findByGroupIdAndSeasonIdAndUserIdOrderByCreatedAtDesc(Long groupId, Long seasonId, Long userId); boolean existsByMatchIdAndUserId(Long matchId, Long userId); }
