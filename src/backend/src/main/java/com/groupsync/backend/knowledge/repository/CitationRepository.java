package com.groupsync.backend.knowledge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.groupsync.backend.knowledge.model.Citation;

public interface CitationRepository extends JpaRepository<Citation, Long> {
    List<Citation> findByMessageIdOrderByCitationOrderAsc(Long messageId);

    /** Deletes all citations whose referenced chunk belongs to the given resource.
     *  Must be called before deleting document_chunks to satisfy ON DELETE RESTRICT. */
    @Modifying
    @Query("delete from Citation c where c.chunk.resource.id = :resourceId")
    void deleteByChunkResourceId(@Param("resourceId") Long resourceId);
}
