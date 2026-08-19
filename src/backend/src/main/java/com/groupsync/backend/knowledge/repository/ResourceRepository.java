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
            select r.* from resources r
            where r.owner_id = :ownerId
              and (:query is null or :query = '' or lower(r.title) like lower(concat('%', :query, '%')))
              and (:tagId is null or exists (select 1 from resource_tags rt where rt.resource_id = r.id and rt.tag_id = :tagId))
              and (:collectionId is null or exists (select 1 from resource_collections rc where rc.resource_id = r.id and rc.collection_id = :collectionId))
            order by r.updated_at desc
            """, nativeQuery = true)
    List<Resource> search(@Param("ownerId") Long ownerId, @Param("query") String query,
            @Param("tagId") Long tagId, @Param("collectionId") Long collectionId);

    @Query(value = """
            select r.* from resources r
            where r.owner_id = :ownerId
              and (:query is null or :query = '' or lower(r.title) like lower(concat('%', :query, '%')))
              and (:tagId is null or exists (select 1 from resource_tags rt where rt.resource_id = r.id and rt.tag_id = :tagId))
              and (:collectionId is null or exists (select 1 from resource_collections rc where rc.resource_id = r.id and rc.collection_id = :collectionId))
            order by
              case when :sort = 'updated_desc' then r.updated_at end desc nulls last,
              case when :sort = 'created_desc' then r.created_at end desc nulls last,
              case when :sort = 'title_asc' then lower(r.title) end asc nulls last,
              case when :sort = 'title_desc' then lower(r.title) end desc nulls last,
              r.id desc
            """,
            countQuery = """
            select count(r.id) from resources r
            where r.owner_id = :ownerId
              and (:query is null or :query = '' or lower(r.title) like lower(concat('%', :query, '%')))
              and (:tagId is null or exists (select 1 from resource_tags rt where rt.resource_id = r.id and rt.tag_id = :tagId))
              and (:collectionId is null or exists (select 1 from resource_collections rc where rc.resource_id = r.id and rc.collection_id = :collectionId))
            """,
            nativeQuery = true)
    org.springframework.data.domain.Page<Resource> searchPaged(
            @Param("ownerId") Long ownerId,
            @Param("query") String query,
            @Param("tagId") Long tagId,
            @Param("collectionId") Long collectionId,
            @Param("sort") String sort,
            org.springframework.data.domain.Pageable pageable);
}
