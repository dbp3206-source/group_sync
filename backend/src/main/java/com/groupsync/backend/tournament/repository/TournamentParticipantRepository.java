package com.groupsync.backend.tournament.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.tournament.model.*;
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, TournamentParticipantId> { List<TournamentParticipant> findByTournamentIdOrderByRegisteredAtAsc(Long tournamentId); Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId); long countByTournamentId(Long tournamentId); }
