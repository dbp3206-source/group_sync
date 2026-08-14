package com.groupsync.backend.knowledge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.Citation;

public interface CitationRepository extends JpaRepository<Citation, Long> { }
