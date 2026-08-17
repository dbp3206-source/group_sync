package com.groupsync.backend.tournament.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.tournament.model.TournamentEntry;

public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {
    List<TournamentEntry> findByTournamentIdOrderBySeedNumberAscCreatedAtAsc(Long tournamentId);
    long countByTournamentId(Long tournamentId);
    boolean existsByTournamentIdAndMembersUserId(Long tournamentId, Long userId);
    void deleteByTournamentId(Long tournamentId);
}
