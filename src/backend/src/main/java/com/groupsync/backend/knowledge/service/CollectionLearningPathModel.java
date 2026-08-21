package com.groupsync.backend.knowledge.service;

import java.time.Instant;
import java.util.*;

final class CollectionLearningPathModel {
    private CollectionLearningPathModel() { }

    record EvidenceChunk(Long chunkId, Long resourceId, int chunkIndex, String section, String content) { }

    record ResourceSnapshot(Long id, String title, String resourceType, String checksum,
                            Long understandingId, String understandingVersion, String normalizedTitle,
                            String summary, List<String> keyIdeas, List<String> broadThemes,
                            List<String> semanticTags, Set<Long> verifiedEvidenceIds,
                            List<EvidenceChunk> chunks) { }

    record ExistingConcept(Long id, String title, String stableKey, String studyStatus,
                           Set<Long> evidenceResourceIds) { }

    record Snapshot(Long areaId, Long collectionId, Long ownerId, String title, String goal,
                    int currentVersion, String currentSignature, String refreshStatus,
                    List<ResourceSnapshot> resources, List<ExistingConcept> existingConcepts,
                    Instant updatedAt) { }

    static final class ConceptPlan {
        private String title;
        private String summary;
        private String whyItMatters;
        private final LinkedHashSet<Long> sourceChunkIds;
        private String stableKey;
        private Long existingId;

        ConceptPlan(String title, String summary, String whyItMatters, Collection<Long> sourceChunkIds) {
            this.title = title;
            this.summary = summary;
            this.whyItMatters = whyItMatters;
            this.sourceChunkIds = new LinkedHashSet<>(sourceChunkIds == null ? List.of() : sourceChunkIds);
        }

        String title() { return title; }
        String summary() { return summary; }
        String whyItMatters() { return whyItMatters; }
        LinkedHashSet<Long> sourceChunkIds() { return sourceChunkIds; }
        String stableKey() { return stableKey; }
        Long existingId() { return existingId; }
        void setStableKey(String value) { stableKey = value; }
        void setExistingId(Long value) { existingId = value; }
        void mergeEvidence(ConceptPlan other) { sourceChunkIds.addAll(other.sourceChunkIds); }
    }

    static final class ModulePlan {
        private final String title;
        private final String stage;
        private final String objective;
        private final List<Long> primaryResourceIds;
        private final List<Long> supportingResourceIds;
        private final List<ConceptPlan> concepts;

        ModulePlan(String title, String stage, String objective, List<Long> primaryResourceIds,
                   List<Long> supportingResourceIds, List<ConceptPlan> concepts) {
            this.title = title;
            this.stage = stage;
            this.objective = objective;
            this.primaryResourceIds = List.copyOf(primaryResourceIds);
            this.supportingResourceIds = List.copyOf(supportingResourceIds);
            this.concepts = new ArrayList<>(concepts);
        }

        String title() { return title; }
        String stage() { return stage; }
        String objective() { return objective; }
        List<Long> primaryResourceIds() { return primaryResourceIds; }
        List<Long> supportingResourceIds() { return supportingResourceIds; }
        List<ConceptPlan> concepts() { return concepts; }
    }

    record LearningPlan(String title, List<ModulePlan> modules) { }
}
