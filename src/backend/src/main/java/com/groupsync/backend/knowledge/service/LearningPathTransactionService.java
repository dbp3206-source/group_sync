package com.groupsync.backend.knowledge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.dto.ResourceDeepDiveResponse;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Owns only short database transactions. No provider call is made from this class. */
@Service
public class LearningPathTransactionService {
    private static final int SOURCE_MAP_LIMIT = 12;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public LearningPathTransactionService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<LearningAreaResponse> listAreas(Long ownerId) {
        return jdbc.query("""
                select c.id collection_id,c.name,c.description,c.updated_at collection_updated,
                       st.id area_id,st.goal,st.refresh_status,st.current_version,st.generation_failure,st.updated_at area_updated,
                       (select count(*) from resource_collections rc join resources r on r.id=rc.resource_id
                        where rc.collection_id=c.id and r.owner_id=:owner and r.processing_status='READY') source_count,
                       (select count(*) from learning_modules lm join learning_path_versions lpv on lpv.id=lm.path_version_id
                        where lpv.topic_id=st.id and lpv.version_number=st.current_version) module_count,
                       (select count(*) from learning_module_concepts lmc join learning_path_versions lpv on lpv.id=lmc.path_version_id
                        where lpv.topic_id=st.id and lpv.version_number=st.current_version) concept_count,
                       (select count(*) from learning_module_concepts lmc join learning_path_versions lpv on lpv.id=lmc.path_version_id
                        join topic_concepts tc on tc.id=lmc.concept_id where lpv.topic_id=st.id and lpv.version_number=st.current_version and tc.study_status='CHECKED') checked_count,
                       (select count(*) from learning_module_concepts lmc join learning_path_versions lpv on lpv.id=lmc.path_version_id
                        join topic_concepts tc on tc.id=lmc.concept_id where lpv.topic_id=st.id and lpv.version_number=st.current_version and tc.study_status='REVIEW_NEEDED') review_count,
                       (select count(*) from learning_module_concepts lmc join learning_path_versions lpv on lpv.id=lmc.path_version_id
                        join topic_concepts tc on tc.id=lmc.concept_id where lpv.topic_id=st.id and lpv.version_number=st.current_version and tc.study_status='LEARNING') learning_count,
                       (select count(*) from learning_module_concepts lmc join learning_path_versions lpv on lpv.id=lmc.path_version_id
                        join topic_concepts tc on tc.id=lmc.concept_id where lpv.topic_id=st.id and lpv.version_number=st.current_version and tc.study_status='NOT_STARTED') not_started_count,
                       (select count(*) from resource_collections rc join resources r on r.id=rc.resource_id
                        where rc.collection_id=c.id and r.owner_id=:owner and r.processing_status='READY'
                          and (st.id is null or not exists(select 1 from study_topic_resources str where str.topic_id=st.id and str.resource_id=r.id))) new_source_count
                from collections c
                left join study_topics st on st.collection_id=c.id and st.learning_area_type='COLLECTION_BACKED' and st.status='ACTIVE'
                where c.owner_id=:owner
                order by coalesce(st.updated_at,c.updated_at) desc,c.name
                """, Map.of("owner", ownerId), (rs, rowNum) -> new LearningAreaResponse(
                nullableLong(rs, "area_id"), rs.getLong("collection_id"), rs.getString("name"),
                coalesce(rs.getString("goal"), rs.getString("description"), "Học có hệ thống từ các nguồn trong Collection này."),
                rs.getInt("source_count"), rs.getInt("module_count"), rs.getInt("concept_count"),
                rs.getInt("checked_count"), rs.getInt("review_count"), rs.getInt("learning_count"),
                rs.getInt("not_started_count"), rs.getString("area_id") == null ? "NOT_BUILT" : rs.getString("refresh_status"),
                rs.getInt("new_source_count"), rs.getInt("current_version"), rs.getString("generation_failure"),
                toInstant(rs.getTimestamp("area_updated") != null ? rs.getTimestamp("area_updated") : rs.getTimestamp("collection_updated"))));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long initialize(Long ownerId, Long collectionId) {
        Map<String, Object> collection = collection(ownerId, collectionId);
        Long existing = jdbc.query("""
                select id from study_topics where owner_id=:owner and collection_id=:collection
                  and learning_area_type='COLLECTION_BACKED' and status='ACTIVE'
                """, Map.of("owner", ownerId, "collection", collectionId), rs -> rs.next() ? rs.getLong(1) : null);
        if (existing != null) return existing;
        String description = (String) collection.get("description");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("owner", ownerId).addValue("collection", collectionId)
                .addValue("title", collection.get("name"))
                .addValue("goal", description == null || description.isBlank()
                        ? "Học có hệ thống từ các nguồn trong Collection này." : description.trim());
        try {
            return Objects.requireNonNull(jdbc.queryForObject("""
                    insert into study_topics(owner_id,title,goal,status,collection_id,learning_area_type,
                        refresh_status,current_version,created_at,updated_at)
                    values(:owner,:title,:goal,'ACTIVE',:collection,'COLLECTION_BACKED','NOT_BUILT',0,now(),now())
                    returning id
                    """, params, Long.class));
        } catch (DataIntegrityViolationException race) {
            Long winner = jdbc.queryForObject("""
                    select id from study_topics where owner_id=:owner and collection_id=:collection
                      and learning_area_type='COLLECTION_BACKED' and status='ACTIVE'
                    """, Map.of("owner", ownerId, "collection", collectionId), Long.class);
            return Objects.requireNonNull(winner);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public LearningAreaDetailResponse detail(Long ownerId, Long areaId) {
        LearningAreaResponse area = areaSummary(ownerId, areaId);
        List<TopicResourceDto> resources = jdbc.query("""
                select r.id,r.title,r.resource_type,r.processing_status,coalesce(lp.progress_percent,0) progress_percent
                from study_topics st join resource_collections rc on rc.collection_id=st.collection_id
                join resources r on r.id=rc.resource_id
                left join learning_progress lp on lp.resource_id=r.id and lp.owner_id=:owner
                where st.id=:area and st.owner_id=:owner and r.owner_id=:owner and r.processing_status='READY'
                order by r.title,r.id
                """, Map.of("area", areaId, "owner", ownerId), (rs, rowNum) -> new TopicResourceDto(
                rs.getLong("id"), rs.getString("title"), rs.getString("resource_type"),
                rs.getString("processing_status"), rs.getInt("progress_percent")));
        List<LearningModuleResponse> modules = modules(ownerId, areaId, area.currentVersion());
        List<TopicConceptDto> retired = concepts(ownerId, areaId, null, "RETIRED");
        return new LearningAreaDetailResponse(area, resources, modules, retired);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public LearningModuleResponse module(Long ownerId, Long areaId, Long moduleId) {
        LearningAreaResponse area = areaSummary(ownerId, areaId);
        return modules(ownerId, areaId, area.currentVersion()).stream()
                .filter(module -> Objects.equals(module.id(), moduleId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Learning module not found."));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Snapshot snapshot(Long ownerId, Long areaId) {
        Map<String, Object> area = jdbc.query("""
                select st.id,st.collection_id,st.owner_id,c.name,st.goal,st.current_version,
                       st.source_signature,st.refresh_status,st.updated_at
                from study_topics st join collections c on c.id=st.collection_id
                where st.id=:area and st.owner_id=:owner and c.owner_id=:owner
                  and st.learning_area_type='COLLECTION_BACKED' and st.status='ACTIVE'
                """, Map.of("area", areaId, "owner", ownerId), rs -> {
            if (!rs.next()) return null;
            Map<String, Object> result = new HashMap<>();
            result.put("id", rs.getLong("id")); result.put("collection", rs.getLong("collection_id"));
            result.put("owner", rs.getLong("owner_id")); result.put("name", rs.getString("name"));
            result.put("goal", rs.getString("goal")); result.put("version", rs.getInt("current_version"));
            result.put("signature", rs.getString("source_signature")); result.put("status", rs.getString("refresh_status"));
            result.put("updated", toInstant(rs.getTimestamp("updated_at"))); return result;
        });
        if (area == null) throw new NotFoundException("Learning Area not found.");
        Long collectionId = (Long) area.get("collection");
        List<ResourceSnapshot> resources = resourceSnapshots(ownerId, collectionId);
        List<ExistingConcept> existing = existingConcepts(ownerId, areaId);
        return new Snapshot(areaId, collectionId, ownerId, (String) area.get("name"), (String) area.get("goal"),
                (Integer) area.get("version"), (String) area.get("signature"), (String) area.get("status"),
                resources, existing, (Instant) area.get("updated"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markGenerating(Long ownerId, Long areaId, boolean refresh) {
        int updated = jdbc.update("""
                update study_topics set refresh_status=:status,generation_failure=null,updated_at=now()
                where id=:area and owner_id=:owner and learning_area_type='COLLECTION_BACKED'
                """, Map.of("status", refresh ? "REFRESHING" : "BUILDING", "area", areaId, "owner", ownerId));
        if (updated == 0) throw new NotFoundException("Learning Area not found.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long ownerId, Long areaId, String message) {
        jdbc.update("""
                update study_topics set refresh_status='FAILED',generation_failure=:failure,updated_at=now()
                where id=:area and owner_id=:owner and learning_area_type='COLLECTION_BACKED'
                """, new MapSqlParameterSource().addValue("failure", abbreviate(message)).addValue("area", areaId).addValue("owner", ownerId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCurrentWithoutChange(Long ownerId, Long areaId) {
        jdbc.update("""
                update study_topics set refresh_status='CURRENT',generation_failure=null,updated_at=now()
                where id=:area and owner_id=:owner and current_version>0
                """, Map.of("area", areaId, "owner", ownerId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(Long ownerId, Snapshot snapshot, ConceptIdentityReconciler.Result result, String signature) {
        Map<String, Object> locked = jdbc.query("""
                select st.id,st.collection_id,st.current_version from study_topics st
                where st.id=:area and st.owner_id=:owner and st.learning_area_type='COLLECTION_BACKED'
                for update
                """, Map.of("area", snapshot.areaId(), "owner", ownerId), rs -> {
            if (!rs.next()) return null;
            return Map.of("id", rs.getLong("id"), "collection", rs.getLong("collection_id"), "version", rs.getInt("current_version"));
        });
        if (locked == null) throw new NotFoundException("Learning Area not found.");
        String currentSignature = sourceSignature(resourceSnapshots(ownerId, snapshot.collectionId()));
        if (!Objects.equals(signature, currentSignature)) {
            throw new BadRequestException("The Collection changed while the path was being generated. Refresh again with the latest sources.");
        }

        int nextVersion = ((Integer) locked.get("version")) + 1;
        jdbc.update("update learning_path_versions set status='ARCHIVED' where topic_id=:area and status='CURRENT'",
                Map.of("area", snapshot.areaId()));
        Long versionId = Objects.requireNonNull(jdbc.queryForObject("""
                insert into learning_path_versions(topic_id,version_number,source_signature,status,created_at,activated_at)
                values(:area,:version,:signature,'CURRENT',now(),now()) returning id
                """, Map.of("area", snapshot.areaId(), "version", nextVersion, "signature", signature), Long.class));

        Map<ConceptPlan, Long> conceptIds = new IdentityHashMap<>();
        int globalPosition = 1;
        for (ModulePlan module : result.plan().modules()) {
            for (ConceptPlan concept : module.concepts()) {
                Long conceptId = concept.existingId();
                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("area", snapshot.areaId()).addValue("title", concept.title())
                        .addValue("summary", concept.summary()).addValue("why", blankToNull(concept.whyItMatters()))
                        .addValue("position", globalPosition++).addValue("stable", concept.stableKey());
                if (conceptId == null) {
                    conceptId = Objects.requireNonNull(jdbc.queryForObject("""
                            insert into topic_concepts(topic_id,title,summary,why_it_matters,study_status,position,
                                stable_key,lifecycle_status,created_at,updated_at)
                            values(:area,:title,:summary,:why,'NOT_STARTED',:position,:stable,'ACTIVE',now(),now()) returning id
                            """, params, Long.class));
                } else {
                    params.addValue("concept", conceptId);
                    int updated = jdbc.update("""
                            update topic_concepts set title=:title,summary=:summary,why_it_matters=:why,
                                position=:position,stable_key=:stable,lifecycle_status='ACTIVE',updated_at=now()
                            where id=:concept and topic_id=:area
                            """, params);
                    if (updated == 0) throw new BadRequestException("A reconciled concept no longer belongs to this Learning Area.");
                    jdbc.update("""
                            delete from topic_concept_sources tcs using document_chunks dc
                            where tcs.concept_id=:concept and dc.id=tcs.document_chunk_id
                              and not exists(
                                select 1 from resources r join resource_collections rc on rc.resource_id=r.id
                                where r.id=dc.resource_id and r.owner_id=:owner and r.processing_status='READY'
                                  and rc.collection_id=:collection)
                            """, Map.of("concept", conceptId, "owner", ownerId, "collection", snapshot.collectionId()));
                }
                for (Long chunkId : concept.sourceChunkIds()) {
                    jdbc.update("""
                            insert into topic_concept_sources(concept_id,document_chunk_id,relevance_note)
                            select :concept,dc.id,'verified curriculum evidence' from document_chunks dc join resources r on r.id=dc.resource_id
                            join resource_collections rc on rc.resource_id=r.id
                            where dc.id=:chunk and r.owner_id=:owner and r.processing_status='READY' and rc.collection_id=:collection
                            on conflict do nothing
                            """, Map.of("concept", conceptId, "chunk", chunkId, "owner", ownerId, "collection", snapshot.collectionId()));
                }
                Integer persistedEvidence = jdbc.queryForObject("""
                        select count(*) from topic_concept_sources tcs
                        join document_chunks dc on dc.id=tcs.document_chunk_id
                        join resources r on r.id=dc.resource_id
                        join resource_collections rc on rc.resource_id=r.id
                        where tcs.concept_id=:concept and r.owner_id=:owner and r.processing_status='READY'
                          and rc.collection_id=:collection
                        """, Map.of("concept", conceptId, "owner", ownerId, "collection", snapshot.collectionId()), Integer.class);
                if (persistedEvidence == null || persistedEvidence == 0) {
                    throw new BadRequestException("A generated concept lost all verified evidence before persistence.");
                }
                conceptIds.put(concept, conceptId);
            }
        }
        for (Long retiredId : result.retiredConceptIds()) {
            jdbc.update("update topic_concepts set lifecycle_status='RETIRED',updated_at=now() where id=:id and topic_id=:area",
                    Map.of("id", retiredId, "area", snapshot.areaId()));
        }

        int modulePosition = 1;
        for (ModulePlan module : result.plan().modules()) {
            Long moduleId = Objects.requireNonNull(jdbc.queryForObject("""
                    insert into learning_modules(path_version_id,stage,position,title,objective)
                    values(:version,:stage,:position,:title,:objective) returning id
                    """, Map.of("version", versionId, "stage", module.stage(), "position", modulePosition++,
                    "title", module.title(), "objective", module.objective()), Long.class));
            insertModuleResources(moduleId, module.primaryResourceIds(), "PRIMARY");
            insertModuleResources(moduleId, module.supportingResourceIds(), "SUPPORTING");
            int conceptPosition = 1;
            for (ConceptPlan concept : module.concepts()) {
                jdbc.update("""
                        insert into learning_module_concepts(path_version_id,module_id,concept_id,position)
                        values(:version,:module,:concept,:position)
                        """, Map.of("version", versionId, "module", moduleId, "concept", conceptIds.get(concept), "position", conceptPosition++));
            }
        }

        jdbc.update("delete from study_topic_resources where topic_id=:area", Map.of("area", snapshot.areaId()));
        jdbc.update("""
                insert into study_topic_resources(topic_id,resource_id,position)
                select :area,r.id,row_number() over(order by r.title,r.id)-1
                from resource_collections rc join resources r on r.id=rc.resource_id
                where rc.collection_id=:collection and r.owner_id=:owner and r.processing_status='READY'
                """, Map.of("area", snapshot.areaId(), "collection", snapshot.collectionId(), "owner", ownerId));
        jdbc.update("""
                update study_topics set current_version=:version,source_signature=:signature,refresh_status='CURRENT',
                    generation_failure=null,last_refreshed_at=now(),updated_at=now()
                where id=:area and owner_id=:owner
                """, Map.of("version", nextVersion, "signature", signature, "area", snapshot.areaId(), "owner", ownerId));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public ResourceDeepDiveResponse resourceDeepDive(Long ownerId, Long resourceId) {
        Integer owned = jdbc.queryForObject("select count(*) from resources where id=:resource and owner_id=:owner",
                Map.of("resource", resourceId, "owner", ownerId), Integer.class);
        if (owned == null || owned == 0) throw new NotFoundException("Resource not found.");
        List<DeepDiveAreaDto> areas = jdbc.query("""
                select st.id area_id,st.collection_id,st.title,st.refresh_status,
                       lm.id module_id,lm.title module_title,
                       (select count(*) from learning_module_concepts lmc2 where lmc2.module_id=lm.id) concept_count,
                       (select count(*) from learning_module_concepts lmc2 join topic_concepts tc on tc.id=lmc2.concept_id
                        where lmc2.module_id=lm.id and tc.study_status='CHECKED') checked_count,
                       (select count(*) from learning_module_concepts lmc2 join topic_concepts tc on tc.id=lmc2.concept_id
                        where lmc2.module_id=lm.id and tc.study_status='REVIEW_NEEDED') review_count
                from resource_collections rc join study_topics st on st.collection_id=rc.collection_id
                left join learning_path_versions lpv on lpv.topic_id=st.id and lpv.version_number=st.current_version
                left join learning_modules lm on lm.path_version_id=lpv.id
                    and exists(select 1 from learning_module_resources lmr where lmr.module_id=lm.id and lmr.resource_id=:resource)
                where rc.resource_id=:resource and st.owner_id=:owner and st.learning_area_type='COLLECTION_BACKED' and st.status='ACTIVE'
                order by st.updated_at desc,lm.position nulls last
                """, Map.of("resource", resourceId, "owner", ownerId), (rs, rowNum) -> new DeepDiveAreaDto(
                rs.getLong("area_id"), rs.getLong("collection_id"), rs.getString("title"), rs.getString("refresh_status"),
                nullableLong(rs, "module_id"), rs.getString("module_title"), rs.getInt("concept_count"),
                rs.getInt("checked_count"), rs.getInt("review_count"))).stream()
                .collect(java.util.stream.Collectors.toMap(DeepDiveAreaDto::learningAreaId, area -> area,
                        (left, right) -> left.moduleId() != null ? left : right, LinkedHashMap::new)).values().stream().toList();
        if (!areas.isEmpty()) {
            DeepDiveAreaDto first = areas.getFirst();
            LearningAreaResponse summary = areaSummary(ownerId, first.learningAreaId());
            return new ResourceDeepDiveResponse(true, first.learningAreaId(), first.title(), summary.goal(), summary.refreshStatus(),
                    summary.conceptCount(), summary.checkedCount(), summary.reviewNeededCount(), summary.learningCount(),
                    summary.notStartedCount(), summary.updatedAt(), areas);
        }

        List<Map<String, Object>> legacy = jdbc.queryForList("""
                select st.id,st.title,st.goal,st.status,st.updated_at from study_topics st
                join study_topic_resources str on str.topic_id=st.id
                where str.resource_id=:resource and st.owner_id=:owner and st.learning_area_type='LEGACY'
                order by st.updated_at desc limit 1
                """, Map.of("resource", resourceId, "owner", ownerId));
        if (legacy.isEmpty()) return ResourceDeepDiveResponse.unavailable();
        Map<String, Object> topic = legacy.getFirst();
        Long topicId = ((Number) topic.get("id")).longValue();
        Map<String, Object> counts = jdbc.queryForMap("""
                select count(*) concept_count,
                       count(*) filter(where study_status='CHECKED') checked_count,
                       count(*) filter(where study_status='REVIEW_NEEDED') review_count,
                       count(*) filter(where study_status='LEARNING') learning_count,
                       count(*) filter(where study_status='NOT_STARTED') not_started_count
                from topic_concepts where topic_id=:topic
                """, Map.of("topic", topicId));
        return new ResourceDeepDiveResponse(true, topicId, (String) topic.get("title"), (String) topic.get("goal"),
                (String) topic.get("status"), number(counts, "concept_count"), number(counts, "checked_count"),
                number(counts, "review_count"), number(counts, "learning_count"), number(counts, "not_started_count"),
                toInstant((Timestamp) topic.get("updated_at")), List.of());
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public LearningAreaSourceMapResponse sourceMap(Long ownerId, Long areaId, List<Long> requestedResourceIds) {
        LearningAreaResponse area = areaSummary(ownerId, areaId);
        List<Long> active = jdbc.queryForList("""
                select r.id from study_topics st join resource_collections rc on rc.collection_id=st.collection_id
                join resources r on r.id=rc.resource_id
                where st.id=:area and st.owner_id=:owner and r.owner_id=:owner and r.processing_status='READY'
                order by r.title,r.id
                """, Map.of("area", areaId, "owner", ownerId), Long.class);
        List<Long> selected = requestedResourceIds == null || requestedResourceIds.isEmpty()
                ? active.stream().limit(SOURCE_MAP_LIMIT).toList() : requestedResourceIds.stream().distinct().toList();
        if (selected.size() > SOURCE_MAP_LIMIT || !active.containsAll(selected)) {
            throw new BadRequestException("Selected Sources must be READY resources from this Learning Area and limited to 12.");
        }
        List<SourceMapNodeDto> nodes = new ArrayList<>();
        List<SourceMapEdgeDto> edges = new ArrayList<>();
        nodes.add(new SourceMapNodeDto("collection:" + area.collectionId(), "COLLECTION", area.title(), null, area.collectionId(), null));
        if (!selected.isEmpty()) {
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", selected).addValue("owner", ownerId);
            jdbc.query("select id,title from resources where owner_id=:owner and id in (:ids) order by title", params, rs -> {
                Long id = rs.getLong("id");
                nodes.add(new SourceMapNodeDto("resource:" + id, "RESOURCE", rs.getString("title"), id, null, null));
                edges.add(new SourceMapEdgeDto("collection:" + area.collectionId(), "resource:" + id,
                        "COLLECTION_MEMBERSHIP", "READY source inherited from the Collection"));
            });
            jdbc.query("""
                    select distinct tc.id,tc.title,dc.resource_id from topic_concepts tc
                    join learning_module_concepts lmc on lmc.concept_id=tc.id
                    join learning_path_versions lpv on lpv.id=lmc.path_version_id
                    join topic_concept_sources tcs on tcs.concept_id=tc.id
                    join document_chunks dc on dc.id=tcs.document_chunk_id
                    where tc.topic_id=:area and lpv.version_number=:version and dc.resource_id in (:ids)
                    order by tc.title limit 40
                    """, new MapSqlParameterSource().addValue("area", areaId).addValue("version", area.currentVersion()).addValue("ids", selected), rs -> {
                Long conceptId = rs.getLong("id"); Long resourceId = rs.getLong("resource_id");
                String conceptNode = "concept:" + conceptId;
                if (nodes.stream().noneMatch(node -> node.id().equals(conceptNode))) {
                    nodes.add(new SourceMapNodeDto(conceptNode, "CONCEPT", rs.getString("title"), null, null, conceptId));
                }
                edges.add(new SourceMapEdgeDto("resource:" + resourceId, conceptNode,
                        "VERIFIED_EVIDENCE", "Verified chunk evidence supports this concept"));
            });
        }
        return new LearningAreaSourceMapResponse(areaId, List.copyOf(nodes), List.copyOf(edges), selected.size(), true);
    }

    public String sourceSignature(List<ResourceSnapshot> resources) {
        String canonical = resources.stream().sorted(Comparator.comparing(ResourceSnapshot::id))
                .map(resource -> resource.id() + ":" + Objects.toString(resource.checksum(), "") + ":"
                        + Objects.toString(resource.understandingId(), "0") + ":" + Objects.toString(resource.understandingVersion(), "")
                        + ":" + resource.semanticTags().stream().sorted().collect(java.util.stream.Collectors.joining(",")))
                .reduce((left, right) -> left + "|" + right).orElse("");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not calculate learning source signature.", exception);
        }
    }

    private LearningAreaResponse areaSummary(Long ownerId, Long areaId) {
        return listAreas(ownerId).stream().filter(area -> Objects.equals(area.id(), areaId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Learning Area not found."));
    }

    private List<LearningModuleResponse> modules(Long ownerId, Long areaId, int version) {
        if (version <= 0) return List.of();
        return jdbc.query("""
                select lm.id,lm.position,lm.stage,lm.title,lm.objective
                from learning_modules lm join learning_path_versions lpv on lpv.id=lm.path_version_id
                join study_topics st on st.id=lpv.topic_id
                where st.id=:area and st.owner_id=:owner and lpv.version_number=:version
                order by lm.position
                """, Map.of("area", areaId, "owner", ownerId, "version", version), (rs, rowNum) -> {
            Long moduleId = rs.getLong("id");
            List<TopicConceptDto> concepts = concepts(ownerId, areaId, moduleId, "ACTIVE");
            List<ModuleResourceDto> primary = moduleResources(ownerId, moduleId, "PRIMARY");
            List<ModuleResourceDto> supporting = moduleResources(ownerId, moduleId, "SUPPORTING");
            return new LearningModuleResponse(moduleId, rs.getInt("position"), rs.getString("stage"),
                    rs.getString("title"), rs.getString("objective"), concepts.size(),
                    (int) concepts.stream().filter(c -> "CHECKED".equals(c.studyStatus())).count(),
                    (int) concepts.stream().filter(c -> "REVIEW_NEEDED".equals(c.studyStatus())).count(),
                    primary, supporting, concepts);
        });
    }

    private List<TopicConceptDto> concepts(Long ownerId, Long areaId, Long moduleId, String lifecycle) {
        String moduleJoin = moduleId == null ? "" : " join learning_module_concepts lmc on lmc.concept_id=tc.id ";
        String moduleWhere = moduleId == null ? "" : " and lmc.module_id=:module ";
        String order = moduleId == null ? "tc.position" : "lmc.position";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("owner", ownerId)
                .addValue("area", areaId).addValue("lifecycle", lifecycle);
        if (moduleId != null) params.addValue("module", moduleId);
        return jdbc.query("""
                select distinct tc.id,tc.title,tc.summary,tc.why_it_matters,tc.study_status,tc.position,%s order_value
                from topic_concepts tc join study_topics st on st.id=tc.topic_id %s
                where tc.topic_id=:area and st.owner_id=:owner and tc.lifecycle_status=:lifecycle %s
                order by order_value
                """.formatted(order, moduleJoin, moduleWhere), params, (rs, rowNum) -> new TopicConceptDto(
                rs.getLong("id"), rs.getString("title"), rs.getString("summary"), rs.getString("why_it_matters"),
                rs.getString("study_status"), rs.getInt("position"), conceptSources(ownerId, rs.getLong("id"))));
    }

    private List<ConceptSourceDto> conceptSources(Long ownerId, Long conceptId) {
        return jdbc.query("""
                select r.id resource_id,r.title,dc.id chunk_id,dc.content
                from topic_concept_sources tcs join document_chunks dc on dc.id=tcs.document_chunk_id
                join resources r on r.id=dc.resource_id
                where tcs.concept_id=:concept and r.owner_id=:owner and dc.content is not null and btrim(dc.content)<>''
                order by r.title,dc.chunk_index
                """, Map.of("concept", conceptId, "owner", ownerId), (rs, rowNum) -> new ConceptSourceDto(
                rs.getLong("resource_id"), rs.getString("title"), rs.getLong("chunk_id"), abbreviateSnippet(rs.getString("content"))));
    }

    private List<ModuleResourceDto> moduleResources(Long ownerId, Long moduleId, String role) {
        return jdbc.query("""
                select r.id,r.title,r.resource_type from learning_module_resources lmr join resources r on r.id=lmr.resource_id
                where lmr.module_id=:module and lmr.source_role=:role and r.owner_id=:owner order by lmr.position,r.title
                """, Map.of("module", moduleId, "role", role, "owner", ownerId), (rs, rowNum) ->
                new ModuleResourceDto(rs.getLong("id"), rs.getString("title"), rs.getString("resource_type"), role));
    }

    private List<ResourceSnapshot> resourceSnapshots(Long ownerId, Long collectionId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select r.id,r.title,r.resource_type,r.checksum_sha256,du.id understanding_id,
                       du.understanding_version,du.normalized_title,du.summary,
                       du.key_ideas_json::text key_ideas,du.broad_themes_json::text broad_themes,
                       coalesce((select jsonb_agg(t.name order by t.name) from resource_tags rt
                                 join tags t on t.id=rt.tag_id where rt.resource_id=r.id),'[]'::jsonb)::text semantic_tags
                from resource_collections rc join resources r on r.id=rc.resource_id
                left join document_understandings du on du.resource_id=r.id and du.status='CURRENT'
                where rc.collection_id=:collection and r.owner_id=:owner and r.processing_status='READY'
                order by r.id
                """, Map.of("collection", collectionId, "owner", ownerId));
        List<ResourceSnapshot> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long resourceId = ((Number) row.get("id")).longValue();
            Set<Long> verified = row.get("understanding_id") == null ? Set.of() : new LinkedHashSet<>(jdbc.queryForList("""
                    select due.document_chunk_id from document_understanding_evidence due
                    where due.understanding_id=:understanding order by due.document_chunk_id
                    """, Map.of("understanding", ((Number) row.get("understanding_id")).longValue()), Long.class));
            List<EvidenceChunk> chunks = jdbc.query("""
                    select dc.id,dc.resource_id,dc.chunk_index,dc.section,dc.content
                    from document_chunks dc where dc.resource_id=:resource and dc.chunk_level='CHILD'
                    order by dc.chunk_index
                    """, Map.of("resource", resourceId), (rs, rowNum) -> new EvidenceChunk(
                    rs.getLong("id"), rs.getLong("resource_id"), rs.getInt("chunk_index"), rs.getString("section"), rs.getString("content")));
            result.add(new ResourceSnapshot(resourceId, (String) row.get("title"), (String) row.get("resource_type"),
                    (String) row.get("checksum_sha256"), row.get("understanding_id") == null ? null : ((Number) row.get("understanding_id")).longValue(),
                    (String) row.get("understanding_version"), (String) row.get("normalized_title"), (String) row.get("summary"),
                    jsonStrings((String) row.get("key_ideas")), jsonStrings((String) row.get("broad_themes")),
                    jsonStrings((String) row.get("semantic_tags")), verified, chunks));
        }
        return List.copyOf(result);
    }

    private List<ExistingConcept> existingConcepts(Long ownerId, Long areaId) {
        return jdbc.query("""
                select tc.id,tc.title,tc.stable_key,tc.study_status from topic_concepts tc join study_topics st on st.id=tc.topic_id
                where tc.topic_id=:area and st.owner_id=:owner and tc.lifecycle_status='ACTIVE' order by tc.id
                """, Map.of("area", areaId, "owner", ownerId), (rs, rowNum) -> {
            Long conceptId = rs.getLong("id");
            Set<Long> resources = new LinkedHashSet<>(jdbc.queryForList("""
                    select distinct dc.resource_id from topic_concept_sources tcs join document_chunks dc on dc.id=tcs.document_chunk_id
                    where tcs.concept_id=:concept order by dc.resource_id
                    """, Map.of("concept", conceptId), Long.class));
            return new ExistingConcept(conceptId, rs.getString("title"), rs.getString("stable_key"), rs.getString("study_status"), resources);
        });
    }

    private Map<String, Object> collection(Long ownerId, Long collectionId) {
        Map<String, Object> row = jdbc.query("select id,name,description from collections where id=:id and owner_id=:owner",
                Map.of("id", collectionId, "owner", ownerId), rs -> {
            if (!rs.next()) return null;
            Map<String, Object> result = new HashMap<>();
            result.put("id", rs.getLong("id")); result.put("name", rs.getString("name")); result.put("description", rs.getString("description"));
            return result;
        });
        if (row == null) throw new NotFoundException("Collection not found.");
        return row;
    }

    private void insertModuleResources(Long moduleId, List<Long> resourceIds, String role) {
        int position = 1;
        for (Long resourceId : resourceIds) {
            jdbc.update("insert into learning_module_resources(module_id,resource_id,source_role,position) values(:module,:resource,:role,:position)",
                    Map.of("module", moduleId, "resource", resourceId, "role", role, "position", position++));
        }
    }

    private List<String> jsonStrings(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() { }); }
        catch (Exception exception) { return List.of(); }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column); return rs.wasNull() ? null : value;
    }
    private int number(Map<String, Object> values, String key) { Object value = values.get(key); return value instanceof Number number ? number.intValue() : 0; }
    private String coalesce(String... values) { return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse(""); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String abbreviate(String value) { String safe = value == null || value.isBlank() ? "Learning path generation failed." : value; return safe.substring(0, Math.min(500, safe.length())); }
    private String abbreviateSnippet(String value) { return value == null ? "" : value.substring(0, Math.min(220, value.length())) + (value.length() > 220 ? "…" : ""); }
    private Instant toInstant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
