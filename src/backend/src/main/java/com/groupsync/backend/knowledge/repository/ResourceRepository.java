package com.groupsync.backend.knowledge.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.knowledge.model.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<Resource> findByOwnerIdAndChecksumSha256(Long ownerId, String checksumSha256);
    List<Resource> findByOwnerIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long ownerId, String query);
    List<Resource> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    List<com.groupsync.backend.knowledge.model.Resource> findByProcessingStatus(com.groupsync.backend.knowledge.model.ResourceProcessingStatus processingStatus);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Resource r SET r.processingStatus = :newStatus WHERE r.id = :id AND r.processingStatus = :expectedStatus")
    int updateStatusIfMatches(@Param("id") Long id,
            @Param("expectedStatus") com.groupsync.backend.knowledge.model.ResourceProcessingStatus expectedStatus,
            @Param("newStatus") com.groupsync.backend.knowledge.model.ResourceProcessingStatus newStatus);

    @Query(value = """
            select distinct r.* from resources r
            left join resource_tags rt on rt.resource_id = r.id
            left join resource_collections rc on rc.resource_id = r.id
            where r.owner_id = :ownerId
              and (:query is null or :query = '' or lower(r.title) like lower(concat('%', :query, '%')))
              and (:tagId is null or rt.tag_id = :tagId)
              and (:collectionId is null or rc.collection_id = :collectionId)
            order by r.updated_at desc
            """, nativeQuery = true)
    List<Resource> search(@Param("ownerId") Long ownerId, @Param("query") String query,
            @Param("tagId") Long tagId, @Param("collectionId") Long collectionId);
}
