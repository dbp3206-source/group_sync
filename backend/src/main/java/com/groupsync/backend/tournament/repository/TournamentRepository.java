package com.groupsync.backend.tournament.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.tournament.model.Tournament;
public interface TournamentRepository extends JpaRepository<Tournament, Long> { List<Tournament> findByGroupIdOrderByCreatedAtDesc(Long groupId); }
