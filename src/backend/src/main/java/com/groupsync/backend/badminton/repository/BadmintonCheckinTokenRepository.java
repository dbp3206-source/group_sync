package com.groupsync.backend.badminton.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.badminton.model.BadmintonCheckinToken;

public interface BadmintonCheckinTokenRepository extends JpaRepository<BadmintonCheckinToken, Long> { Optional<BadmintonCheckinToken> findByToken(String token); }
