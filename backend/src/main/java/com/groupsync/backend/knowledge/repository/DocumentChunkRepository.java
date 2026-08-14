package com.groupsync.backend.knowledge.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.DocumentChunk;
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> { List<DocumentChunk> findByResourceIdOrderByChunkIndex(Long resourceId); void deleteByResourceId(Long resourceId); }
