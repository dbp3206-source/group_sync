package com.groupsync.backend.tournament.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.groupsync.backend.tournament.model.TournamentMatch;
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentIdOrderByStageAscMatchNumberAsc(Long tournamentId);
    Optional<TournamentMatch> findByTournamentIdAndMatchNumber(Long tournamentId, int matchNumber);
    List<TournamentMatch> findByTournamentIdAndNextMatchNumber(Long tournamentId, Integer nextMatchNumber);
    @Query("select tm from TournamentMatch tm join fetch tm.match m where m.id = :matchId")
    Optional<TournamentMatch> findByMatchId(@Param("matchId") Long matchId);
}
